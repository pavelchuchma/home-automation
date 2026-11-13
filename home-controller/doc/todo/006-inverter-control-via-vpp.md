---
id: 006
title: Drive the inverter's temporary battery modes through the VPP interface instead of EEPROM settings
type: enhancement
status: in-progress
priority: medium
component: inverter
created: 2026-11-11
---

# Drive the inverter's temporary battery modes through the VPP interface instead of EEPROM settings

## Summary

`InverterManager` switches the inverter's behavior by rewriting stored settings: the work mode
(`SolarChargerUseMode` 0x001F ★) for force discharge and `SystemON_OFF` (0x001C ★) for the negative
price turn-off. ★ registers are written to EEPROM on change, and EEPROM endurance is limited and not
published.

SolaX inverters have a VPP (remote control) interface for exactly this: a mode command written with
FC 0x10 that runs for a given duration and then returns the inverter to its stored work mode. It is held
in RAM. The protocol is transcribed in `doc/inverter/solax_vpp_protocol.md`.

This issue moves the temporary behaviors onto VPP and introduces one owner of the battery while a VPP
mode runs.

## Motivation

1. **Active inverter management.** The plan is to manage the inverter actively from the weather forecast
   and the spot prices. That switches the battery behavior many times a day, which stored settings cannot
   carry: at tens of changes a day even an optimistic EEPROM endurance is used up within years. VPP is
   the control channel that makes it possible, and this issue builds and proves it on the two existing
   temporary behaviors. The active management itself is out of scope.
2. **Grid charging** (`005-battery-grid-charge-planner.md`) costs 4–6 EEPROM writes per plan night as
   designed. It will be reworked on top of this separately.
3. **Failsafe.** Today an app crash leaves the inverter turned off or in Manual / ForceDischarge until the
   app is back. A VPP mode with a duration ends by itself (confirmed by the experiment).
4. **No grid check after a negative price window:** the inverter stays on instead of spending ~5.5 min in
   `Checking`.

Saving today's EEPROM writes is not the main gain. In the current configuration force discharge and the
export priority timer are off and the high tariff reserve is 0, so the only moved feature that writes is
the negative price turn-off, and the buy price is negative only when the spot price is below about
−1.2 CZK/kWh (distribution fee ~1.4 CZK/kWh, 21 % VAT), a few days a year. The most frequent writer
today is most likely the export limit, which stays a stored setting (see Scope).

## Current writes to the inverter

| Feature | Code | Register | EEPROM | Frequency |
| --- | --- | --- | --- | --- |
| Force discharge | `startForceDischarge` / `stopForceDischarge` | 0x001F Manual ↔ SelfUse, 0x0020 ForceDischarge | 0x001F ★ | two 0x001F writes per discharge block; disabled today |
| Negative price turn-off | `doPowerManagement` | 0x001C | ★ | off/on per negative window (a few days a year), plus `setInverterOn(true)` every 15 min while `Idle` (same value) |
| Export priority timer | `applyBatteryMode` | 0x001F SelfUse ↔ FeedInPriority | ★ | two per day when enabled; disabled today |
| High tariff reserve | `applyMinBatterySoc` | 0x0061 | ★ | two per day when the reserve is > 0 (today 0) |
| Export limit on negative sell price | `doPowerManagement` | 0x0042 | ★ | two per negative-sell window. The net sell price is negative whenever spot with VAT is below the sell fee (0.45 CZK/kWh), which is common on sunny days. Not measured; count the sign changes of `sell_price_net` in `home-inverter-manager`. |
| PgridBias | `InverterHandler` | 0x008D | ★ | manual, rare |

## Evidence from homeassistant-solax-modbus

