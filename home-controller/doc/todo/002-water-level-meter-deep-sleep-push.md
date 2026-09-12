---
id: 002
title: Water level meter: switch from polled web server to deep-sleep push model
type: enhancement
status: in-progress
priority: medium
component: infra
created: 2026-08-19
---

# Water level meter: switch from polled web server to deep-sleep push model

## Summary

The water tank level sensor (ESP8266 + JSN-SR04T ultrasonic module, PlatformIO
project at `~/work/esp/water-level-meter`) currently runs as an
always-connected web server: it stays on Wi-Fi 24/7 and home-controller's
`WaterTankClient` polls its `/api/get` JSON endpoint periodically. That is
wasteful (constant Wi-Fi power draw, one more device permanently occupying the
Wi-Fi segment) and unnecessary — the level changes slowly and one reading
every few minutes is plenty.

Refactor to a push model: the ESP deep-sleeps, wakes once every ~5 minutes,
measures the level, pushes the result to pi.local, and goes back to sleep.
As part of this refactor, publish the `water-level-meter` project to GitHub.

## Current state

- Firmware (`src/main.cpp`) keeps Wi-Fi connected, serves an HTML status page
  at `http://<sensor-host>/` and JSON at `/api/get`
  (`{"distance":<mm>,"status":..,"ageSec":..,"samples":..,"validSamples":..}`).
- home-controller polls it: `WaterTankClient.java:33` builds
  `http://<host>/api/get` and `WaterTankMonitor` refreshes periodically.
- Sensor reading is a non-blocking median-of-samples state machine, designed
  around keeping the HTTP server responsive — a constraint that disappears
  entirely in the push model.

## Proposed change

Agreed design (2026-09-12), superseding the first draft:

ESP side (`src/main.cpp`):

1. Each cycle: start Wi-Fi (`WiFi.begin()`) first, then take the blocking
   median-of-samples measurement while the radio associates in the
   background, wait for the connection (bounded), POST the result to
   home-controller, then sleep `CYCLE_INTERVAL_MS`. Connect time (2-5 s) and
   measurement (1.5-6 s) overlap instead of adding up.
2. Payload is `application/x-www-form-urlencoded`, POSTed to
   `http://<pi-ip>/rest/wtank/push`: `distance` (mm), `status` (`OK`/`FAIL`),
   `samples`, `validSamples`, plus diagnostics `rssi` and `resetReason`. A
   failed measurement is posted too, so the server can tell "sensor alive but
   no echo" from "sensor dead".
3. Target URL is a fixed IP in the gitignored `include/credentials.h`
   (`PUSH_URL`). `pi.local` cannot be used: the ESP8266 mDNS library is a
   responder only, lwIP has no `.local` resolver.
4. Failure handling: Wi-Fi connect and HTTP each have a bounded timeout
   (15 s / 5 s); on failure log to Serial and go to sleep, no retries.
5. Cold boot vs. wake: `ESP.getResetInfoPtr()->reason`. Anything other than
   `REASON_DEEP_SLEEP_AWAKE` (power-on, RST button, reset after flashing) is a
   cold boot: turn the radio off (`WiFi.mode(WIFI_OFF)`, persisted so the SDK
   does not bring RF up on its own at the next boot either), sleep
   `BOOT_DELAY_MS`, then enter the normal cycle. The ESP shares the 5 V rail
   with the Pi and its disk; delaying Wi-Fi after a power outage spreads the
   inrush peaks apart.
6. No periodic 24 h reboot: a deep-sleep wake already is a full chip reset
   (only RTC memory survives), so an explicit `ESP.restart()` adds nothing.
7. Timing constants with a debug and a production value:

   | constant                 | debug (now) | production |
   | ------------------------ | ----------- | ---------- |
   | `USE_DEEP_SLEEP`         | `false`     | `true`     |
   | `BOOT_DELAY_MS`          | 1 000       | 60 000     |
   | `CYCLE_INTERVAL_MS`      | 10 000      | 300 000    |

   In debug mode the sleep is a plain `delay()` and Wi-Fi stays connected
   between cycles (`WiFi.begin()` is skipped while connected). Deep sleep
   needs the D0/GPIO16 -> RST wire, which is not installed yet; both are
   switched on together at the end.
8. The HTML status page, `/api/get` and the mDNS responder go away. The
   `SENSOR_MODE_SERIAL` branch stays in the file, disabled, as before.

home-controller side:

1. `WaterTankMonitor` no longer extends `AbstractStateMonitor` (no polling
   thread): it is a passive holder of the last pushed reading with
   `recordReading(...)` and `getFillPercent()`, which returns -1 when the last
   `OK` reading is older than the 15 min validity window (three missed
   cycles). Injectable clock for tests.
2. `WaterTankClient` is deleted.
3. New `WaterTankHandler` (`/rest/wtank`): `POST .../push` records a reading
   (200, or 400 on a missing/invalid parameter), `GET .../status` reports fill
   percent, last distance, age and the diagnostics from the last push. Listed
   in `/rest/all` like the other device handlers.
4. `waterTank.host` is dropped from `app.properties`; the monitor always
   exists and reports -1 until the first push arrives. `WaterPumpHandler` and
   the pump widget are unchanged.
5. Unit test `WaterTankMonitorTest` (percent maths, `FAIL` keeps the last good
   value, validity window expiry). No hardware involved.

Rollout order: server first (`b c b` + `script/deploy-pi.sh`, restarts the
service on the Pi), then flash the ESP (`pio run -t upload`) and watch the
Serial monitor and the Pi log. Bogus readings during development (sensor
measuring while the firmware is being tuned) are acceptable.

GitHub:

1. Create a GitHub repository for `water-level-meter` and push the project.
2. Before publishing, verify no secrets are committed — Wi-Fi credentials and
   `PUSH_URL` are in the gitignored `include/credentials.h` with a `.example`
   template, and docs must not contain private LAN IPs.

Final phase, once the push model is debugged: switch the constants to the
production column, install the D0/RST wire, publish to GitHub, close this
issue.

## Notes

- Side benefit: removes the failure mode seen on 2026-08-19, where the sensor
  was unreachable for hours (`Water tank status FAIL`, `validSamples=0`) —
  with push + age-based staleness the server no longer depends on the sensor
  being reachable on demand (see [001](001-boiler-turnon-no-retry.md) for the
  wider network-outage context of that day).
- The interactive status page at `http://<sensor-host>/` goes away; the
  current level is still visible in the home-controller UI (pump widget).

## Progress

- 2026-09-12: server side (`WaterTankMonitor`, `WaterTankHandler`, `WaterTankMonitorTest`)
  deployed to the Pi; firmware rewritten to the push model and flashed with the debug
  constants (no deep sleep, 1 s boot delay, 10 s cycle). Verified end to end: cold boot
  path, `POST /rest/wtank/push` → 200, `/rest/wtank/status` and the pump widget show the
  level, 400/405 on bad requests. A cycle takes ~4.5 s with Wi-Fi already up, ~6 s from a
  cold boot including association.
- 2026-09-12 (later): the ~50 % invalid-sample rate turned out to be trigger spacing, not
  noise — the sensor drops a trigger sooner than ~80-100 ms after the previous one, so with
  60 ms spacing exactly every second sample was lost. Spacing raised to 100 ms, now 25/25
  valid per cycle. D0/RST wire installed, deep sleep enabled and production constants set
  (60 s boot delay, 5 min cycle); a wake cycle takes ~7 s including Wi-Fi association.
  README added to the firmware repo.
- Remaining: GitHub publication (repo not created yet).

## Resolution

_Not resolved yet._
