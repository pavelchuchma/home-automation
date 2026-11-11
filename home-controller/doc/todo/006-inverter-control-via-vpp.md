---
id: 006
title: Drive the inverter's temporary battery modes through the VPP interface instead of EEPROM settings
type: enhancement
status: open
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
   app is back. A VPP mode with a duration ends by itself (to be confirmed, see the experiment).
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

## Scope

**Moved to VPP:**

- **Force discharge → VPP mode 8** (PV & BAT individual): `PvWTarget` at or above the PV peak power (PV
  not limited), `BatWTarget = +dischargePowerW` (positive is discharge), `NextMotion = 0xA0` (back to the
  stored work mode). The stored work mode stays SelfUse / FeedInPriority, so 0x001F is no longer written
  for force discharge and Manual mode is not used at all. Mode 4 (Push Power) is the natural mode for
  this, but it is unproven through 0x00A0; using mode 8 for both features also means one block layout
  and one client method.
- **Negative buy price → VPP mode 8** instead of turning the inverter off: `PvWTarget = 0`,
  `BatWTarget = −chargePowerW` while SoC < 100 %, otherwise 0. The house runs from the grid as today, the
  battery additionally charges from the grid while the buy price is negative, and the inverter stays on.
  0x001C is no longer written.

The export during force discharge must not exceed `hardMaxExportPower`. If the experiment shows that a
VPP mode ignores `ExportControl_User_Limit` (0x0042), the manager limits the battery target itself every
minute: `BatWTarget = min(dischargePowerW, hardMaxExportPower − PV power)`. Ignoring the house load keeps
it on the safe side.

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
- Firmware gate: the VPP methods refuse to write when the ARM version is below 1.45 (holding 0x0080 /
  0x0083, already read for `getVersion()`).
- Status: input register 0x0100 is added to the polled ranges and exposed as `getVppMode()`, if the
  experiment confirms that it reflects the 0x00A0 modes.
- Unlock (0x0000 = password) only if the experiment shows the inverter is locked (0x0054). Today's 0x06
  writes work without it.

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

Every VPP command carries `ExecDuration`; when it runs out the inverter should return to the stored work
mode by itself. That is the failsafe: an app crash or a lost Modbus connection ends the mode within one
duration. Whether the firmware honors the duration in mode 8 is not certain (see the evidence above) and
is settled by the experiment.

- `manageForceDischarge` and the negative price check run every minute; while the decision holds they
  re-issue the command with a short duration (proposed 180 s), so a crash leaves at most three minutes
  of an unmanaged VPP mode. The writes are RAM-only, so re-issuing every minute is fine.
- Alternative: one command per block with the duration up to the block end. Fewer writes, but a crash
  leaves the mode running until then.
- At startup, and whenever the owner is `NONE`, a non-zero 0x0100 is cleared with VPP mode 0.
- If the duration turns out to be ignored, the mode is ended only by the explicit mode 0. A crash then
  leaves the mode running until the restart clears it, which is no worse than today's stored settings,
  but the failsafe motivation is gone and the startup cleanup above becomes mandatory.

### Metrics and status

Report the owner as `battery_owner` (string) and the commanded VPP mode as `vpp_mode` (int) in
`home-inverter-manager`; `force_discharge` and `inverter_off_negative_price` stay as derived bools for
the existing Grafana panels. Document in `doc/metrics.md`.

The stored work mode stays SelfUse while a VPP mode runs, so the `InverterHandler` status gets the owner
too and the inverter dialog shows it.

## Prerequisite: experiment on the real inverter

Run by the owner (HW-touching, not run automatically), as a one-off test like the 2026-11-08 one. The
order matters: the first write must not be mode 0. A single 0 written to 0x00A0 is a valid
`MeterFunction = 0` ★ in the FC 0x06 map, should the firmware not know the VPP block; the mode 8 block
starts with 8, which is out of range there.

1. Snapshot before any write: input 0x0054 (lock state) and 0x0100–0x0121 (legacy VPP status), holding
   0x008B / 0x008C (work mode, manual mode) and 0x0108–0x010D (`MeterFunction`, meter IDs, directions,
   `ExternalInv`: the read side of what FC 0x06 has at 0x00A0–0x00A7).
2. Mode 8, `PvWTarget` = PV peak power, `BatWTarget = −2000` (charge), 120 s, `NextMotion = 0xA0`.
   Accepted? Battery charging? 0x0100 = 8? Holding 0x0108–0x010D and 0x008B / 0x008C unchanged?
3. Send nothing for the 120 s: does the mode end by itself, 0x0100 back to 0 and the stored work mode
   active again? This is the failsafe.
4. The same command re-issued every 60 s, three times: does the duration restart, any dip in the battery
   power at the re-issue?
5. Mode 0 while a mode runs: immediate return to the stored mode?
6. Mode 8, `PvWTarget` = PV peak power, `BatWTarget = +2000` (discharge) at a SoC near
   `SelfUse_Discharge_MinSoC`: does it stop at min SoC? Does `ExportControl_User_Limit` (0x0042) cap the
   export? Is PV really not limited, and is a `PvWTarget` above the PV peak accepted?
7. Mode 8, `PvWTarget = 0`, `BatWTarget = −2000`, by day: PV off, battery charging from the grid? How long
   does PV take to go down and to come back afterwards? What happens at SoC 100 %?
8. Optional: mode 4 through 0x00A0 (`BatWTarget = +2000`, 120 s). Accepted? If so it is an alternative
   for force discharge.
9. For 005, not needed here: mode 5 or 7 with a 120 s duration, which settles the register offsets of
   modes 5–7 that SolaX's document contradicts (tables: duration 0xA1 / next motion 0xA2; examples:
   0xA3 / 0xA4); and a charge command from `Idle` (`VPPExitIdleEn`, read 0x00B4 / write 0x00F4 ★).

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
- **Deployments:** `PiPeConfigurator` has no export (`hardMaxExportPower = 0`) and no battery parameters
  for force discharge; the negative price behavior there must stay as it is or be configurable.

## Implementation steps

1. Experiment (above), results recorded in `doc/inverter/solax_vpp_protocol.md`.
2. `ModbusClient.writeMultipleRegisterValues`, the VPP block builder and the `SolaxInverterModbusClient`
   VPP methods with the firmware gate; unit tests of the block layout.
3. Owner enum in `InverterManager`; force discharge on mode 8; leftovers of the old control; startup
   cleanup of a running VPP mode; tests.
4. Negative price on mode 8 in a one-minute task, with the old turn-off kept behind an option until the
   first real negative day is checked.
5. Metrics, `InverterHandler` status and `doc/metrics.md`.
6. Inverter documentation section on VPP ownership (new `doc/inverter.md` or similar), referenced from
   `CLAUDE.md`.
7. Manual verification by the owner on the real inverter.