The integration drives Gen4 inverters through the same interface
(<https://github.com/wills106/homeassistant-solax-modbus>, `plugin_solax.py`, and its documentation page
<https://homeassistant-solax-modbus.readthedocs.io/en/latest/solax-mode8-modbus-power-control/>):

- Modes 8 and 9 are written with FC 0x10 from 0x00A0 on Gen4, with the same field order as in
  `solax_vpp_protocol.md` (mode, set type, PV limit U32, battery power S32, duration or target SoC, next
  motion).
- Modes 1–7 are written **only through the legacy block 0x007C**. Nothing there writes mode 4 to 0x00A0,
  so that path has no field evidence.
- Before a write it reads input register 0x0100 and accepts values 0–9, so the status register most
  likely reflects the new protocol modes too.
- Re-sending the command periodically ("autorepeat") is its normal way of holding a mode.
- Its "Negative Injection and Consumption Price" submode of mode 8 is our negative price case: PV limited
  and the battery charged from the grid.
- Known issues listed there: the mode 8 duration "seems to be ignored by the SolaX firmware", the inverter
  sometimes ends in VPP mode 6 afterwards, and PV can take a minute or more to reach its target. The
  integration's default next motion is 0xA1 (switch to mode 6), which may explain the second one.
- "On the Gen4, these actions are not stored in EEPROM, so they can be executed frequently." This is the
  only source for the RAM claim and it cannot be verified on the device.

The experiment on the home inverter (below) confirmed the block layout, the status register and the
re-sending, and showed that mode 4 works through 0x00A0 as well. It did not reproduce the duration issue:
modes 4, 7 and 8 ended on time.

## Scope

**Moved to VPP:**

- **Force discharge → VPP mode 8** (PV & BAT individual): `PvWTarget` at or above the PV peak power (PV
  not limited), `BatWTarget = +dischargePowerW` (positive is discharge), `NextMotion = 0xA0` (back to the
  stored work mode). The stored work mode stays SelfUse / FeedInPriority, so 0x001F is no longer written
  for force discharge and Manual mode is not used at all. Mode 4 (Push Power) is the natural mode for
  this and works through 0x00A0 too (experiment), but mode 8 for both features means one block layout
  and one client method.
- **Negative buy price → VPP mode 8** instead of turning the inverter off: `PvWTarget = 0`,
  `BatWTarget = −chargePowerW` while SoC < 100 %, otherwise 0. The house runs from the grid as today, the
  battery additionally charges from the grid while the buy price is negative, and the inverter stays on.
  0x001C is no longer written.
  - 2026-11-13, decided by the owner: where the firmware has VPP, the turn-off through 0x001C is
    replaced in one go, with no option to switch back to it. Where it has no VPP, the turn-off stays,
    with fewer writes of 0x001C (see step 4).

The export during force discharge must not exceed `hardMaxExportPower`. Whether a VPP mode obeys
`ExportControl_User_Limit` (0x0042) is not measured yet. If it does not, the manager limits the battery
target itself every minute: `BatWTarget = min(dischargePowerW, hardMaxExportPower − PV power)`. Ignoring
the house load keeps it on the safe side.

Measured 2026-11-12: the inverter does obey the limit in mode 8 (4 kW discharge, limit lowered from
8.5 kW to 1 kW, feed-in at 1 kW within 3–5 s). The manager's own limit of the battery target is already
implemented (step 3) and stays as a second guard: the measurement was without PV and with a limit
above 0.

Decided 2026-11-13 (code review, finding 1 of `007-vpp-force-discharge-review-findings.md`): the battery
target is worked out once a minute from the PV power at that moment, so PV that rises between two
re-issues adds to the export. That is accepted and `PvWTarget` stays unlimited. The inverter's export
limit, which `doPowerManagement` keeps at `hardMaxExportPower` while the net sell price is positive, is
the cap, and force discharge is there to empty the battery as fast as possible at the highest price, so
PV on top of it is welcome. The limit with PV running is still an open row of the experiment.

A VPP discharge does not stop at `SelfUse_Discharge_MinSoC` (experiment: with the min SoC at 42 % it ran
at full power from 43 % to 39 %, until the test ended it). The only floor is `forceDischargeMinimalSoc`,
which `shouldDischargeNow` evaluates every minute; `manageForceDischarge` must end the mode with VPP
mode 0 as soon as the SoC is at or below it, whatever the prices say. After a crash the discharge goes on
for the rest of the duration, which at 5.9 kW and 180 s is up to about 0.3 kWh (3 % SoC) below the floor.

**Kept as stored settings** (no VPP equivalent or not worth it):

- Export priority timer: FeedInPriority exports first and charges the battery only from the surplus above
  the export limit, and it discharges the battery to its own min SoC when PV is short. No VPP mode does
  that; mode 5 (battery idle) is the closest and behaves differently. Emulating it needs a control loop
  in seconds.
- Export limit on negative sell price: VPP has no grid-meter export limit (mode 1 controls the inverter AC
  port, mode 8 only caps PV at a fixed power). Replacing it means a PV limit that follows the house load
  in a loop of seconds, as the Home Assistant "Negative Injection Price" submode does. That belongs to the
  active management, not here, although this is probably the most frequent EEPROM writer today.
- High tariff reserve, PgridBias: few writes.

## Design

### Client layer

- `ModbusClient`: `writeMultipleRegisterValues(int address, int[] values)` (FC 0x10 with a register array).
  The existing `writeMultipleRegisterValue` writes one register only.
- `SolaxInverterModbusClient`: `vppDisable()` and `vppPvBattery(int pvLimitW, int batteryW, int durationS)`.
  No generic "write any VPP block" method, so 0x007C / 0x00A0 can never go through
  `writeSingleRegisterValue` (in the FC 0x06 map they are `PowerFactor_Curve_PfLockInPoint` ★ and
  `MeterFunction` ★).
- The block is built by a package-private pure static function returning the register array (S32/U32
  LSB word first), unit tested against SolaX's own example
  `0008 0001 0000 0000 EC78 FFFF 0708 00A0`. A recording client as in `InverterHandlerTest` overrides the
  setter itself and cannot check the layout.
- Firmware gate: the application also runs in a deployment with an older inverter, where VPP need not
  work but must fail the same way every time. `vppPvBattery` and `vppDisable` throw
  `UnsupportedOperationException` before any write when the ARM version is below 1.45 (holding 0x0080 /
  0x0083), and `isVppSupported()` lets the manager ask first. The version is the X3-Hybrid-G4
  requirement; other models are not told apart.
- Status: input register 0x0100 is exposed as `getVppMode()`. It shows the modes of the new protocol,
  but up to about 13 s behind the battery (experiment), so it serves the status display and the startup
  cleanup, not as a confirmation of a command just sent. An inverter without VPP may reject the read and
  with it the whole poll, so the register is polled only where `isVppSupported()` holds; elsewhere
  `getVppMode()` is 0.
- Run mode: while a VPP mode runs, input 0x0009 reads 20. `getMode()` used to index `Mode.values()` and
  threw on it, which broke the monitor, the metrics and the inverter dialog for as long as the mode ran.
  It now maps the register by code: 20 is `Mode.RemoteControl`, any other unlisted code is
  `Mode.Unknown` with one warning in the log. `doPowerManagement` takes `RemoteControl` as a running
  inverter (`Mode.isRunning()`).
- No unlock (0x0000 = password): the inverter reports unlocked (input 0x0054 = 1) and takes the blocks
  without it.

### Owner state in `InverterManager`

`forceDischargeActive` became an owner enum in `VppBatteryControl`, which sends the VPP commands for the
manager: `NONE / FORCE_DISCHARGE` so far, `NEGATIVE_PRICE` comes with the negative price case (005 adds
`GRID_CHARGE`). While an owner holds the battery:

- `applyBatteryMode` (export priority) is skipped, as it is for force discharge today;
- the other VPP features do not start; negative price wins over force discharge (discharging into a
  negative-price grid makes no sense anyway);
- when the owner releases, it writes VPP mode 0 and calls `exportPriorityScheduler.applyCallback()`.

The negative price check moves from the 15-minute `doPowerManagement` to a one-minute task, next to
`manageForceDischarge`.

### Leftovers of the old control

The new code no longer writes 0x001C or 0x001F for these features, so a state left by the old version
(deployment or restart in the middle of a window) would stay forever:

- stored Manual / ForceDischarge is switched back to SelfUse while force discharge is enabled and
  allowed, the same condition under which the old code adopted it. Without that condition a Manual /
  ForceDischarge set by hand would be reverted within a minute, also in a deployment that never used
  the feature;
  - ❌ 2026-11-13, decided by the owner (finding 3 of `007-vpp-force-discharge-review-findings.md`): no
    migration from the old version is needed, so this cleanup (`leaveStoredForceDischarge`) is removed.
    The application never writes 0x001F for force discharge. A Manual work mode set by hand stays, and
    force discharge is refused meanwhile with "the stored work mode is Manual";
- `setInverterOn(true)` is still written when the inverter is `Waiting` outside a negative price window.
  - ❌ 2026-11-13, decided by the owner: no migration from the old version is needed, so with VPP this
    write is not kept as a cleanup. It is the live "on" half of the turn-off in `doPowerManagement`,
    which stays for a firmware without VPP (see step 4).

A VPP mode is not started when the inverter cannot take it, and a mode the owner had running is ended.
This is a refusal, not an exception: the check runs every minute and a sleeping inverter is an expected
state. The reason is logged as a warning with the SoC and the stored min SoC, once until it changes or
the owner stops wanting the battery, so every discharge block reports it again.
`VppBatteryControl.getRefusal(owner)` keeps it for the status in step 5. The reasons:

- the stored work mode is Manual (protocol rule), whatever the manual mode;
- the inverter is not running (`Idle`, `Waiting`, a fault). It is not woken from `Idle`, see the open
  points;
- the firmware has no VPP. Force discharge then never starts; it no longer falls back to Manual /
  ForceDischarge.

### Deployments without export

Added 2026-11-12 after an audit of every write to the inverter. A deployment with
`hardMaxExportPower = 0` (`PiPeConfigurator`) must never feed the grid, whatever the options, the UI or a
VPP mode ask for; `InverterManager.isExportAllowed()` is the single switch.

- ✅ `VppBatteryControl` refuses every mode that can export, with the reason "export is not allowed in
  this deployment": a discharging battery, and any PV limit above 0, because mode 8 sends what the house
  does not take to the grid. The export limit caps a battery discharge in mode 8 (measured 2026-11-12),
  but it is not measured with PV running or at 0 W, so the refusal stays.
  Charging with PV off passes, which is what the negative price case and 005 need.
- ✅ Force discharge was already tied to `hardMaxExportPower > 0`; the battery target
  `min(dischargePowerW, hardMaxExportPower − PV power)` is never positive there either.
- ✅ The feed-in priority timer no longer writes the work mode there (`getTimerBatteryMode`). Before, the
  option switched the inverter to FeedInPriority and only the inverter's own limit stood against export.
- ✅ Pgrid bias `Grid` (feed-in held at ~+40 W) is rejected by the `InverterHandler` action there.
- ✅ The inverter dialog hides the feed-in priority timer, force discharge and Pgrid bias where the status
  says `exportAllowed` is false. The options stay editable on the Options page, without effect.
- ✅ No active correction of the inverter's own settings (decided by the owner: changing them takes the
  admin password, so it does not happen by accident). `doPowerManagement` logs an error, once until it
  changes, when the stored export limit is not 0 or Pgrid bias is `Grid`.
