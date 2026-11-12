# SolaX VPP (Remote Control) Protocol

Transcribed from the SolaX knowledge base article "SolaX_VPP function Definition of ESS"
(<https://kb.solaxpower.com/solution/detail/2c9fa4148ecd09eb018edf67a87b01d2>, page images uploaded
2026-02-03). The article is published only as images; this file keeps the register tables and the rules
as text. Where the article contradicts itself, it is noted under "Inconsistencies in the source".

The VPP interface lets an external controller drive the inverter for a limited time and then fall back
to the stored work mode. It is the RAM-side alternative to changing the stored work mode settings
(`SolarChargerUseMode` 0x001F ★, `SelfUse_Discharge_MinSoC` 0x0061 ★, see
`solax_modbus_spec_3.34.md`). The article itself does not mention EEPROM; that the VPP registers are not
saved there comes from the homeassistant-solax-modbus documentation.

## Two protocol versions

| Protocol | Start register | Modes | Write |
| --- | --- | --- | --- |
| New | 0x00A0 | 0–9, 11, 12 | FC 0x10, mode-specific length |
| Legacy | 0x007C | 0–7 | FC 0x10, 0x007C–0x0088 (modes 0–3) or 0x007C–0x008A (modes 4–7), all registers filled, unused ones 0 |

The new protocol needs at least this ARM firmware (DSP upgraded to the matching version):

| Model | ARM |
| --- | --- |
| X1-Hybrid-G4 | V1.44 |
| **X3-Hybrid-G4** | **V1.45** |
| X3-Hybrid-G4-Plus | V7 |
| X1-VAST | V3.11 |
| X1-IES / X1-IES-A | V9.12 |
| X3-IES / X3-IES-A | V13.01 |
| X3-IES-P | V13.04 |
| X3-ULTRA / X3-ULTRA-GLV | V20.06 |
| X3-AELIO | V6.00 |

The home inverter (X3-Hybrid-G4) runs ARM 1.54 / DSP 1.56 (SolaX Cloud "ARM IC" / "DSP IC", 2026-11)
and accepts the new protocol, see "Verified on the home inverter".

Caution: in the single-register write map (FC 0x06) 0x007C is `PowerFactor_Curve_PfLockInPoint` ★ and
0x00A0 is `MeterFunction` ★. Write the VPP blocks only with FC 0x10, never with
`writeSingleRegisterValue`.

## Operation procedure

1. **Unlock:** FC 0x06, register 0x0000 `UnlockPassword` (0–9999, default 2014, the same as the local
   password). The device stays unlocked until a wrong password is written or it reboots. Lock state:
   FC 0x04, register 0x0054 `InverterLockState` (0 locked, 1 unlocked).
2. **Manual mode first:** when the inverter work mode is Manual, switch it to another work mode before
   starting a VPP mode. From other work modes a VPP mode can be started directly.
3. **Exit:** write VPP mode 0.
4. Values are 16-bit words; negative values in two's complement. 32-bit values are LSB word first.

## Modes

| Mode | Name | Controls | Ends on | After auto-exit |
| --- | --- | --- | --- | --- |
| 0 | VPP Disable | – | – | basic work mode |
| 1 | Power Control | AC (grid) port active/reactive power | duration | basic work mode |
| 2 | Electric Quantity Target | AC port active power | AC port energy | basic work mode |
| 3 | SOC Target | AC port active power | battery SoC | basic work mode |
| 4 | Push Power – Positive/Negative | battery power | duration | basic work mode or VPP mode 6 |
| 5 | Push Power – Zero | battery idle, works like a string inverter | duration | basic work mode or VPP mode 6 |
| 6 | Self-Consume – Charge/Discharge | like Self-Use, battery charged from PV only | duration | basic work mode or VPP mode 6 |
| 7 | Self-Consume – Charge Only | like Self-Use, battery charged from PV only, **no discharge** | duration | basic work mode or VPP mode 6 |
| 8 | PV&BAT Individual – Duration | PV power limit and battery power | duration | basic work mode or VPP mode 6 |
| 9 | PV&BAT Individual – Target SOC | PV power limit and battery power | duration and target SoC | basic work mode or VPP mode 6 |
| 10 | reserved | | | |
| 11 | Max Input | PV off, battery charges at maximum | never (only mode 0 or another mode) | – |
| 12 | Max Output | PV maximum, battery discharges at maximum | never | – |

