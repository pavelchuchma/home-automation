# Metrics reporting to InfluxDB

home-controller pushes the technical state it already holds (energy, heating, ventilation,
water, mower) to the InfluxDB 2 instance of the Grafana stack
(`~/work/HomeAutomation/grafana/`) once a minute. Grafana dashboards live in `grafana/` in
this repo (`home-energy`, `home-climate`, `home-water`, `home-mower`; see
`grafana/README.md` for the export/push workflow). Design notes and the reasoning behind what is and is not recorded are in
`doc/todo/003-influxdb-metrics-grafana.md`.

**Not recorded, on purpose:** motion and magnetic sensors, lights, louvers, air valves,
garage door, PIC node health. See the scope section of todo 003 before adding anything.

## Configuration

Reporting is off unless `influx.url` is set. Defaults in
`app/src/main/resources/default-app.properties`, real values in the git-ignored
`cfg/app.properties` (only the `pi.local` deployment reports):

| Option | Default | Meaning |
| --- | --- | --- |
| `influx.url` | *(empty)* | base URL, e.g. `http://localhost:8086`; empty disables reporting |
| `influx.org` | *(empty)* | organisation |
| `influx.bucket` | *(empty)* | bucket |
| `influx.token` | *(empty)* | write-only token for the bucket (create in InfluxDB UI, Load Data → API Tokens) |
| `influx.measurement.prefix` | `home` | measurements are written as `<prefix>-<name>` |

## How it works

- `MetricsReporter` (`extensions/.../metrics/`) ticks on every whole minute, asks each
  registered `MetricSource` to write its lines, appends events queued since the last tick
  and POSTs everything in one request (`precision=ms`). A failed POST drops the batch; the
  first failure is logged at WARN (`Metrics upload failed`), repeats at DEBUG, recovery at INFO.
- **A gap is a gap.** A source whose device state is unknown writes nothing. Why the data is
  missing is in the app log (`Failed to refresh ... state`).
- `AbstractStateMonitor` runs nonstop at two rates: the fast one (5 s) for 60 s after the
  web UI asked for the state, the idle one otherwise. The reporter reads with
  `peekState()`, which does not switch the fast rate on. Idle rates: inverter, Futura and
  Robonect 60 s; boiler 10 min (a boiler refresh is a 15 s key sequence on its display).
- The HVAC library has no notion of stale data; `HvacPacketClock` records the last packet
  time and `hvac` is skipped when the bus has been silent for more than 5 minutes.
- Robonect off season: the monitor backs off to one attempt per 10 minutes after ten
  failures and `mower` is simply absent until spring.

## Measurements

Names use the default prefix. *grid* = one point per minute, *event* = at the time it
happened, *refresh* = one point per successful device read.

| Measurement | Fields | Cadence |
| --- | --- | --- |
| `home-inverter` | mode, battery_mode, manual_mode, pgrid_bias (str); pv1_w, pv2_w, grid_w, feed_in_w (+export / −import), battery_w (+charge), eps_w; soc, batt_temp_c, batt_v, grid1_v..grid3_v; yield_today_kwh, pv_yield_today_kwh, feed_in_today_kwh, consumed_today_kwh, yield_total_kwh, feed_in_total_kwh, consumed_total_kwh; min_soc, export_limit_w | grid |
| `home-inverter-manager` | high_tariff, export_priority, force_discharge, inverter_off_negative_price (bool); desired_min_soc; buy_price, sell_price_net (CZK/kWh) | grid |
| `home-price` | buy (incl. distribution fee and VAT), spot (buy − distribution fee), sell_net (buy − fees), all CZK/kWh | event: one point per 15 min interval, written ahead once the day is published |
| `home-futura` | speed (0 off, 1–5, 6 auto), temp_ambient_c (outdoor), temp_fresh_c, temp_indoor_c, temp_waste_c, filter_pct, power_w, heat_recovery_w, co2_ppm, wall_temp_c, time_program (bool) | grid |
| `home-boiler` | mode (str), status_age_s, t5u_c, t5l_c, t3_c, t4_c, tp_c, th_c, target_c; on, hot, eheat, pump, vacation (bool); last_fail (str); scheduled_on, disinfect_req, eheat_req (bool), target_req_c (what `BoilerManager` wants) | refresh, 10 min, stamped with the read time |
| `home-hvac` | on, auto, quiet, sleep, defrost (bool); mode, target_mode, fan (str); target_c, air_c, air2_c, room_c, unit_c | grid, skipped when the bus is silent |
| `home-pump` | on (bool) | event, both edges |
| `home-pump-min` | cycles (ended since the previous tick), run_s (their total run time) | grid |
| `home-tank` | status (str), distance_mm, samples, valid_samples, rssi, reset_reason (str); fill_pct only for `OK` readings | event, each ESP push (~5 min) |
| `home-mower` | status, mode (str); battery_pct, duration_s, hours, distance_m, home, stopped (bool); wlan_dbm; temp_c, humidity_pct; error_code; lat, lon | grid, absent off season |

Grafana conventions: `spanNulls: false` and `createEmpty: true` wherever a gap is
meaningful. `home-boiler` (10 min) and `home-price` (15 min) are sampled slower than a
zoomed-in panel's window, so those series use `stepAfter` and a bounded `spanNulls`
(25 min / 20 min) instead.

## Verifying on the Pi

```sh
grep 'Metrics' /usr/local/bin/homeAutomation/out/app.log | tail
```

`Metrics reporting to http://...` at startup confirms the configuration, a `Metrics flushed`
line (DEBUG) every minute confirms uploads. In the InfluxDB UI (`http://pi.local:8086`,
Data Explorer) filter the `metrics` bucket by measurement `home-inverter`.