- The export limit register (0x0042) was and is written only where export is allowed.

Not covered: what is set at the inverter itself, and how fast its zero-export regulation follows a load
drop. The hardware tests (`SolaxVppExperimentTest`, `SolaxInverterModbusClientTest`) discharge to the grid
and change the export limit; they must never be pointed at such an inverter.

### Duration and refresh

Every VPP command carries `ExecDuration`; when it runs out the inverter returns to the stored work mode
by itself (experiment: 5–15 s after the duration, straight into SelfUse without a `Checking` phase). That
is the failsafe: an app crash or a lost Modbus connection ends the mode within one duration.

- `manageForceDischarge` and the negative price check run every minute; while the decision holds they
  re-issue the command with a short duration (proposed 180 s), so a crash leaves at most three minutes
  of an unmanaged VPP mode. A re-issued command restarts the duration and the battery power does not dip
  (experiment). The writes are RAM-only, so re-issuing every minute is fine.
- Alternative: one command per block with the duration up to the block end. Fewer writes, but a crash
  leaves the mode running until then.
- At startup a non-zero 0x0100 is cleared with VPP mode 0, once, in the first minute. Mode 0 ends a
  running mode within about 5 s and is harmless when none runs (experiment). Later a mode without an
  owner is left alone: it ends by its own duration, and clearing it every minute would also end the
  modes of `SolaxVppExperimentTest`.
  - ❌ 2026-11-13, decided by the owner (finding 5 of `007-vpp-force-discharge-review-findings.md`):
    the startup cleanup (`clearLeftoverOnce`) is removed. The 180 s duration is the failsafe after a
    restart as well, and the experiment test ends its own modes. Register 0x0100 now serves the status
    display only.