Modes 11 and 12 are only for "specific models".

"Grid port" is the inverter's AC port; the house load sits between it and the grid meter. Modes 1–3
control that port, modes 4, 5, 8, 9 control the battery port.

### Common fields

- `TargetSetType` 1 = reset: a new command restarts the accumulated target (energy etc.) from zero.
  2 = update: a new command changes the target and keeps the accumulated value.
- `ExecDuration` (s): how long the mode runs. If the mode has a `WaitTimeout`, the device then waits for
  a new command; otherwise it leaves the mode immediately.
- `WaitTimeout` (s): after the duration or the target (energy, SoC) is reached, the device waits this long
  for a new command, then leaves VPP. In modes 1–3 the AC port stops input/output while it waits.
- `NextMotion`: 0x00A0 = leave VPP and return to the basic work mode, 0x00A1 = switch to VPP mode 6.

### New protocol registers (FC 0x10 from 0x00A0)

Mode 0:

| Reg | Variable | Type | Meaning |
| --- | --- | --- | --- |
| 0xA0 | VPPModeNum | U16 | 0 |

Mode 1 – Power Control:

| Reg | Variable | Type | Meaning |
| --- | --- | --- | --- |
| 0xA0 | VPPModeNum | U16 | 1 |
| 0xA1 | TargetSetType | U16 | 1 reset, 2 update |
| 0xA2–0xA3 | GridWTarget | S32, W | AC port active power, + absorb (import), − output |
| 0xA4–0xA5 | GridVarTarget | S32, Var | + inductive, − capacitive |
| 0xA6 | ExecDuration | U16, s | |
| 0xA7 | WaitTimeout | U16, s | |

With +x the PV runs at maximum and the battery takes PV + x, limited by its maximum charge power (the
grid import is then reduced; no error is reported). With −x the AC port outputs x from PV first and the
battery covers the rest. Example: −5000 W, 300 s, timeout 600 s → `A0=0001 A1=0001 A2=EC78 A3=FFFF
A4=0000 A5=0000 A6=012C A7=0258`; after 300 s the port stops, after another 600 s the inverter returns
to its basic mode.

Mode 2 – Electric Quantity Target:

| Reg | Variable | Type | Meaning |
| --- | --- | --- | --- |
| 0xA0 | VPPModeNum | U16 | 2 |
| 0xA1 | TargetSetType | U16 | |
| 0xA2–0xA3 | GridWhTarget | U32, Wh | AC port energy |
| 0xA4–0xA5 | GridWTarget | S32, W | + absorb, − output |
| 0xA6 | WaitTimeout | U16, s | |

Mode 3 – SOC Target:

| Reg | Variable | Type | Meaning |
| --- | --- | --- | --- |
| 0xA0 | VPPModeNum | U16 | 3 |
| 0xA1 | TargetSetType | U16 | |
| 0xA2 | SocTarget | U16, % | |
| 0xA3–0xA4 | GridWTarget | S32, W | + absorb, − output |
| 0xA5 | WaitTimeout | U16, s | |

If the SoC is already past the target in the requested direction, the command counts as done
immediately (then `WaitTimeout`, then exit).

Mode 4 – Push Power Positive/Negative:

| Reg | Variable | Type | Meaning |
| --- | --- | --- | --- |
| 0xA0 | VPPModeNum | U16 | 4 |
| 0xA1–0xA2 | BatWTarget | S32, W | battery, **+ discharge, − charge** |
| 0xA3 | ExecDuration | U16, s | |
| 0xA4 | NextMotion | U16 | 0xA0 / 0xA1 |

Battery power has priority, PV runs as high as possible, the grid covers the rest.

Modes 5, 6, 7 – Push Power Zero, Self-Consume Charge/Discharge, Self-Consume Charge Only:

| Reg | Variable | Type | Meaning |
| --- | --- | --- | --- |
| 0xA0 | VPPModeNum | U16 | 5 / 6 / 7 |
| 0xA1 | ExecDuration | U16, s | verified for mode 7; SolaX's examples wrongly write it to 0xA3 |
| 0xA2 | NextMotion | U16 | SolaX's examples wrongly write it to 0xA4 |

