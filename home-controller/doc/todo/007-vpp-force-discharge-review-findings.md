---
id: 007
title: Resolve the code review findings of the VPP force discharge work
type: task
status: open
priority: high
component: inverter
created: 2026-11-13
---

# Resolve the code review findings of the VPP force discharge work

**Status 2026-11-13:** finding 8 fixed, 4 decided otherwise (1, 3, 5, 10), 6 open.
Findings 3, 5 and 10 are closed by removing the code they are about: `leaveStoredForceDischarge`,
because no migration from the old version is needed, and `clearLeftoverOnce`, because a VPP mode ends
by its own duration.

## Summary

Code review of the five commits `ceb9a33..c7641df` on the branch `sunCalculator` (the work of
`006-inverter-control-via-vpp.md`), done on 2026-11-12 by reading the code. No finding is confirmed by a
test or on the inverter; check each one against the code before fixing it. Line numbers are those of
commit `c7641df`.

Findings 2, 4, 8 and 9 are in code that runs whether force discharge is enabled or not, so they matter
for any deployment of the branch. Finding 3 matters in a deployment without export.

Markers: ✅ done · ⏳ in progress · ⬜ open · ❌ decided otherwise.

## Findings

### 1. ❌ Force discharge does not cap the export when PV rises between two re-issues

`InverterManager.manageForceDischarge` (line 435) holds VPP mode 8 with `PV_UNLIMITED_W` and a battery
target worked out once a minute from the PV power at that moment
(`getForceDischargeBatteryW`). PV that rises within the minute adds to the export.

- 2026-11-13, decided by the owner: no change. The inverter's export limit caps the feed-in of a mode 8
  discharge (`testStep6ExportLimit`, 2026-11-12), and force discharge is there to empty the battery as
  fast as possible at the highest price, so PV on top of it is welcome. Recorded in 006, section
  "Scope".

### 2. ⬜ No fallback once the VPP status register is polled

`SolaxInverterModbusClient.startVppStatusPolling` (line 73) replaces the client with one that also reads
input 0x0100 and never goes back, although its Javadoc says the register is "dropped again when the read
keeps failing".

- Failure: the firmware later rejects the 0x0100 read (in `Idle` or Standby, after a firmware update).
  `refreshData()` then throws on every `getState()`, and the monitor, the metrics, `InverterManager` and
  the REST status lose the whole inverter state until the application restarts.
- Fix: count failures of the extended read and fall back to the basic client after a few in a row.

### 3. ❌ A stored Manual / ForceDischarge is cleared only while force discharge is enabled

`InverterManager.manageForceDischarge` (line 414) calls `leaveStoredForceDischarge` only when
`forceDischargeEnabled && isForceDischargeAllowed()`.

- Failure: the application starts with Manual / ForceDischarge stored by the previous implementation
  and force discharge is off, or the deployment has no export. Nothing switches the mode back, and the
  battery discharges to the grid, which is fined in a deployment without export.
- Fix: run the cleanup whatever the enabled flag is.
- 2026-11-13, decided by the owner: no migration from the old version is needed. Only the old
  implementation wrote Manual / ForceDischarge, so the cleanup is removed altogether
  (`leaveStoredForceDischarge` and its call) instead of being extended. A Manual work
  mode set by hand at the inverter stays; force discharge is refused meanwhile with "the stored work
  mode is Manual" (`VppBatteryControl.getRefusal`, `testNoStartFromManualWorkMode`).

### 4. ⬜ VPP status polling is given up for good after one short outage

`SolaxInverterModbusClient` (line 76): the three attempts of `VPP_STATUS_ATTEMPTS` come on consecutive
`getState()` calls, about 15 s at the monitor's 5 s period.

- Failure: one short network outage uses up all three. `getVppMode()` then returns 0 until restart: the
  dialog shows "Remote control: Off" while a VPP mode runs, and `clearLeftoverOnce` never sees a
  leftover mode.
- Fix: space the attempts out (for example one per several minutes) or never give up for a firmware
  that has VPP.
- 2026-11-13: `clearLeftoverOnce` is removed (finding 5), so only the wrong "Remote control: Off" in
  the dialog is left of the failure.