### Metrics and status

The mode the inverter itself reports (0x0100) is already there: `vpp_mode` in `home-inverter`, a
"VPP mode" row in the "Modes and decisions" panel of the `home-energy` dashboard, `vppMode` in the
`InverterHandler` status, and in the UI an icon on the inverter item and a "Remote control" row in the
dialog.

The owner is the string field `battery_owner` (`NONE`, `FORCE_DISCHARGE`) in `home-inverter-manager`,
with a "Battery owner" row in the same panel; `force_discharge` stays as a bool derived from it for the
existing row. The `InverterHandler` status has `batteryOwner`, and `refusedOwner` with `refusal` (both
null when nothing is refused); the dialog shows a "Battery held by" row and, while a feature is refused,
a "<feature> cannot run" row with the reason. The refusal reason is in the dialog and the log only, not
in the metrics. `inverter_off_negative_price` still reports the option, not a state; it becomes derived
from the owner with the negative price case (step 4).

## Experiment on the real inverter

Run by the owner with `SolaxVppExperimentTest` (HW-touching, never run automatically), one test method
per step. The night part was done on 2026-11-11 from SelfUse / `Normal` without PV; the measured values
are in `doc/inverter/solax_vpp_protocol.md`, section "Verified on the home inverter".

| Step | Question | Result |
| --- | --- | --- |
| 1 | Snapshot before any write | unlocked, `VPPExitIdleEn` disabled, status block 0x0100–0x0121 readable |
| 2 | Mode 8 charge: accepted, battery charging, 0x0100 = 8, stored settings unchanged? | yes to all; the run mode reads 20 meanwhile |
| 3 | Does the mode end by itself after the duration? | yes, 5–15 s after it |
| 4 | Does a re-issued command restart the duration, does the battery power dip? | it restarts, no dip |
| 5 | Mode 0 while a mode runs | back in the stored mode within about 5 s |
| 6 | Mode 8 discharge: does it stop at min SoC? | **no**, it discharges below it |
| 6 | Is a `PvWTarget` above the PV peak accepted? | yes (30 kW) |
| 6 | Does `ExportControl_User_Limit` cap the export? | **yes** (2026-11-12, `testStep6ExportLimit`): 4 kW discharge, feed-in 3.47 kW → 1.0 kW within 3–5 s of the limit lowered to 1 kW, by a lower battery power. Without PV and with a limit above 0 only |
| 6 | Is PV really not limited by a high `PvWTarget`? | **open**; needs PV |
| 7 | Mode 8 with `PvWTarget = 0` by day: PV off, battery charging from the grid, how fast PV goes down and comes back, what happens at SoC 100 % | **open**; needs PV |
| 8 | Mode 4 through 0x00A0 | works |
| 9 | For 005: register offsets of modes 5–7 | as in SolaX's tables (duration at 0xA1) |
| 9 | For 005: a charge command while `Idle` | **open**, not needed for force discharge; `VPPExitIdleEn` (read 0x00B4 / write 0x00F4 ★) is disabled |