Mode 7 example: PV 1000 W, load 3000 W → battery 0 W, inverter outputs 1000 W, 2000 W from the grid.

Mode 8 – PV&BAT Individual, Duration:

| Reg | Variable | Type | Meaning |
| --- | --- | --- | --- |
| 0xA0 | VPPModeNum | U16 | 8 |
| 0xA1 | TargetSetType | U16 | |
| 0xA2–0xA3 | PvWTarget | U32, W | PV power target (0 = PV off) |
| 0xA4–0xA5 | BatWTarget | S32, W | + discharge, − charge |
| 0xA6 | ExecDuration | U16, s | |
| 0xA7 | NextMotion | U16 | |

The battery target has priority, then PV approaches its target. Example: PV 0, battery −5000 W, 1800 s,
leave VPP → `A0=0008 A1=0001 A2=0000 A3=0000 A4=EC78 A5=FFFF A6=0708 A7=00A0`.

Mode 9 – PV&BAT Individual, Target SOC:

| Reg | Variable | Type | Meaning |
| --- | --- | --- | --- |
| 0xA0 | VPPModeNum | U16 | 9 |
| 0xA1 | TargetSetType | U16 | |
| 0xA2–0xA3 | PvWTarget | U32, W | |
| 0xA4–0xA5 | BatWTarget | S32, W | + discharge, − charge |
| 0xA6 | SocTarget | U16, % | |
| 0xA7 | ExecDuration | U16, s | |
| 0xA8 | NextMotion | U16 | |

When the target SoC is reached, **both PV and battery power stay at 0** until the mode ends.

Modes 11 and 12 write only 0xA0 (0x000B / 0x000C).

### Legacy protocol registers (FC 0x10 from 0x007C)

| Reg | Variable | Modes | Meaning |
| --- | --- | --- | --- |
| 0x7C | VPPModeNum | all | 0–7 |
| 0x7D | TargetSetType | 1–3 | |
| 0x7E–0x7F | GridWTarget | 1 | S32 W, + absorb |
| 0x80–0x81 | GridVarTarget | 1 | S32 Var |
| 0x82 | ExecDuration | 1 | s |
| 0x83 | SocTarget | 3 | % |
| 0x84–0x85 | GridWhTarget | 2 | U32 Wh |
| 0x86–0x87 | GridWTarget | 2, 3 | S32 W, + absorb |
| 0x88 | WaitTimeout | 1–3 | s |
| 0x89–0x8A | BatWTarget | 4 | S32 W, + discharge, − charge |

In the legacy protocol modes 4–7 have no duration and no next motion: they run **10 minutes and then
switch to VPP mode 6**. This matches homeassistant-solax-modbus issue #2418 (Gen4: mode 4/7 falls back to
mode 6 after 10 minutes), whose reporter still used 0x007C.

`solax_modbus_spec_3.34.md` lists the legacy block under "Remote Power Control (V3.33)" and the legacy
status input registers at 0x0100–0x0121 (`ModbusPowerControl`, `TargetFinishFlag`, …). The article
defines no status registers for the new protocol; on the home inverter 0x0100 shows the new protocol's
mode as well.

## Inconsistencies in the source

- Modes 5, 6, 7: the register tables put `ExecDuration` at 0xA1 and `NextMotion` at 0xA2, but the
  examples write them to 0xA3 and 0xA4 (the mode 4 offsets). The tables are right: verified with mode 7
  on the home inverter.
- The mode 9 example writes `VPPModeNum = 0x0008` although it describes mode 9.
- The mode 12 example writes `0x000B` (mode 11).
- The mode 7 example's "exit mode" line names mode 6.

## Verified on the home inverter

X3-Hybrid-G4, ARM 1.54 / DSP 1.56, stored work mode SelfUse, run mode `Normal`, no PV (night of
2026-11-11), battery 39–45 %, house load 0.5–1.3 kW. Measured with `SolaxVppExperimentTest`, which
samples the inverter every 3–6 s, so the times below are that coarse. The export limit was measured on
2026-11-12 at 66–68 % with a house load of about 0.5 kW.

