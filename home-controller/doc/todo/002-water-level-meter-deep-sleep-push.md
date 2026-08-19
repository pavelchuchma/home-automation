---
id: 002
title: Water level meter: switch from polled web server to deep-sleep push model
type: enhancement
status: open
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

ESP side:

1. On wake: read the sensor (median of N samples as today, but a simple
   blocking loop is fine now), connect to Wi-Fi, POST the result as JSON to a
   home-controller HTTP endpoint on pi.local, then `ESP.deepSleep(5 min)`.
   (Requires the D0/GPIO16 → RST wire for deep-sleep wakeup.)
2. Keep the payload compatible with today's `/api/get` JSON where practical
   (`distance`, `samples`, `validSamples`) so the server-side parsing stays
   trivial.
3. Failure handling: if Wi-Fi or the POST fails, don't retry forever — give up
   after a bounded time and go back to sleep (battery/power budget beats one
   lost sample).
4. Power-on delay: after a cold boot (power-up, as opposed to a deep-sleep
   wakeup), sleep for 60 s before entering the measure/push loop. The ESP
   shares a single 5 V supply rail with the Raspberry Pi (pi.local) and its
   disk; after a power outage everything boots at once, and the ESP's Wi-Fi
   radio init on top of the Pi + disk spin-up inrush can brown out the rail.
   Delaying the ESP's Wi-Fi activity spreads the load peaks apart.

home-controller side:

1. Add a small REST endpoint (servlet handler) accepting the pushed
   measurement and feeding it into `WaterTankMonitor`'s state.
2. Replace the polling logic in `WaterTankClient`/`WaterTankMonitor` with
   "last received sample + age" bookkeeping; report FAIL when no sample
   arrives for several push periods (e.g. > 15 min).

GitHub:

1. Create a GitHub repository for `water-level-meter` and push the project.
2. Before publishing, verify no secrets are committed — Wi-Fi credentials are
   in the gitignored `include/credentials.h` with a `.example` template, and
   docs must not contain private LAN IPs.

## Notes

- Side benefit: removes the failure mode seen on 2026-08-19, where the sensor
  was unreachable for hours (`Water tank status FAIL`, `validSamples=0`) —
  with push + age-based staleness the server no longer depends on the sensor
  being reachable on demand (see [001](001-boiler-turnon-no-retry.md) for the
  wider network-outage context of that day).
- The interactive status page at `http://<sensor-host>/` goes away; the
  current level is still visible in the home-controller UI (pump widget).

## Resolution

_Not resolved yet._