The export limit row of step 6 says the manager need not limit the battery target itself; it does so
anyway, as a second guard. Step 7 comes before the negative price case is implemented.

Firmware: ARM 1.54 / DSP 1.56 (SolaX Cloud "ARM IC" / "DSP IC"); the new 0x00A0 protocol needs ARM
≥ 1.45 on X3-Hybrid-G4.

The legacy 0x007C protocol is a fallback for force discharge only: it has no mode 8, and its modes 4–7
run a fixed 10 minutes and then switch to VPP mode 6 instead of the stored work mode, so they must be
re-issued within 10 minutes and always ended with mode 0.

## Open points

- **RAM-only is taken on trust:** it comes from the Home Assistant documentation, SolaX's article does not
  mention EEPROM, and the device offers no way to check it.
- **Negative price completeness:** `IntervalPrice.price()` is spot with VAT plus `distributionFee`. Grid
  charging during negative prices is only a gain if every per-kWh charge is in `distributionFee`. Check
  the configured fee against an invoice before enabling mode 8 charging; the current turn-off relies on
  the same assumption.
- **Import limit:** mode 8 charging adds ~4.8 kW to the house load; check against the main breaker.
- **Start from `Idle`:** decided not to wake the inverter, and not measured. The inverter sleeps only
  when the SoC is below the stored min SoC, and force discharge wants the SoC above
  `forceDischargeMinimalSoc`, so both hold only with the stored min SoC above that floor: a min SoC set
  to 100 % by hand, or the `HOLD` state of 005, which is planned for cheap hours. Force discharge is
  refused then (see the leftovers section). If it is needed later: a Manual / ForceCharge write and a
  lowered min SoC wake the inverter in about 5 minutes (see 005) at two stored writes per block; whether
  a VPP command does, with `VPPExitIdleEn` (write 0x00F4 ★, now disabled) enabled or not, is the open
  experiment row.