### 5. ❌ `clearLeftoverOnce` marks the check done before it has succeeded

`VppBatteryControl.clearLeftoverOnce` (line 122) sets `leftoverChecked` before it reads `getVppMode()`
and calls `vppDisable()`.

- Failure: the first call comes before VPP status polling has succeeded (`getVppMode()` is 0 until
  then), or `vppDisable()` throws a Modbus timeout. A long VPP mode left by a previous run or by
  `SolaxVppExperimentTest` then runs until its own duration ends.
- Fix: set the flag only after the VPP mode was really read and, if needed, ended.
- 2026-11-13, decided by the owner: `clearLeftoverOnce` is removed with its three tests
  instead of being fixed. A mode of the application lasts 180 s (`DURATION_S`), so after a restart
  it ends by itself within three minutes or is taken over by the next `hold`; `SolaxVppExperimentTest`
  ends its own modes.

### 6. ⬜ A rejected Pgrid bias `Grid` is answered with HTTP 202

`InverterHandler.applyAction` (line 90) throws inside `processAction`, which `AbstractRestHandler` runs
on an executor after the response is sent.

- Failure: `pgridBias=true` in a deployment without export gets 202, the dialog reports a successful
  save, nothing is written and the switch goes back at the next refresh. The rejection is only in the
  log.
- Fix: check before the response is sent and return an error status.

### 7. ⬜ `InverterHandler` takes "no manager" for "no export"

`InverterHandler.isExportAllowed` (line 76) returns false when `manager` is null.

- Failure: in `PiConfigurator` (export allowed, 8500 W) the `InverterManager` constructor fails, for
  example on a bad option. The status reports `exportAllowed = false`, the dialog hides the Grid,
  feed-in and force discharge settings and Pgrid bias `Grid` is rejected.
- Fix: pass the deployment's export permission to the handler instead of inferring it from the manager.

### 8. ✅ `Mode.Unknown` counts as "not running" in the negative price turn-off

`InverterManager.doPowerManagement` (line 274) with `turnOffOnNegativePrice`: `Mode.Unknown.isRunning()`
is false.

- Failure: the firmware reports a run mode code missing from the enum. `setInverterOn(true)` (0x001C,
  a stored write) is sent every quarter of an hour while the price is not negative, and the inverter is
  not turned off when it is. Before this work the unknown code threw and nothing was written.
- Fix: decide only on known modes; skip the block for `Unknown`.
- 2026-11-13, done: `setInverterOn(true)` is written only in the `Waiting` run mode
  and `setInverterOn(false)` only to a running inverter (`InverterManager.getInverterOnWrite`), so
  `Unknown` writes nothing either way. The change was made to stop the writes of 0x001C every quarter
  of an hour while the inverter sleeps, see step 4 of 006. Not run against the inverter.

### 9. ⬜ A firmware without VPP takes the client lock at every poll

`SolaxInverterModbusClient.getState` (line 56) enters the synchronized `startVppStatusPolling()` and
`isVppSupported()` at every call when the firmware has no VPP.

- Failure: a write holds the same lock for up to its 3 s timeout and the monitor's `getState()` waits
  behind it, although the firmware answer never changes.
- Fix: remember the negative answer in a volatile flag.

### 10. ❌ Javadoc of `leaveStoredForceDischarge` tells the code's history

`InverterManager` (line 450): "Force discharge used to run that way, so the state can be left by a
previous version of the application".

- Fix: keep what the method does, for example "Switches a stored Manual / ForceDischarge back to
  SelfUse; a VPP mode cannot start from it."
- 2026-11-13: no longer applies, the method is removed with finding 3.

### 11. ⬜ Javadoc of `VppBatteryControl.canExport` is out of date

Not from the review, noticed on 2026-11-13: the Javadoc (line 131) says the inverter's export limit "is
not known to apply" to a VPP mode. Since `testStep6ExportLimit` it is measured for a battery discharge
without PV and with a limit above 0, as `CLAUDE.md` and 006 already say.

- Fix: reword it to match; the refusal itself stays.