| Command | Block from 0x00A0 | Observed |
| --- | --- | --- |
| Mode 8, PV 10000 W, battery −2000 W, 120 s | `0008 0001 2710 0000 F830 FFFF 0078 00A0` | accepted; battery charging 1950–1990 W within 3 s, the grid covers it and the house load |
| Mode 8, PV 30000 W (above the PV peak), battery −2000 W, 60 s | `0008 0001 7530 0000 F830 FFFF 003C 00A0` | accepted, same behavior |
| Mode 8, PV 10000 W, battery +2000 W, 900 s | `0008 0001 2710 0000 07D0 0000 0384 00A0` | accepted; battery discharging 2070–2100 W, the surplus above the house load exported |
| Mode 4, battery +2000 W, 120 s | `0004 07D0 0000 0078 00A0` | accepted; battery discharging 2035–2065 W, the surplus above the house load exported |
| Mode 7, 60 s | `0007 003C 00A0` | accepted; battery 0 W, the house runs from the grid |
| Mode 0 | `0000` | ends a running mode: the battery leaves its target within about 5 s. With no mode running it is accepted and changes nothing. |

Findings:

- **The duration is honored.** Modes 4, 7 and 8 ended by themselves 5–15 s after the commanded duration
  and the inverter continued in the stored work mode (SelfUse discharging to the house) within about
  10 s, without a `Checking` phase. `NextMotion` was 0x00A0 in all commands.
- **A re-issued command restarts the duration.** The same mode 8 command sent every 60 s kept the mode
  running; it ended about 125 s after the last one. The battery power did not dip at a re-issue.
- **A VPP discharge ignores `SelfUse_Discharge_MinSoC`.** With the min SoC stored as 42 %, mode 8 kept
  discharging at full power from 43 % down to 39 %, where the test ended it with mode 0. SelfUse then
  stopped the battery at once and the inverter went `Idle`. The controller has to watch the lower SoC
  limit itself.
- **`ExportControl_User_Limit` caps a VPP discharge.** Mode 8 with the battery at +4000 W and the limit
  at 8500 W exported 3470 W. With the limit lowered to 1000 W while the mode ran, the feed-in was at the
  limit 3–5 s after the write and stayed at 780–1050 W (mean 1000 W over a minute); the inverter did it
  by lowering the battery power to about 1530 W. The mode itself kept running (0x0100 = 8). Measured
  without PV and with a limit above 0 only.
- **Stored settings are untouched.** Holding 0x008B / 0x008C (work mode, manual mode) and 0x0108–0x010D
  (`MeterFunction`, meter IDs, directions, `ExternalInv`, which FC 0x06 writes at 0x00A0–0x00A7) did not
  change. Whether the VPP registers themselves avoid EEPROM cannot be checked on the device.
- **No unlock was needed:** input 0x0054 `LockState` read 1.
- **Run mode 20.** While a VPP mode runs, input 0x0009 `RunMode` reads 20, a value missing in SolaX's
  run mode table (`InverterState.Mode.RemoteControl`). It returns to 2 (`Normal`) when the mode ends.
- **Status registers.** Input 0x0100 shows the running mode of the new protocol (8, 4, 7 seen) and 0
  afterwards. It and the run mode follow the battery with a delay of up to about 13 s, at the start and
  at the end. 0x011E `RemoteCtrlTimeOut` holds the last commanded duration and does not count down.
  0x0101 `TargetFinishFlag`, 0x0114 `Charge_Discharg_Power` and 0x011A `Time_of_Duration` stayed 0.
- **Modes 5–7 take three registers,** as in SolaX's tables: mode 7 with 60 at 0xA1 ended after about
  75 s. An earlier run with 120 s proved nothing, because the mode 4 command before it had left 120 at
  0xA3.
- `VPPExitIdleEn` (holding 0x00B4) reads 0 (disabled).

Not tested yet: mode 8 with PV running (`PvWTarget = 0`, PV not limited by a high target, the time PV
takes to go down and to come back), whether the export limit also holds with PV running and at 0 W,
charging at SoC 100 %, and a command sent while the inverter is `Idle`.