- **Deployments:** `PiPeConfigurator` has no export (`hardMaxExportPower = 0`) and no battery parameters
  for force discharge; the negative price behavior there must stay as it is or be configurable. Export
  itself is closed there, see "Deployments without export" (2026-11-12).
  - 2026-11-13, decided by the owner: the firmware decides, not the deployment and not an option.
    With VPP the negative price case runs as mode 8; mode 8 with PV off and the battery charging or idle
    is allowed without export (`testNoExportAllowsChargingWithPvOff`). Without VPP the inverter is
    turned off through 0x001C as today (see step 4). The firmware version of the second deployment is
    not known.

## Implementation steps

1. Experiment (above), results recorded in `doc/inverter/solax_vpp_protocol.md`. Night part done; the
   open rows remain.
2. Client layer: `ModbusClient.writeMultipleRegisterValues`, the mode 8 block builder with its layout
   test (`SolaxVppBlockTest`), `vppPvBattery` and `vppDisable` with the firmware gate, `getVppMode()`
   and the run mode 20, shown in the metrics, Grafana and the UI. Done.
3. Owner enum (`VppBatteryControl`); force discharge on mode 8 with the SoC floor and the battery target
   limited to `hardMaxExportPower − PV power`; leftovers of the old control; startup cleanup of a running
   VPP mode; tests (`VppBatteryControlTest`, `ForceDischargeTest`). Done, not yet run against the
   inverter.
