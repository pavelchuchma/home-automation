---
id: 003
title: Report home-controller state to InfluxDB and build Grafana dashboards
type: enhancement
status: done
resolved: 2026-10-17
priority: medium
component: app
created: 2026-10-17
---

# Report home-controller state to InfluxDB and build Grafana dashboards

## Summary

The Grafana + InfluxDB 2 stack already runs on the Pi (`~/work/HomeAutomation/grafana/`,
org `home`, bucket `metrics`) and `mps-inverter-controller` writes into it every
minute (`src/influx.cpp`, dashboard `grafana/chajda.json`). home-controller writes
nothing, so questions about energy, heating or air quality history still go through
the app log.

Goal: push the **technical** state home-controller already holds (energy, heating,
ventilation, water, mower) to InfluxDB on a one-minute grid, plus exact-time events
where a device reports by push, and build dashboards on top. Implemented: reporter, sources and options (`doc/metrics.md` is the measurement
reference as built) and the four dashboards in `grafana/`.

## Scope: what is deliberately not recorded

Nothing that describes the movement or behaviour of the people in the house goes to
InfluxDB: **no PIR / magnetic sensors, no lights, no louvers, no air valves, no garage
door.** It is not needed for any question the dashboards should answer and it would
amount to surveillance of the household. The web UI shows the live state of all of these and that is enough.
Do not add them later "because the hook is cheap".

Also not recorded: PIC node health (reboots, ping age, message rates). The nodes have run
for years without trouble and the node page in the web UI shows their state when needed.

## Current state

- **External devices are read on demand only.** Every `AbstractStateMonitor`
  (`SolaxInverterMonitor`, `FuturaMonitor`, `BoilerMonitor`, `RobonectMonitor`) refreshes
  only while somebody called `getState()` within `maxUnusedRunTimeMs`, then drops its
  state and sleeps. Wiring in `PiConfigurator`:

  | Monitor | refresh | sleeps after unused | cost of one refresh |
  | --- | --- | --- | --- |
  | Solax inverter | 5 s | 60 s | Modbus TCP, ~1.2 s |
  | Futura (recuperation) | 5 s | 60 s | Modbus TCP, cheap |
  | Boiler | 10 min | 60 min | coil 200 = 15 s key sequence on the boiler display |
  | Robonect (mower) | 2.5 s | 5 h | HTTP, 1-3 requests |

- `InverterManager` makes decisions every minute (`doPowerManagement`,
  `manageForceDischarge`) using its own `client.getState()` calls, independent of the
  monitor. Its decisions (high tariff, export priority, force discharge) are only in the log.
- Other state already held in memory and exposed to the web UI: `WaterPumpMonitor` (pump
  run records), `WaterTankMonitor` (pushed by the ESP every 5 min), `HvacDevice`.
- The reference implementation in `mps-inverter-controller` samples every 10 s and
  flushes once a minute; that cadence is for debugging. Here one sample per minute is enough.

## Design

### Reporter

One `MetricsReporter` in `extensions` (`org.chuma.homecontroller.extensions.metrics`):

- Hand-written **line protocol**, POSTed with `java.net.http.HttpClient` (Java 21, no new
  dependency; the official client drags in OkHttp/Retrofit, not worth it on a 2 GB Pi).
  Tag values must be escaped (space, comma, `=`), string fields quoted.
- **One POST per minute** to `<influx.url>/api/v2/write?org=…&bucket=…&precision=ms`.
  The tick samples every registered `MetricSource`, appends any queued events, sends,
  drops the batch on failure (log a warning, rate-limited). Never buffer to disk.
- `precision=ms`, so that an event and a grid sample of the same measurement in the same
  second do not collide (same measurement + tags + timestamp = overwrite).
- `MetricSource` interface: `void sample(LineProtocolWriter w, long nowMs)`. Sources are
  plain classes registered in the configurator; `reporter.event(...)` for off-grid points
  from any thread (same pattern as `influx_log_event()` in the ESP firmware).
- **A gap must look like a gap**: a source whose state is `null` writes nothing. Why the
  data is missing is answered by the app log, which already records every failed refresh;
  there is no separate health series.
- **Reporting is off unless configured.** The application has three deployments and only
  the `pi.local` instance reports. New options, with the defaults in
  `app/src/main/resources/default-app.properties` and the real values in the git-ignored
  `cfg/app.properties` (so the token may live there):

  | Option | Default | Meaning |
  | --- | --- | --- |
  | `influx.url` | *(empty)* | InfluxDB base URL, e.g. `http://localhost:8086`. Empty = reporter is not created at all. **No default URL**, not even localhost, same convention as `boiler.ip`. |
  | `influx.org` | *(empty)* | organisation |
  | `influx.bucket` | *(empty)* | bucket |
  | `influx.token` | *(empty)* | write-only token for the bucket (grafana README section 4), name it `home-controller` |
  | `influx.measurement.prefix` | `home` | measurement name prefix; every measurement is written as `<prefix>-<name>` |

  The prefix is what separates sites in the shared bucket (`chajda-*` is the cottage
  firmware). A second deployment that one day wants reporting sets its own prefix and
  the dashboards' measurement names become a Grafana variable. The reporter is wired
  in every configurator the same way; with an empty `influx.url` it is a no-op, so
  `martin`, `petr` and `ondra` are unaffected.

