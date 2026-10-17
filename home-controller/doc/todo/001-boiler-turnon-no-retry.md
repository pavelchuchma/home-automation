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
exactly 12:00. On 2026-10-16 it failed again, this time because the gateway
rejected the power-on write while the boiler display was still on an info
screen. In both cases the exception was logged and swallowed; nothing ever
retried, so the boiler stayed OFF for the entire interval and the water was
not heated that day.

## Observed behavior

### Case 1 — 2026-08-19: gateway unreachable

The 12:00 cron task fired but the Modbus TCP connection to the
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

### Case 2 — 2026-10-16: write rejected while display not unlocked

The gateway was reachable and `refreshStatus()` succeeded, but the state read
~8.7 s later showed the display still in `infoTh` mode instead of `unlocked`.
The immediately following power-on coil write was rejected:

```
2026-10-16T12:00:00,003 DEBUG [BoilerController] Refreshing status
2026-10-16T12:00:08,777 DEBUG [BoilerController] Refreshing state
2026-10-16T12:00:08,983 DEBUG [BoilerController] Mode: infoTh, statusAge: 0, tSU: 37, ..., tTarget: 42, flags[]
2026-10-16T12:00:08,983 DEBUG [InverterManager] Turn on: OFF -> ON
2026-10-16T12:00:08,983 DEBUG [BoilerController] Setting power on: true
2026-10-16T12:00:08,983 DEBUG [ModbusClient] writing coil register: 0x00D2=true
2026-10-16T12:00:09,142 ERROR Turn on failed
java.lang.RuntimeException: com.ghgande.j2mod.modbus.ModbusSlaveException: Slave Device Failure
  at ModbusClient.writeCoilRegister(ModbusClient.java:153)
  at BoilerController.setPowerOn(BoilerController.java:123)
  at BoilerManager.turnOn(BoilerManager.java:115)
```

The refresh took the same ~8–9 s as on every other day in the preceding two
weeks (all of which turned on fine with `Mode: unlocked`); the display simply
had not returned from the last info screen (`Th`) yet. The gateway refuses to
press the power key in any mode other than `unlocked` and reports it as a
Modbus "Slave Device Failure" exception. `BoilerMonitor` read `Mode: unlocked`
again at 12:04. The boiler stayed OFF all day and 17:00 logged
`Turn off: already OFF`; the upper tank temperature dropped from 41 °C to
31 °C by the evening.

This shows the failure is not purely a network problem: any step inside
`turnOn()` can fail and there is no second chance.

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

Secondary cause (case 2): `turnOn()` writes to the gateway right after reading
the state without checking `State.getDisplayMode()`. A write issued while the
display is not `unlocked` is rejected by the gateway.

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

3. **Wait for `unlocked` before writing.** Independently of the above, after
   `refreshStatus()` re-read the state until `displayMode == unlocked` (with
   a bounded timeout) and only then issue `setTargetTemp` / `setPowerOn` /
   `turnEHeatOn`. This removes the case-2 race entirely instead of relying on
   the retry to paper over it.

Open question: should a repeated failure raise a user-visible alert (web UI /
notification) instead of just an ERROR log line?

## Resolution

_Not resolved yet._
