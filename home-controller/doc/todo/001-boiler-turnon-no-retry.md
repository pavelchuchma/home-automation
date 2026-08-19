---
id: 001
title: Boiler stays off for the whole day when turnOn() fails at interval start
type: bug
status: open
priority: high
component: boiler
created: 2026-08-19
---

# Boiler stays off for the whole day when turnOn() fails at interval start

## Summary

The scheduled boiler heating interval (`boiler.times=12:00-17:00`) is driven by
a single one-shot `turnOn()` call at the interval start. On 2026-08-19 that
call failed because the boiler Modbus gateway was briefly unreachable at
exactly 12:00. The exception was logged and swallowed; nothing ever retried,
so the boiler stayed OFF for the entire interval and the water was not heated
that day.

## Observed behavior

On 2026-08-19 the 12:00 cron task fired but the Modbus TCP connection to the
boiler failed after ~24 s of connection attempts:

```
2026-08-19T12:00:00,004 DEBUG [BoilerController] Refreshing status
2026-08-19T12:00:23,743 ERROR Turn on failed
java.lang.RuntimeException: com.ghgande.j2mod.modbus.ModbusIOException:
    Connection failed for <boiler-host>/192.168.x.x:502 No route to host
  at ModbusClient.doModbusCall(ModbusClient.java:135)
  at ModbusClient.writeCoilRegister(ModbusClient.java:153)
  at BoilerController.refreshStatus(BoilerController.java:118)
  at BoilerManager.turnOn(BoilerManager.java:103)
```

The outage was transient: `BoilerMonitor` read the boiler successfully at
10:51 and again at 13:23. Nevertheless the boiler never turned on — all state
readouts between 12:00 and 17:00 show `flags[]` without `on`, and the 17:00
interval-end task logged `Turn off: already OFF`.

Wider context: the Robonect mower and the water tank level sensor on the same
network segment were failing from ~10:00 onward, so this looks like a Wi-Fi /
network segment outage that happened to cover the 12:00:00–12:00:23 window for
the boiler as well.

## Expected behavior

A transient connection failure at the interval start must not cancel heating
for the whole day. The system should keep trying to reach the desired state
(boiler ON, correct target temperature) as long as the current time is inside
a configured heating interval.

## Root cause

`BoilerManager.turnOn()` is invoked exactly once by `IntervalScheduler` at the
interval start time. Its `catch (Exception e)` block only logs
`"Turn on failed"` and returns — there is no retry and no periodic
reconciliation. `BoilerMonitor` only reads the state; it never enforces it.

Relevant code:

- `BoilerManager.turnOn()` / `turnOff()` — one-shot, exception swallowed
  (`extensions/.../boiler/BoilerManager.java`)
- `IntervalScheduler.setIntervals()` — schedules only the two edge callbacks
  (`extensions/.../utils/IntervalScheduler.java`)

## Proposed fix

Options, roughly in order of preference:

1. **Periodic reconciliation (preferred).** Inside a heating interval,
   periodically (e.g. every 5–10 min, possibly from `BoilerMonitor`'s refresh
   loop) compare the actual state with the desired state and call `turnOn()`
   again if the boiler is not ON or the target temperature does not match.
   This also self-heals cases where the boiler was manually switched off or
   rebooted mid-interval. `turnOff()` at interval end likely deserves the same
   treatment (a failed turn-off leaves the boiler heating outside the
   interval).
2. **Simple retry with backoff.** Retry the failed `turnOn()` a few times
   (e.g. every minute for 30 min). Cheaper but does not cover mid-interval
   state drift.

Open question: should a repeated failure raise a user-visible alert (web UI /
notification) instead of just an ERROR log line?

## Resolution

_Not resolved yet._