### Monitors run nonstop, at two rates

Change `AbstractStateMonitor` instead of each monitor:

- The loop never sleeps. `maxUnusedRunTimeMs` is reinterpreted as "how long after the last
  UI request the fast rate stays on"; afterwards the monitor keeps refreshing at a new
  `idleRefreshIntervalMs`. `getState()` from REST handlers bumps `lastUseTime` as today;
  the reporter uses a new `peekState()` that does not, so Grafana sampling alone never
  turns the fast rate on.
- Proposed idle rates: inverter 60 s, Futura 60 s, Robonect 60 s, **boiler stays at
  10 min** (a refresh walks the boiler display for 15 s, so once a minute is not an option;
  consider 5 min only inside heating intervals). The Robonect backoff after 10 failures
  stays as is (mower off in winter).
- `InverterManager` keeps its own direct `client.getState()` calls; nothing to change there
  except exposing the decision flags (getters for `highTariffScheduler.isInInterval()`,
  `exportPriorityScheduler.isInInterval()`, `forceDischargeActive`, current buy/sell price).
- **HVAC is not an `AbstractStateMonitor` and needs no cadence change.** `HvacDevice`
  (library `org.chuma.hvac-controller`) runs a reader thread on the serial port and
  updates its state from every packet the wall controller and the unit exchange; it only
  transmits in `set()`. The getters are therefore always current while the bus is alive,
  but nothing exposes *when* the last packet arrived, so a dead bus would keep writing the
  last values forever. The constructor accepts `IPacketProcessor[]` (`PiConfigurator`
  passes `null`): register one that records the last packet time and skip the `home-hvac`
  point when it is older than a few minutes, so the gap rule holds for HVAC too.

### Off-season devices (Robonect)

The mower is powered off from autumn to spring, so `RobonectMonitor` fails every refresh
for months. What that must look like:

- **Polling cost stays negligible.** The existing backoff (10 consecutive failures, then one
  attempt per 10 min) applies to the nonstop loop unchanged; each attempt is a single HTTP
  request with a 30 s timeout, so the idle 60 s rate costs one failed request per 10 min.
  Failures after the tenth are logged at DEBUG, so the log stays quiet.
- **`home-mower` is a gap**, as designed. The Mower dashboard gets a stat panel
  "last seen" (`last()` of any `home-mower` field, shown as relative time) so an empty
  time series reads as "last data 2026-10-14", not as a broken dashboard.
- `robonect.enabled` exists in `cfg/app.properties` but nothing reads it. It could become a
  manual season switch that does not start the monitor at all (`isRunning()` false already
  disables the REST handler). Not needed for v1: the automatic backoff is cheap and needs
  no remembering in spring.

### Event hooks

Things that report by push or by edge are written at the time they happen, not on the
minute grid:

| Source | Hook |
| --- | --- |
| Water pump | `WaterPumpMonitor` on/off actions already exist |
| Water tank | `WaterTankMonitor.recordReading()` |
| Spot prices | when `ElectricitySpotPriceMonitor` fetches a day, write all 96 intervals with their own (future) timestamps; idempotent on refetch |

## Measurements

Names below use the default prefix `home` (`influx.measurement.prefix`; the cottage uses
`chajda-`; the bucket is shared, so the prefix is the namespace). `(grid)` = one point per
minute, `(event)` = at change time, `(refresh)` = one point per successful device read.