4. Negative price on mode 8 in a one-minute task, with the old turn-off kept behind an option until the
   first real negative day is checked. Needs step 7 of the experiment first.
   - 2026-11-13, decided by the owner: ❌ no option for the old turn-off. A negative day can be months
     away and the option would be dead code until then. Step 7 of the experiment measures what mode 8
     does with `PvWTarget = 0`; the price condition does not need the inverter and is covered by unit
     tests. Once step 7 confirms it, mode 8 replaces the turn-off at once where the firmware has VPP.
   - 2026-11-13, decided by the owner: a firmware without VPP (`isVppSupported()` false) keeps the
     turn-off through 0x001C, so that code stays, chosen by the firmware. It must not write 0x001C more
     than needed. Today `setInverterOn(true)` is written every 15 minutes whenever the inverter is not
     running and the price is not negative, that is all night in `Idle`.
   - ✅ Proposed 2026-11-13, decided and done the same day (see below): write `setInverterOn(true)`
     only in the `Waiting` run mode, which is what
     a turned-off inverter reports (`SolaxInverterModbusClientTest.testSetSystemOff` waits for it); the
     spec has no read register for `SystemON_OFF`, so the stored value cannot be compared. It needs no
     state, so an application restart inside a negative window still turns the inverter back on, and a
     lost write is repeated at the next quarter of an hour. That leaves one write per turn-off and one
     per turn-on. Also closes finding 8 of 007 (`Mode.Unknown` no longer triggers a write). To check
     first: that a turned-off inverter stays in `Waiting` for the whole window, also after sunset, and
     does not go to `Idle`; the run mode of a past negative day is in the metrics. This can be done
     now, before step 7 of the experiment.
   - 2026-11-13, InfluxDB checked (`home-price`, `home-inverter`, `home-inverter-manager`, data from
     2026-10-17): no interval with a negative buy price, the lowest is 3.33 CZK/kWh, so what a
     turned-off inverter reports over a whole window is still not known. The option is on at home
     in every sample. The inverter was not running for about 6760 minutes (`Idle` 6602, `Checking` 155,
     `Waiting` 6), which is about 450 writes of `setInverterOn(true)` in four weeks. `Waiting` by itself
     shows only as a single one-minute sample when the inverter wakes from `Idle`, before `Checking`
     (6 times, none at a quarter of an hour), so the `Waiting` condition would leave almost no write
     outside a negative window.
   - 2026-11-13, application logs on the Pi checked (2026-08-15 to today, the price and the run mode
     are logged at every 15-minute check): the lowest buy price is 1.03 CZK/kWh and
     `setInverterOn(false)` was never written, so the logs do not show a turned-off inverter either.
     `setInverterOn(true)` was written 823 times: 810 in `Idle`, 12 in `Checking`, 1 in `Waiting`. With
     the `Waiting` condition it would have been one write in three months. Whether a turned-off
     inverter stays in `Waiting` can only be seen by turning it off (`testSetSystemOff`, two writes of
     0x001C) and watching the run mode for a few hours, or on the next negative day.
   - ✅ 2026-11-13, decided by the owner and done: turned on only from `Waiting`, with no further
     safeguard (`InverterManager.getInverterOnWrite`, tested in `ForceDischargeTest`). Accepted risk: if
     a turned-off inverter leaves `Waiting` during a long window, it is not turned back on; that will
     show as an inverter that stays off after a negative price, and is fixed then. Not run against the
     inverter. The change is independent of the rest of step 4, which still waits for step 7 of the
     experiment.
5. Metrics, `InverterHandler` status and `doc/metrics.md`. Done (2026-11-12): `battery_owner` in the
   metrics and Grafana, the owner and the refusal reason in the status and the dialog. Not seen in a
   browser, dashboard not pushed.
6. Inverter documentation section on VPP ownership (new `doc/inverter.md` or similar), referenced from
   `CLAUDE.md`.
