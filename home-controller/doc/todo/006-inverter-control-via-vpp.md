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

The export during force discharge must not exceed `hardMaxExportPower`. Whether a VPP mode obeys
`ExportControl_User_Limit` (0x0042) is not measured yet. If it does not, the manager limits the battery
target itself every minute: `BatWTarget = min(dischargePowerW, hardMaxExportPower − PV power)`. Ignoring
the house load keeps it on the safe side.

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

`forceDischargeActive` becomes an owner enum, e.g. `NONE / FORCE_DISCHARGE / NEGATIVE_PRICE` (005 adds
`GRID_CHARGE`). While an owner holds the battery:

- `applyBatteryMode` (export priority) is skipped, as it is for force discharge today;
- the other VPP features do not start; negative price wins over force discharge (discharging into a
  negative-price grid makes no sense anyway);
- when the owner releases, it writes VPP mode 0 and calls `exportPriorityScheduler.applyCallback()`.

The negative price check moves from the 15-minute `doPowerManagement` to a one-minute task, next to
`manageForceDischarge`.

### Leftovers of the old control

The new code no longer writes 0x001C or 0x001F for these features, so a state left by the old version
(deployment or restart in the middle of a window) would stay forever. Independent of any option:

- stored Manual / ForceDischarge is still adopted and switched back to SelfUse, as today;
- `setInverterOn(true)` is still written when the inverter is `Waiting` outside a negative price window.

A VPP mode must not be started from the Manual work mode (protocol rule). With any other stored Manual
state (Stop, ForceCharge) the manager leaves the inverter alone and logs a warning.

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
- At startup, and whenever the owner is `NONE`, a non-zero 0x0100 is cleared with VPP mode 0. Mode 0
  ends a running mode within about 5 s and is harmless when none runs (experiment).

### Metrics and status

The mode the inverter itself reports (0x0100) is already there: `vpp_mode` in `home-inverter`, a
"VPP mode" row in the "Modes and decisions" panel of the `home-energy` dashboard, `vppMode` in the
`InverterHandler` status, and in the UI an icon on the inverter item and a "Remote control" row in the
dialog.

Still to add: the owner as `battery_owner` (string) in `home-inverter-manager`, with `force_discharge`
and `inverter_off_negative_price` kept as derived bools for the existing Grafana panels, and the owner
in the `InverterHandler` status and the dialog. Document in `doc/metrics.md`.

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
| 6 | Does `ExportControl_User_Limit` cap the export? | **open**; needs the export limit lowered below the discharge power |
| 6 | Is PV really not limited by a high `PvWTarget`? | **open**; needs PV |
| 7 | Mode 8 with `PvWTarget = 0` by day: PV off, battery charging from the grid, how fast PV goes down and comes back, what happens at SoC 100 % | **open**; needs PV |
| 8 | Mode 4 through 0x00A0 | works |
| 9 | For 005: register offsets of modes 5–7 | as in SolaX's tables (duration at 0xA1) |
| 9 | For 005: a charge command while `Idle` | **open**; `VPPExitIdleEn` (read 0x00B4 / write 0x00F4 ★) is disabled |

The open rows of step 6 decide whether the manager has to limit the battery target itself; step 7 comes
before the negative price case is implemented.

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
- **Start from `Idle`:** not measured. Negative prices come with PV, so the inverter is `Normal` then,
  but a force discharge in the evening starts from `Idle` whenever the stored min SoC is above the SoC
  (as now, with the min SoC at 100 %). A Manual / ForceCharge write wakes the inverter (measured on
  2026-11-08, see 005); a VPP command may not, with `VPPExitIdleEn` disabled.
- **Deployments:** `PiPeConfigurator` has no export (`hardMaxExportPower = 0`) and no battery parameters
  for force discharge; the negative price behavior there must stay as it is or be configurable.

## Implementation steps

1. Experiment (above), results recorded in `doc/inverter/solax_vpp_protocol.md`. Night part done; the
   open rows remain.
2. Client layer: `ModbusClient.writeMultipleRegisterValues`, the mode 8 block builder with its layout
   test (`SolaxVppBlockTest`), `vppPvBattery` and `vppDisable` with the firmware gate, `getVppMode()`
   and the run mode 20, shown in the metrics, Grafana and the UI. Done.
3. Owner enum in `InverterManager`; force discharge on mode 8 with the SoC floor; leftovers of the old
   control; startup cleanup of a running VPP mode; tests.
4. Negative price on mode 8 in a one-minute task, with the old turn-off kept behind an option until the
   first real negative day is checked. Needs step 7 of the experiment first.
5. Metrics, `InverterHandler` status and `doc/metrics.md`.
6. Inverter documentation section on VPP ownership (new `doc/inverter.md` or similar), referenced from
   `CLAUDE.md`.
7. Manual verification by the owner on the real inverter.