| Measurement | Tags | Fields | Cadence |
| --- | --- | --- | --- |
| `home-inverter` | | mode, battery_mode, manual_mode (str); pv1_w, pv2_w, grid_w, feed_in_w (+export/−import), battery_w (+charge), eps_w; soc, batt_temp_c, batt_v, grid1..3_v; yield_today_kwh, pv_yield_today_kwh, feed_in_today_kwh, consumed_today_kwh, *_total; min_soc, export_limit_w, pgrid_bias (str) | grid, null = gap |
| `home-inverter-manager` | | high_tariff, export_priority, force_discharge (bool); buy_price, sell_price_net; desired_min_soc, inverter_off_negative_price (bool) | grid |
| `home-price` | | spot, buy (fees + VAT), sell_net | event: 96 × 15 min per day, written ahead |
| `home-futura` | | speed (0-6), temp_ambient_c (outdoor, the only outdoor sensor we have), temp_fresh_c, temp_indoor_c, temp_waste_c, filter_pct, power_w, heat_recovery_w, co2_ppm, wall_temp_c, time_program (bool) | grid |
| `home-boiler` | | mode (str), status_age_s, t5u_c, t5l_c, t3_c, t4_c, tp_c, th_c, target_c; on, hot, eheat, pump, vacation (bool); last_fail (str); scheduled_on, disinfect_req, eheat_req (bool, from `BoilerManager`) | refresh (10 min); Grafana `spanNulls` 25 min, `stepAfter` like `chajda-inverter-config` |
| `home-hvac` | | on, defrost, auto, quiet, sleep (bool); mode, target_mode, fan (str); target_c, air_c, air2_c, room_c, unit_c | grid, only when `hvac.port` set |
| `home-pump` | | on (bool) | event |
| `home-pump-min` | | cycles, run_s | grid; cycles/day is the leak indicator |
| `home-tank` | | distance_mm, fill_pct, status (str), samples, valid_samples, rssi, reset_reason (str) | event, each ESP push (~5 min) |
| `home-mower` | | status, mode (str); battery_pct, duration_s, hours, distance_m, home, stopped (bool); wlan_dbm; temp_c, humidity_pct; error_code; lat, lon (for a geomap panel) | grid |

Volume: roughly 100 fields on the minute grid plus a few dozen events a day, in one
request per minute. That is the load the stack README was sized for; a few MB per month
in InfluxDB.

## Dashboards

Keep the JSON in this repo under `grafana/` with the same export/push workflow as
`mps-inverter-controller/grafana/README.md` (export sorts keys; a Python helper script fits
better than the two curl pipelines). Suggested dashboards, one per question:

1. **Energy** — PV / load / grid / battery stacked, SoC, today's yields, day-ahead price
   curve with high-tariff / export-priority / force-discharge bands as a state timeline,
   `min_soc` and `export_limit_w` as steps, inverter mode. Boiler `on` and HVAC `on`
   overlaid as the two big controllable consumers.
2. **Climate and air** — Futura temperatures (and recovery efficiency derived from them),
   CO2, speed, filter wear; HVAC temperatures and mode; outdoor temperature; boiler
   temperatures and heating cycles.
3. **Water** — tank level and sensor diagnostics (RSSI, reset reason, failed readings),
   pump cycles per day and run time.
4. **Mower** — geomap of GPS points, status timeline, battery, WLAN signal, "last seen".

Conventions to carry over from the chajda dashboard: `spanNulls: false` and
`createEmpty: true` everywhere a gap is meaningful; the boiler and price series are the
exception (fixed cadence, `stepAfter`, bounded `spanNulls`).

## Code changes (in order)

1. `AbstractStateMonitor`: nonstop loop, idle rate, `peekState()`. Unit-testable with a
   fake clock, no hardware needed.
2. `metrics` package: `LineProtocolWriter` (escaping, typed fields, ms timestamps),
   `MetricsReporter` (tick, event queue, HTTP, failure handling), `MetricSource`.
   Unit tests on the produced line protocol; the HTTP part is a one-off manual check
   against the Pi with `curl`-equivalent output.
3. Sources per measurement above, wired in `PiConfigurator`; getters on
   `InverterManager` and `BoilerManager`, HVAC `IPacketProcessor` for the last packet
   time.
4. Options in `default-app.properties` (empty url, prefix `home`), real values + token in
   `cfg/app.properties` on the Pi only, deploy, verify in the InfluxDB Data Explorer. The
   other two deployments get nothing to configure and keep reporting off.
5. Dashboards, `grafana/README.md`, `doc/metrics.md` (measurement reference, linked from
   `CLAUDE.md`).

## Open decisions

- **Boiler cadence inside heating intervals** — 5 min would show the heating curve better
  at the price of the display cycling twice as often. Default: keep 10 min.
- **Which sources each configurator registers.** The reporter itself is created from the
  options in every configurator; the list of sources is per configurator, because
  `PiPeConfigurator` has only the inverter and `martin` / `ondra` have no external devices.

## Not in scope

- Downsampling tasks and retention tuning — revisit once the bucket has a few months of data.
- Alerting (Grafana alerts on tank sensor silence or boiler `lastFail` changes) — possible
  follow-up; failures of the monitors themselves stay in the app log.

## Resolution

- Reporter, sources, options and the two-rate `AbstractStateMonitor` in commit
  `81698c6`; deployed on `pi.local` 2026-10-17, first points at 17:20.
- Dashboards `home-energy`, `home-climate`, `home-water`, `home-mower` generated, pushed
  and exported to `grafana/*.json`; every Flux target checked through `/api/ds/query`
  (0 errors) and the panels checked visually in the browser.
- Dropped during the design: sensors, lights, louvers, valves, garage, node health, the
  monitor health series and JVM metrics, mower blade counters, a computed sun position.
- Open: a panel-level time shift into the future is not supported by Grafana, so
  tomorrow's prices need a dashboard range ending after now.