7. Manual verification by the owner on the real inverter.

## Current state (2026-11-12)

Steps 1 (night part), 2, 3 and 5 are committed on the branch `sunCalculator`, not pushed. The export
guards for deployments without export ("Deployments without export") are done on top of them. The build
and the hardware-free tests pass. Force discharge through VPP has not run on the inverter, and the feature is
disabled in the configuration (`inverter.manager.force.discharge=false`), so deploying changes nothing
by itself.

Next:

- ✅ Step 5: `battery_owner` in `home-inverter-manager`, the owner and the refusal reason in the
  `InverterHandler` status and the inverter dialog (2026-11-12).
- The day part of the experiment needs PV and the owner's permission for every run:
  `testStep7PvOffGridCharge` and `testStep6PvTargetAbovePeak`. Step 4 waits for it.
- ✅ The export limit row of step 6: `testStep6ExportLimit` run on 2026-11-12 at 22:50, result in the
  experiment table and in `doc/inverter/solax_vpp_protocol.md`. The stored limit was read back as
  8500 W afterwards. It discharges 4 kW in mode 8 with the stored limit untouched, lowers
  the limit to 1 kW, compares the mean feed-in of the two phases and logs a `RESULT:` line. It costs two
  stored writes of 0x0042 and about 0.25 kWh from the battery. The application rewrites 0x0042 at every
  quarter of an hour, so the test starts only 30 s to 9 min after one, and it refuses to start outside
  the `Normal` run mode, below SoC 30 % or with the stored limit below 4 kW. With a feed-in below 2 kW
  in the first phase (house load too high) it ends before any stored write.

- 2026-11-13: the code review of the branch is in `007-vpp-force-discharge-review-findings.md`. Finding 1
  (export not capped when PV rises) is decided as no change, see "Scope"; the other ten are
  open.
- 2026-11-13: findings 3, 5 and 10 are decided otherwise, by removing code: the
  cleanup of a stored Manual / ForceDischarge, see "Leftovers of the old control", and the startup
  cleanup of a running VPP mode, see "Duration and refresh". Both were part of step 3.
- 2026-11-13: step 4 is changed by the owner: where the firmware has VPP, the negative price turn-off
  is replaced by mode 8 in one go once step 7 of the experiment is done, with no option for the old
  turn-off and no wait for a negative day. A firmware without VPP keeps the turn-off through 0x001C.
- 2026-11-13: the turn-off writes `setInverterOn(true)` only from `Waiting` now, instead of every
  15 minutes while the inverter is not running (823 writes in three months of logs). Not run
  against the inverter. Closes finding 8 of 007.

Loose ends:

- `grafana/home-energy.json` has the "VPP mode" and "Battery owner" rows, but the dashboard is not
  pushed to Grafana (`script/grafana_dashboards.py push`).
- The UI changes (icon on the inverter item, the "Remote control", "Battery held by" and
  "<feature> cannot run" rows in the dialog, the settings hidden without export) were not seen in a
  browser.
- `SolaxInverterModbusClient.State.getBatteryMode()` and `getManualMode()` index the enum by the register
  value; a work mode code above 3 (PeakShaving, TOU) would throw. `getMode()` was fixed the same way in
  this work.
- The firmware gate checks the ARM version only, not the inverter model. The model of the inverter in
  the second deployment is not known.
- 005 does not have two findings of the experiment yet: `VPPExitIdleEn` is disabled, and VPP modes 5–7
  take the duration at 0x00A1 as in SolaX's tables.
- The application log on the Pi from the experiment night (2026-11-11 23:35 to 2026-11-12 00:23) was
  not read. The version running then could not map the run mode 20.
- In `SolaxVppExperimentTest`, `testStep9ChargeOnlyExampleLayout` was left out on purpose (the table
  layout was confirmed), and `PV_NO_LIMIT_W = 10_000` is a guess, the PV peak power is not in the
  repository.
