# SolaX X1/X3 G4 Modbus TCP/RTU Protocol Specification V3.34

This MD file was generated from `Solax_Hybrid_X1.X3-G4_ModbusTCP.RTU_V3.34_-_English_240313.pdf`.

## Changes vs V3.24

- V3.25 — Added Read 0x0160 `CTFalutEn`, 0x0161 `u16SuperBuckUpEn`; Write 0x00FD `CTFalutEn`, 0x00FE `SuperBuckUpEn`
- V3.26 — Added Read 0x0162–0x016C generator schedule registers; Write 0x00FF `SmartScheduleWorkMode`, 0x0100–0x010B Gen periods
- V3.27 — X3 protection range adjustments (0x05/0x06/0x0C/0x0D, QuResponseV1–4, ResponseV1–4, Adjust_AC_Volt_R/S/T, CalibGainInvVoltR/S/T, CalibEPSDcvAdjR/S/T); modify 0x0047 `Language`
- V3.28 — Added Write 0x00DF `ResetINV`, 0x010C `FastInEPS`; Read 0x016D `FastInEPSEn`
- V3.29 — Modified `SolarChargerUseMode` (added Peak Shaving + TOU); modified `485CommFunSelect`; added 0x016E–0x0173 TOU mode registers; 0x0174 `bShotoffEn(X3)`; 0x0175 `PowerFactor_Qu_VoltRatio2`; 0x0176 `PowerFactor_Qu_VoltRatio3`
- V3.30 — Language list extended; Gen WorkPeriod1&2 description revisions
- V3.31 — Added Read 0x0177 `CTCutDownINVEn`; Write 0x010D `CTCutDownINVEn`
- V3.32 — Added EvCharger chapters (0x03/0x04/0x10); `UpgradeModule` 10 = EVCharger
- V3.33 — Modified `0x10 ModbusPowerControl` (write multiple) description
- V3.34 — EvCharger details delegated to `(Solax)EVC ModbusRTU V3.3` document

Significant register-value extensions vs V3.24:
- `SolarChargerUseMode` now `0..5` (added 4:PeakShaving, 5:TOU)
- `Language` now `0..9` (added 6:Italian, 7:Chinese(BAN), 8:Ukrainian, 9:Brazil)
- `485CommFunSelect` now `0..6` (added 3:AdaptBoxG2, 4:EVC&AdaptBoxG2, 5:AdaptBoxG2&Meter, 6:EVC&AdaptBoxG2&Meter)
- `ExternalGenEn` now `0..2` (added 2:Dry Contact)
- `ModbusPowerControl` now `0..7` (added VPP push & self-consume modes)
- Additional safety codes (X3 40–48, X1 37–41)
- Inverter error code bit 21 (X1+X3) now `Parallel Fault`; X3 bit 22 now `Hard Limit Fault`
- Lead-acid voltage write ranges X3 expanded to `1600–8000` (0.1 V)

## Connection Parameters

### Modbus RTU (RS485)
- Address: `1` (default)
- Baud Rate: `19200` (default)
- Data bits: `8`, Stop bit: `1`, Parity: `None`

### Modbus TCP (via Monitoring Module)
- Port: `502`
- Unit ID: `0x01` (default)
- Transaction ID / Protocol ID: no strict requirements

> **Note:** The inverter itself does not support Modbus TCP. It requires a SolaX monitoring module. Query cycle should be ~1 second.

### Timing
| Parameter | Value |
|---|---|
| Min interval between instructions | 1 sec |
| Silent time between packets | >100 ms |
| Response timeout | 1 sec |

### EEPROM Warning
Registers marked with ★ in the "EE Save" column are written to EEPROM on change. EEPROM has limited write cycles — excessive writes cause irreversible hardware damage.

### Endianness
- 32-bit data uses **little-endian** format (LSB register first, MSB register second)
- 16-bit registers with Hi/Lo bytes: Hi = upper 8 bits, Lo = lower 8 bits

---

## Function Code 0x03: Read Holding Registers

### Request Format
| Field | Size | Description |
|---|---|---|
| Slave ID | 1 byte | Default `0x01` |
| Function code | 1 byte | `0x03` |
| Start address | 2 bytes | MSB first |
| Register count | 2 bytes | N |
| CRC | 2 bytes | |

### Response Format
| Field | Size | Description |
|---|---|---|
| Slave ID | 1 byte | |
| Function code | 1 byte | `0x03` |
| Byte count | 1 byte | `2*N` |
| Register data | N×2 bytes | MSB first per register |
| CRC | 2 bytes | |

### Error Response
Function code `0x83`, then 1-byte abnormal code (`0x01`–`0x04`).

**Example:** Read InverterSN (0x0000–0x0006)
- Request: `01 03 00 00 00 07 04 08`
- Response: `01 03 0E 48 34 37 35 32 32 5A 48 45 4E 47 57 45 4E 63 26`

---

### Holding Registers (0x03)

#### Device Info

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x0000–0x0006 | InverterSN | 14char | uint16 | 7 | Serial number, MSB=SN[14] |
| 0x0007–0x000D | FactoryName | 14char | uint16 | 7 | |
| 0x000E–0x0014 | ModuleName | 14char | uint16 | 7 | |
| 0x0016 | TimeStart | 1s | uint16 | 1 | Launch wait time |
| 0x0017 | ReconnectionTime | 1s | uint16 | 1 | |
| 0x0018 | CheckingTime | 1s | uint16 | 1 | |

#### Grid Protection

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x0019 | VacMinProtect | 0.1V | uint16 | 1 | Min grid voltage |
| 0x001A | VacMaxProtect | 0.1V | uint16 | 1 | Max grid voltage |
| 0x001B | FacMinProtect | 0.01Hz | uint16 | 1 | Min grid frequency |
| 0x001C | FacMaxProtect | 0.01Hz | uint16 | 1 | Max grid frequency |
| 0x001D | SafetyCode | — | uint16 | 1 | See [Safety Codes](#safety-codes) |
| 0x001E | MateBoxEnable | 1 | uint16 | 1 | 0:Disable 1:Enable |
| 0x001F | Grid10MinAvgProtect | 0.1V | uint16 | 1 | 10min over voltage protect |
| 0x0020 | VacMinSlowProtect | 0.1V | uint16 | 1 | |
| 0x0021 | VacMaxSlowProtect | 0.1V | uint16 | 1 | |
| 0x0022 | FacMinSlowProtect | 0.01Hz | uint16 | 1 | |
| 0x0023 | FacMaxSlowProtect | 0.01Hz | uint16 | 1 | |
| 0x0025 | PowerLimitsPercent | 0–100 | uint16 | 1 | Output power limit % |

#### Power Factor

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x0026 | PowerfactorMode | 1 | uint16 | 1 | 0:Off 1:OverExcited 2:UnderExcited 3:Curve 4:Qu 5:FixQPower |
| 0x0027 | PowerfactorData | 0.01 | uint16 | 1 | |
| 0x0028 | PowerFactor_Curve_PF1 | 0.01 | uint16 | 1 | |
| 0x0029 | PowerFactor_Curve_PF2 | 0.01 | uint16 | 1 | |
| 0x002A | PowerFactor_Curve_PF3 | 0.01 | uint16 | 1 | |
| 0x002B | PowerFactor_Curve_PF4 | 0.01 | uint16 | 1 | |
| 0x002C | PowerFactor_Curve_Power1 | 1% | uint16 | 1 | |
| 0x002D | PowerFactor_Curve_Power2 | 1% | uint16 | 1 | |
| 0x002E | PowerFactor_Curve_Power3 | 1% | uint16 | 1 | |
| 0x002F | PowerFactor_Curve_Power4 | 1% | uint16 | 1 | |
| 0x0030 | PowerFactor_Curve_PfLockInPoint | 0.01 | uint16 | 1 | |
| 0x0031 | PowerFactor_Curve_PfLockOutPoint | 0.01 | uint16 | 1 | |
| 0x0032 | PowerFactor_Curve_3Tau | 1s | uint16 | 1 | |
| 0x0033 | PowerFactor_Qu_VoltRatio1 | 1% | uint16 | 1 | |
| 0x0034 | PowerFactor_Qu_VoltRatio4 | 1% | uint16 | 1 | |
| 0x0035 | PowerFactor_Qu_QuResponseV1 | 0.1V | uint16 | 1 | |
| 0x0036 | PowerFactor_Qu_QuResponseV2 | 0.1V | uint16 | 1 | |
| 0x0037 | PowerFactor_Qu_QuResponseV3 | 0.1V | uint16 | 1 | |
| 0x0038 | PowerFactor_Qu_QuResponseV4 | 0.1V | uint16 | 1 | |
| 0x0039 | PowerFactor_Qu_K | 0.1 | int16 | 1 | |
| 0x003A | PowerFactor_Qu_3Tau | 1s | uint16 | 1 | |
| 0x003B | PowerFactor_Qu_QuDelayTimer | 1s | uint16 | 1 | |
| 0x003C | PowerFactor_Qu_QuLockEn | 1 | uint16 | 1 | 0:Disable 1:Enable |
| 0x003D | PowerFactor_Qu_QuLockIn | 1% | uint16 | 1 | |
| 0x003E | PowerFactor_Qu_QuLockOut | 1% | uint16 | 1 | |
| 0x003F | PowerFactor_FixQPower | 1Var | int16 | 1 | |
| 0x0040 | PowerFactor_FixQPower_Max | 1Var | int16 | 1 | |
| 0x0041 | PowerFactor_FixQPower_Min | 1Var | int16 | 1 | |

#### Connection / Reconnection

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x0042 | wConnection_FL | 0.01Hz | int16 | 1 | Connection Low freq |
| 0x0043 | wConnection_FH | 0.01Hz | int16 | 1 | Connection High freq |
| 0x0044 | wConnection_VL | 0.1V | int16 | 1 | Connection Low voltage |
| 0x0045 | wConnection_VH | 0.1V | int16 | 1 | Connection High voltage |
| 0x0046 | wConnection_ObserveT | 1s | int16 | 1 | Observation time |
| 0x0047 | wConnection_GradientEn | 1 | int16 | 1 | Gradient Select |
| 0x0048 | wReconnection_FL | 0.01Hz | int16 | 1 | |
| 0x0049 | wReconnection_FH | 0.01Hz | int16 | 1 | |
| 0x004A | wReconnection_VL | 0.1V | int16 | 1 | |
| 0x004B | wReconnection_VH | 0.1V | int16 | 1 | |
| 0x004C | wReconnection_ObserveT | 1s | int16 | 1 | |
| 0x004D | wReconnection_GradientEn | 1 | int16 | 1 | |
| 0x004E | wReconnection_Gradient | 1% | int16 | 1 | |

#### Firmware

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x007D | FirmwareVersion_DSP_Minor | 1 | uint16 | 1 | |
| 0x007E | HardwareVersion_DSP | 1 | uint16 | 1 | |
| 0x007F | FirmwareVersion_DSP_Major | 1 | uint16 | 1 | |
| 0x0080 | FirmwareVersion_ARM_Major | 1 | uint16 | 1 | |
| 0x0082 | FirmwareVersion_ModbusRTU | 1 | uint16 | 1 | Matches ARM version |
| 0x0083 | FirmwareVersion_ARM_Minor | 1 | uint16 | 1 | |
| 0x0084 | FirmwareVersion_ARM_Bootloader | 1 | uint16 | 1 | |

#### RTC

| Register | Variable | Format | Len |
|---|---|---|---|
| 0x0085 | RTC-Seconds | uint16 | 1 |
| 0x0086 | RTC-Minutes | uint16 | 1 |
| 0x0087 | RTC-Hours | uint16 | 1 |
| 0x0088 | RTC-Days | uint16 | 1 |
| 0x0089 | RTC-Months | uint16 | 1 |
| 0x008A | RTC-Years | uint16 | 1 |

#### Battery & Charger Settings

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x008B | SolarChargerUseMode | 1 | uint16 | 1 | 0:SelfUse 1:FeedinPriority 2:BackUp 3:Manual 4:PeakShaving 5:TOU |
| 0x008C | ManualMode | 1 | uint16 | 1 | 0:Stop 1:ForceCharge 2:ForceDischarge |
| 0x008D | wBattery1_Type | 1 | uint16 | 1 | 0:LeadAcid 1:Lithium |
| 0x008E | Charge_floatVolt | 0.1V | uint16 | 1 | Lead-acid charge float voltage |
| 0x008F | Battery_DischargeCutVoltage | 0.1V | uint16 | 1 | Lead-acid discharge cutoff |
| 0x0090 | Battery_ChargeMaxCurrent | 0.1A | uint16 | 1 | |
| 0x0091 | Battery_DischargeMaxCurrent | 0.1A | uint16 | 1 | |
| 0x0092 | absorpt_voltage | 0.1V | uint16 | 1 | Lead-acid absorpt voltage |

#### SoC & Night Charge Settings

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x0093 Hi | SelfUse_Discharge_MinSoC | 1% | uint8 | — | 10–100% |
| 0x0093 Lo | SelfUse_NightCharge_Enable | 1 | uint8 | — | 0:Disable 1:Enable |
| 0x0094 | SelfUse_NightCharge_UpperSoC | 1% | uint16 | 1 | 10–100% (active when 0x0093 Lo = 1) |
| 0x0095 Hi | Feedin_NightCharge_UpperSoC | 1% | uint8 | — | 10–100% |
| 0x0095 Lo | Feedin_Discharge_MinSoC | 1% | uint8 | — | 10–100% |
| 0x0096 Hi | BackUp_NightCharge_UpperSoC | 1% | uint8 | — | 30–100% |
| 0x0096 Lo | BackUp_Discharge_MinSoC | 1% | uint8 | — | 30–100% |

#### Charge / Discharge Periods

Period registers: Hi byte = minute (0–59), Lo byte = hour (0–23).

| Register | Variable |
|---|---|
| 0x0097 | ChargePeriod1_Start (Hi:min Lo:hour) |
| 0x0098 | ChargePeriod1_End |
| 0x0099 | DischargePeriod1_Start |
| 0x009A | DischargePeriod1_End |
| 0x009B | Set_Chrg&DischrgPeriod2_Enable (0/1) |
| 0x009C | ChargePeriod2_Start |
| 0x009D | ChargePeriod2_End |
| 0x009E | DischargePeriod2_Start |
| 0x009F | DischargePeriod2_End |

#### Off-grid & System Settings

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x00A0 | EpsRestartSoc | 1% | uint16 | 1 | 10–100 |
| 0x00A1 | HotStandbyEN | 1 | uint16 | 1 | 0:enable 1:disable |
| 0x00A2 | ExtendBmsSetting | 1 | uint16 | 1 | 0:disable 1:enable |
| 0x00A3 | BatteryHeatingEn | — | uint16 | 1 | 0:disable 1:enable |
| 0x00A4 | HeatingPeriod1_Start | — | uint16 | 1 | Hi:Minute Lo:Hour |
| 0x00A5 | HeatingPeriod1_End | — | uint16 | 1 | Hi:Minute Lo:Hour |
| 0x00A6 | HeatingPeriod2_Start | — | uint16 | 1 | Hi:Minute Lo:Hour |
| 0x00A7 | HeatingPeriod2_End | — | uint16 | 1 | Hi:Minute Lo:Hour |
| 0x00A8 | wBatteryDischargeBackupVoltage | 0.1V | uint16 | 1 | |
| 0x00A9 | MatchResistanceSet (X3) | — | uint16 | 1 | 0:disable 1:enable |
| 0x00AA–0x00AE | RegistrationCode | 10char | uint16 | 5 | For external module |
| 0x00AF | ModBusRTU_Address | 1 | uint16 | 1 | |
| 0x00B0 | ModBusRTU_BaudRate | bit/s | uint16 | 1 | 0:115200 1:57600 2:56000 3:38400 4:19200 5:14400 6:9600 |
| 0x00B1 | InvVoltZeroResult (X3) | 1 | uint16 | 1 | 1:calibration done; other:fail |
| 0x00B2 | PgridBias | — | uint16 | 1 | 0:Disable 1:Grid 2:INV |
| 0x00B3 | FastCtCheckEn | 1 | uint16 | 1 | 0:disable 1:enable |
| 0x00B4 | VPPExitIdleEn | 1 | uint16 | 1 | 0:Disable 1:Enable |
| 0x00B5 | Factorylimit | 1W | uint16 | 1 | |
| 0x00B6 | ExportControlUserLimit | 1W(X1)/10W(X3) | uint16 | 1 | |
| 0x00B7 | Off-grid_Mute | 1 | uint16 | 1 | 0:off 1:on |
| 0x00B8 | Off-grid_MinSoC | 1% | uint16 | 1 | |
| 0x00B9 | Off-grid_Frequency | 1 | uint16 | 1 | |
| 0x00BA | InverterPowerType | 1W | uint16 | 1 | X1G4: 3000/3680/5000/6000/7500; X3G4: 15K/12K/10K/8K/6K/5K |
| 0x00BB | Language | — | uint16 | 1 | 0:EN 1:DE 2:FR 3:PL 4:ES 5:PT 6:IT 7:CN(BAN) 8:UA 9:BR |
| 0x00BC | EnableMPPT | 0/1 | uint16 | 1 | |

#### Self-Test Parameters (0x00BD–0x00D6)

Trip-time units: `1ms (X1)` / `10ms (X3)`.

| Register | Variable | Unit | Format | Len |
|---|---|---|---|---|
| 0x00BD | wTuvp_L2 | 1ms(X1)/10ms(X3) | uint16 | 1 |
| 0x00BE | wTovp_L2 | 1ms(X1)/10ms(X3) | uint16 | 1 |
| 0x00BF | wTufp_L2 | 1ms(X1)/10ms(X3) | uint16 | 1 |
| 0x00C0 | wTofp_L2 | 1ms(X1)/10ms(X3) | uint16 | 1 |
| 0x00C1 | wTuvp_L1 | 1ms(X1)/10ms(X3) | uint16 | 1 |
| 0x00C2 | wTovp_L1 | 1ms(X1)/10ms(X3) | uint16 | 1 |
| 0x00C3 | wTufp_L1 | 1ms(X1)/10ms(X3) | uint16 | 1 |
| 0x00C4 | wTofp_L1 | 1ms(X1)/10ms(X3) | uint16 | 1 |
| 0x00C5 | TestStep | 1–9 | uint16 | 1 |
| 0x00C6 | OvpValue (Ovp 59.S2) | 0.1V | uint16 | 1 |
| 0x00C7 | OvpTime (Ovp 59.S2) | 1ms | uint16 | 1 |
| 0x00C8 | UvpValue (Uvp 27.S1) | 0.1V | uint16 | 1 |
| 0x00C9 | UvpTime (Uvp 27.S1) | 1ms | uint16 | 1 |
| 0x00CA | OfpValue (Ofp 81>.S1) | 0.01Hz | uint16 | 1 |
| 0x00CB | OfpTime (Ofp 81>.S1) | 1ms | uint16 | 1 |
| 0x00CC | UfpValue (Ufp 81<.S1) | 0.01Hz | uint16 | 1 |
| 0x00CD | UfpTime (Ufp 81<.S1) | 1ms | uint16 | 1 |
| 0x00CE | SelfTestOvp10mAvgVal (Ovp_10 59.S1) | 0.1V | uint16 | 1 |
| 0x00CF | SelfTestOvp10mAvgTime (Ovp_10 59.S1) | 1s | uint16 | 1 |
| 0x00D0 | SelfTestOfpVal_Restrictive (Ofp2 81>.S2) | 0.01Hz | uint16 | 1 |
| 0x00D1 | SelfTestOfpTime_Restrictive (Ofp2 81>.S2) | 1ms | uint16 | 1 |
| 0x00D2 | SelfTestUfpVal_Restrictive (Ufp2 81<.S2) | 0.01Hz | uint16 | 1 |
| 0x00D3 | SelfTestUfpTime_Restrictive (Ufp2 81<.S2) | 1ms | uint16 | 1 |
| 0x00D4 | SelfTest_UvpRestrictive_Val (Uvp 27.S2) | 0.1V | uint16 | 1 |
| 0x00D5 | SelfTest_UvpRestrictive_Time (Uvp 27.S2) | 1ms | uint16 | 1 |
| 0x00D6 | SelfTest_Time | 1s | uint16 | 1 |

`TestStep` values: 1=Ovp(59.S2) 2=Uvp(27.S1) 3=Uvp(27.S2) 4=Ofp(81>.S1) 5=Ufp(81<.S1) 6=Ofp2(81>.S2) 7=Ufp2(81<.S2) 8=Ovp_10(59.S1) 9=success.

#### Export / Power Limits & Frequency Curves

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x00D7 | MainBreakerCurrentLimit | 1A | uint16 | 1 | 32A–100A |
| 0x00D8 | PfLockInPoint | — | uint16 | 1 | 105–110 |
| 0x00D9 | PfLockOutPoint | — | uint16 | 1 | 90–98 |
| 0x00DA | wInverter_OutPut_Switch | 0/1 | uint16 | 1 | 1=ON 0=Off |
| 0x00DB | OFPL_Point | 0.01Hz | uint16 | 1 | Overfreq load reduction point |
| 0x00DC | OFPL_SetRate | 1% | uint16 | 1 | |
| 0x00DD | OFPL_DelayTime | 1ms | uint16 | 1 | |
| 0x00DE | OFPL_fstop_disch | 0.01Hz | uint16 | 1 | |
| 0x00DF | OFPL_fPmin | 0.01Hz | uint16 | 1 | |
| 0x00E0 | UserPassword | 1 | uint16 | 1 | |
| 0x00E1 | AdvancePassword | 1 | uint16 | 1 | |
| 0x00E2 | UFPL_Point | 0.01Hz | uint16 | 1 | Underfreq load increase point |
| 0x00E3 | UFPL_SetRate | 1% | uint16 | 1 | |
| 0x00E4 | UFPL_DelayTime | 1ms | uint16 | 1 | |
| 0x00E5 | OFPL_CurveType | 0/1 | uint16 | 1 | 0:Symmetry 1:Asymmetry |
| 0x00E6 | OFPL_Tstop | 1s | uint16 | 1 | Asymmetry stop time |
| 0x00E7 | OFPL_RemovePoint | 0.01Hz | uint16 | 1 | |
| 0x00E8 | UFPL_RemovePoint | 0.01Hz | uint16 | 1 | |
| 0x00E9 | ExportSoftLimitEn | — | uint16 | 1 | |
| 0x00EA | ExportHardLimitEn | — | uint16 | 1 | |
| 0x00EB | GeneralSoftLimitEn | — | uint16 | 1 | |
| 0x00EC | GeneralHardLimitEn | — | uint16 | 1 | |
| 0x00ED | wAcPowerLimit | 1VA(X1)/10VA(X3) | uint16 | 1 | |
| 0x00EE | ConnectSlop (X3) | 1% | uint16 | 1 | |
| 0x00EF | ReconnectSlop (X3) | 1% | uint16 | 1 | |
| 0x00F0 | HardExportPower | 1W(X1)/10W(X3) | uint16 | 1 | |
| 0x00F1 | HardAcPowerLimit | 1VA(X1)/10VA(X3) | uint16 | 1 | |
| 0x00F2 | SetpointTimeout | 1ms | uint16 | 1 | |
| 0x00F3 | wPowerLimitGra | 0.0001 | uint16 | 1 | |

#### PU Function

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x00F4 | PuFunc_VoltResponse_V2 | 0.1V | uint16 | 1 | |
| 0x00F5 | PuFunc_VoltResponse_V3 | 0.1V | uint16 | 1 | |
| 0x00F6 | PuFunc_VoltResponse_V4 | 0.1V | uint16 | 1 | |
| 0x00F7 | PuFunc_VoltResponse_V1 | 0.1V | uint16 | 1 | |
| 0x00F8 | PuFunc_3Tau | 0.01 | uint16 | 1 | |
| 0x00F9 | PUFuncEnable | 1 | uint16 | 1 | 0:disable 1:enable |
| 0x00FA | SetPuPower1 | 1% | uint16 | 1 | |
| 0x00FB | SetPuPower2 | 1% | uint16 | 1 | |
| 0x00FC | SetPuPower3 | 1% | uint16 | 1 | |
| 0x00FD | SetPuPower4 | 1% | uint16 | 1 | |
| 0x00FF | Pu_Type | 1 | uint16 | 1 | |

#### System Config

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x0100 | UFPL_fstop_ch | 0.01Hz | uint16 | 1 | |
| 0x0101 | UFPL_fPmax | 0.01Hz | uint16 | 1 | |
| 0x0102 | DRMFunctionEnable | 1 | uint16 | 1 | 0:disable 1:enable |
| 0x0103 | CtType (X3) | 1 | uint16 | 1 | 0:100A 1:200A |
| 0x0104 | wShadowFixFuncEnable | 1 | uint16 | 1 | 0:Off 1:Low 2:Middle 3:High |
| 0x0105 | MachineType_X1orX3 | — | uint16 | 1 | 1:X1 3:X3 |
| 0x0106 | PhasePowerBalance (X3) | 1 | uint16 | 1 | 0:disable 1:enable |
| 0x0107 | wMachineStyle | 1 | uint16 | 1 | 0:X-Hybrid 1:X-FIT |
| 0x0108 | MeterFunction | 1 | uint16 | 1 | 0:disable 1:enable |
| 0x0109 | Meter1ID | 1 | uint16 | 1 | 1–200 |
| 0x010A | Meter2ID | 1 | uint16 | 1 | 1–200 |
| 0x010B | DirectionMeterCT1 | 1 | uint16 | 1 | 0:Positive 1:Negative |
| 0x010C | DirectionMeter2 | 1 | uint16 | 1 | 0:Positive 1:Negative |
| 0x010D | ExternalInv | 1 | uint16 | 1 | 0:Enable 1:Disable |
| 0x010E | BatteryChargeMaxSoc | 1% | uint16 | 1 | Charger upper limit |
| 0x010F | bBatterToEVCharge | 1 | uint16 | 1 | 0:Enable 1:Disable |
| 0x0110 | InPutDI1 | 1 | uint16 | 1 | 0:Low 1:High |
| 0x0111 | DischCutOffPoint_DifferentEN | 1 | uint16 | 1 | Lead-acid: 0:disable 1:enable |
| 0x0113 | DischCutOffVoltage_GridMode | 0.1V | uint16 | 1 | Lead-acid on-grid discharge cutoff |
| 0x0114 | ShadowFixFuncEnable2 | 1 | uint16 | 1 | 0:Off 1:Low 2:Middle 3:High |
| 0x0115 | Meter/CT_Select | 1 | uint16 | 1 | 0:Meter 1:CT |
| 0x0116 | FVRT_Function | 1 | uint16 | 1 | 0:Disable 1:Enable |
| 0x0117 | FVRT_VacUpper | 0.1V | uint16 | 1 | (active when FVRT_Function=1) |
| 0x0118 | FVRT_VacLower | 0.1V | uint16 | 1 | (active when FVRT_Function=1) |
| 0x011B | bPVConnectionMode (X1) | 1 | uint16 | 1 | |
| 0x011C | ShutDown (X1) | 1 | uint16 | 1 | 0:Disable 1:Enable |
| 0x011D | MicroGrid (X1) | 1 | uint16 | 1 | 0:Disable 1:Enable |
| 0x011E | SelfuseModeBackupEn | 1 | uint16 | 1 | 0:Disable 1:Enable |
| 0x011F | bSelfUse_BackupSoc | 1% | uint16 | 1 | 10–100 |
| 0x0120 | bLeaseModeEnable | 1 | uint16 | 1 | 0:Disable 1:Enable |
| 0x0121 | bDeviceLockFlag | 1 | uint16 | 1 | 0:Disable 1:Enable |

#### Dry Contact

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x0122 | ManualModeControl | 1 | uint16 | 1 | 0:OFF 1:ON |
| 0x0123 | FeedinOnPower | 1W | uint16 | 1 | Grid connected pull-in power point |
| 0x0124 | bSwitchOnSoc | 1% | uint16 | 1 | SOC trigger for pull-in |
| 0x0125 | ConsumeOffPower | 1W | uint16 | 1 | Power consumption off trigger |
| 0x0126 | bSwitchOffSoc | 1% | uint16 | 1 | SOC trigger for breaking |
| 0x0127 | MinimumPerOnSignal | 1min | uint16 | 1 | Min duration single pull-in |
| 0x0128 | MaximumPerDayOn | — | uint16 | 1 | Max cumulative pickup time/day |
| 0x0129 | bScheduleEnable | 1 | uint16 | 1 | 0:Disable 1:Enable |
| 0x012A | bP1_Start | — | uint16 | 1 | Hi:Minute Lo:Hour |
| 0x012B | bP1_Stop | — | uint16 | 1 | Hi:Minute Lo:Hour |
| 0x012C | bP2_Start | — | uint16 | 1 | Hi:Minute Lo:Hour |
| 0x012D | bP2_Stop | — | uint16 | 1 | Hi:Minute Lo:Hour |
| 0x012E | WorkMode | 1 | uint16 | 1 | 0:Disable 1:Manual 2:SmartSave |
| 0x012F | DryContactMode | 1 | uint16 | 1 | 0:LoadManagement 1:GeneratorControl |
| 0x0130 | ParallelSetting | 1 | uint16 | 1 | 0:Free 1:Master 2:Slave |
| 0x0131 | ExternalGenEn | 1 | uint16 | 1 | 0:Disable 1:ATS Control 2:Dry Contact |
| 0x0132 | ExternalGenMaxCharge | 1W(X1)/10W(X3) | uint16 | 1 | |
| 0x013E | 485CommFunSelect | 1 | uint16 | 1 | 0:Modbus485 1:EVCharge 2:DataHub 3:AdaptBoxG2 4:EVC&AdaptBoxG2 5:AdaptBoxG2&Meter 6:EVC&AdaptBoxG2&Meter |

#### Generator Control (0x0140–0x0148)

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x0140 | StartGenMethod | 1 | uint16 | 1 | 0:reference SoC 1:immediately |
| 0x0141 | SwitchOnSoC | 1% | uint16 | 1 | reference SoC start |
| 0x0142 | SwitchOffSoC | 1% | uint16 | 1 | reference SoC stop |
| 0x0143 | MaxRunTime | 1min | uint16 | 1 | |
| 0x0145 | MinRestTime | 1min | uint16 | 1 | |
| 0x0146 | AllowWorkStartTime | — | uint16 | 1 | Hi:Minute Lo:Hour |
| 0x0147 | AllowWorkStopTime | — | uint16 | 1 | Hi:Minute Lo:Hour |
| 0x0148 | GenMinPower | 1W | uint16 | 1 | 0–60000 |

#### Peak Shaving (0x014F–0x0158)

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x014F | PeakShavingDischarPeriod.bP1_Start | — | uint16 | 1 | Hi:Minute Lo:Hour |
| 0x0150 | PeakShavingDischarPeriod.bP1_Stop | — | uint16 | 1 | Hi:Minute Lo:Hour |
| 0x0151 | PeakShavingDischarPeriod.bP2_Start | — | uint16 | 1 | Hi:Minute Lo:Hour |
| 0x0152 | PeakShavingDischarPeriod.bP2_Stop | — | uint16 | 1 | Hi:Minute Lo:Hour |
| 0x0153 | PeakShaving.PeriodBPeakLimits1 | 1W | uint16 | 1 | Discharge Period 1 power limit |
| 0x0154 | PeakShaving.PeriodDPeakLimits2 | 1W | uint16 | 1 | Discharge Period 2 power limit |
| 0x0155 | PeakShaving.PeriodAChargeFromGridEn | 1 | uint16 | 1 | Charge from grid switch |
| 0x0156 | PeakShaving.PeriodAChargePowerLimits | 1W | uint16 | 1 | Charge power from grid |
| 0x0157 | PeakShaving.PeriodAMax_SOC | 1% | uint16 | 1 | Max SoC charged from grid |
| 0x0158 | PeakShaving.PeriodCReserved_SOC | 1% | uint16 | 1 | Reserved SoC |

#### EV Charger / AdaptBox & Misc (0x015C+)

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x015C | EVChargerAddr | 1 | uint16 | 1 | 0–255 |
| 0x015E | AdaptBoxG2Addr | 1 | uint16 | 1 | 0–255 |
| 0x0160 | CTFalutEn | 1 | uint16 | 1 | Cycle CT detection enable. 0:Disable 1:Enable |
| 0x0161 | u16SuperBuckUpEn | 1 | uint16 | 1 | EPS-without-battery enable. 0:Disable 1:Enable |

#### Generator Schedule (0x0162–0x016D)

| Register | Variable | Unit | Format | Description |
|---|---|---|---|---|
| 0x0162 | GenCharge_Start | — | uint16 | Hi:Minute Lo:Hour |
| 0x0163 | GenCharge_End | — | uint16 | Hi:Minute Lo:Hour |
| 0x0164 | GenDischarge_Start | — | uint16 | Hi:Minute Lo:Hour |
| 0x0165 | GenDischarge_End | — | uint16 | Hi:Minute Lo:Hour |
| 0x0166 | GenP2_SetEnable | 1 | uint16 | 0:Disable 1:Enable |
| 0x0167 | GenP2Charge_Start | — | uint16 | Hi:Minute Lo:Hour |
| 0x0168 | GenP2Charge_End | — | uint16 | Hi:Minute Lo:Hour |
| 0x0169 | GenP2Discharge_Start | — | uint16 | Hi:Minute Lo:Hour |
| 0x016A | GenP2Discharge_End | — | uint16 | Hi:Minute Lo:Hour |
| 0x016B | ChargeFromGenEnable | 1 | uint16 | 0:Disable 1:Enable |
| 0x016C | ChargeFromGen_ChargeSoC | 1% | uint16 | 10–100 |
| 0x016D | FastInEPSEn | 1 | uint8 | 0:Disable 1:Enable |

#### TOU Mode (0x016E–0x0173)

| Register | Bytes | Variable | Unit | Format | Description |
|---|---|---|---|---|---|
| 0x016E | Lo | TOUMode_TotalMinSoc | 1% | uint8 | 10–100 |
| 0x016E | Hi | TOUMode_WorkMode | 1 | uint8 | 0xA0:SelfUse 0xA1:AllowCharging 0xA2:ForceDischarging 0xA3:BatteryOff 0xA4:PeakShaving |
| 0x016F | — | TOUMode_SelfuseMinSOC | 1% | uint16 | 10–100 |
| 0x0170 | Lo | TOUMode_ChargeFromGridEn | 1 | uint8 | 0xA0:Disable 0xA1:Enable |
| 0x0170 | Hi | TOUMode_ChargeStopSOC | 1% | uint8 | 10–100 |
| 0x0171 | Lo | TOUMode_DischgPowerLimitRate | 1% | uint8 | 0–100 |
| 0x0171 | Hi | TOUMode_DischargeMinSOC | 1% | uint8 | 10–100 |
| 0x0172–0x0173 | — | TOUMode_PeakShavingLimit | 1W | uint32 | LSB at 0x0172 |

#### Misc (0x0174+)

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x0174 | bShotoffEn (X3) | 1 | uint16 | 1 | 0:NO 1:NC (inverted) |
| 0x0175 | PowerFactor_Qu_VoltRatio2 | 1% | uint16 | 1 | |
| 0x0176 | PowerFactor_Qu_VoltRatio3 | 1% | uint16 | 1 | |
| 0x0177 | CTCutDownINV | 1 | uint16 | 1 | 0:Disable 1:Enable |

---

### BMS Info Holding Registers (0x03, base 0x0200)

| Register | Variable | Format | Len | Description |
|---|---|---|---|---|
| 0x0200 | Subsystem_Num | uint16 | 1 | |
| 0x0201 | BMS_MasterVersion | uint16 | 1 | x.y: Hi=x Lo=y |
| 0x0202–0x0209 | BMS_Slave1–8Version | uint16 | 1 each | |
| 0x020A–0x0210 | masterSN | 14char | 7 | |
| 0x0211–0x0217 | slave1_2SN | 14char | 7 | |
| 0x0218–0x021E | slave3_4SN | 14char | 7 | |
| 0x021F–0x0225 | slave5_6SN | 14char | 7 | |
| 0x0226–0x022C | slave7_8SN | 14char | 7 | |

---

### Data Hub Holding Registers (0x03, internal use only)

| Register | Variable | Description |
|---|---|---|
| 0x3098–0x30A9 | ReadBlockCheckResult | DataHub upgrade results |
| 0xF000 | SetLength | Number of set items |
| 0xF001+ | ReadSetValue | Value of each setting item |

> Note: Internal device communication only.

---

### EV Charger Holding Registers (0x03)

Refer to companion document **`(Solax)EVC ModbusRTU V3.3`** for the EV-charger register map.

---

## Function Code 0x04: Read Input Registers

### Request/Response
Same structure as 0x03 but with function code `0x04`. Error code: `0x84`.

**Example:** Read Mgr FaultMessage + BMS Fault (0x0043–0x0045)
- Request: `01 04 00 43 00 03 41 DF`
- Response: `01 04 06 00 00 00 00 00 00 60 93`

---

### Input Registers (0x04) — Real-time Data

#### Grid & PV (X1 single-phase / X3 three-phase)

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x0000 | GridVoltage (X1) | 0.1V | uint16 | 1 | |
| 0x0001 | GridCurrent (X1) | 0.1A | int16 | 1 | |
| 0x0002 | GridPower (X1) | 1W | int16 | 1 | |
| 0x0003 | PvVoltage1 | 0.1V | uint16 | 1 | |
| 0x0004 | PvVoltage2 | 0.1V | uint16 | 1 | |
| 0x0005 | PvCurrent1 | 0.1A | uint16 | 1 | |
| 0x0006 | PvCurrent2 | 0.1A | uint16 | 1 | |
| 0x0007 | GridFrequency (X1) | 0.01Hz | uint16 | 1 | |
| 0x0008 | Temperature | 1°C | int16 | 1 | Radiator temperature |
| 0x0009 | RunMode | — | uint16 | 1 | See [Run Modes](#run-modes) |
| 0x000A | Powerdc1 | 1W | uint16 | 1 | |
| 0x000B | Powerdc2 | 1W | uint16 | 1 | |

#### Fault Values

| Register | Variable | Unit | Format | Len |
|---|---|---|---|---|
| 0x000C | TemperFaultValue | 1°C | int16 | 1 |
| 0x000D | Pv1VoltFaultValue | 0.1V | uint16 | 1 |
| 0x000E | Pv2VoltFaultValue | 0.1V | uint16 | 1 |
| 0x000F | GfciFaultValue | 1mA | uint16 | 1 |
| 0x0010 | GridVoltFaultValue | 0.1V | uint16 | 1 |
| 0x0011 | GridFreqFaultValueT | 0.01Hz | uint16 | 1 |
| 0x0012 | DciFaultValue | 1mA | uint16 | 1 |
| 0x0013 | TimeCountDown | 1ms | uint16 | 1 |

#### Battery

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x0014 | BatVoltage_Charge1 | 0.1V | int16 | 1 | |
| 0x0015 | BatCurrent_Charge1 | 0.1A | int16 | 1 | |
| 0x0016 | Batpower_Charge1 | 1W | int16 | 1 | |
| 0x0017 | BMS_Connect_State | — | uint16 | 1 | 0:Disconnected 1:Connected |
| 0x0018 | TemperatureBat | 1°C | int16 | 1 | |
| 0x0019 | BDCStatus | — | uint16 | 1 | 0:discharge 1:charge 2:stop |
| 0x001A | GridStatus | — | uint16 | 1 | 0:OnGrid 1:OffGrid |
| 0x001B | MPPTCount | 1 | uint16 | 1 | |
| 0x001C | BatteryCapacity | 1% | uint16 | 1 | |

#### Energy Counters

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x001D–0x001E | OutputEnergy_Charge | 0.1kWh | uint32 | 2 | LSB first |
| 0x0020 | OutputEnergy_Charge_today | 0.1kWh | uint16 | 1 | |
| 0x0021–0x0022 | InputEnergy_Charge | 0.1kWh | uint32 | 2 | LSB first |
| 0x0023 | InputEnergy_Charge_today | 0.1kWh | uint16 | 1 | |
| 0x0024 | BMS_ChargeMaxCurrent | 0.1A | uint16 | 1 | Real time |
| 0x0025 | BMS_DischargeMaxCurrent | 0.1A | uint16 | 1 | Real time |
| 0x0026–0x0027 | BMS_BatteryCapacity | 1Wh | uint16 | 2 | |

#### Faults

| Register | Variable | Format | Len | Description |
|---|---|---|---|---|
| 0x003E | PCSMajorFault | uint16 | 1 | |
| 0x003F | BatteryMajorFault | uint16 | 1 | |
| 0x0040–0x0041 | InvFaultMessage | uint32 | 2 | LSB first. See error code tables |
| 0x0043 | MgrFaultMessage | uint16 | 1 | See [Manager Error Codes](#manager-error-codes) |
| 0x0044–0x0045 | Bat_BMS_FaultMessage | uint32 | 2 | LSB first. See [BMS Warning Codes](#bms-warning-codes) |

#### Meter / CT Data

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x0046–0x0047 | feedin_power | 1W | int32 | 2 | Positive=export, Negative=import. LSB first |
| 0x0048–0x0049 | feedin_energy_total | 0.01kWh | uint32 | 2 | Energy to grid. LSB first |
| 0x004A–0x004B | consum_energy_total | 0.01kWh | uint32 | 2 | Energy from grid. LSB first |

#### Off-grid (X1)

| Register | Variable | Unit | Format | Len |
|---|---|---|---|---|
| 0x004C | Off-gridVoltage (X1) | 0.1V | uint16 | 1 |
| 0x004D | Off-gridCurrent (X1) | 0.1A | uint16 | 1 |
| 0x004E | Off-gridPower (X1) | 1VA | uint16 | 1 |
| 0x004F | Off-gridFrequency (X1) | 0.01Hz | uint16 | 1 |

#### Inverter Energy

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x0050 | Etoday_togrid | 0.1kWh | uint16 | 1 | Today (inverter AC port) |
| 0x0052–0x0053 | Etotal_togrid | 0.1kWh | uint32 | 2 | Total (inverter AC port). LSB first |
| 0x0054 | LockState | — | uint16 | 1 | 0:locked 1:unlocked |
| 0x0066 | BusVolt | 0.1V | uint16 | 1 | |
| 0x0067 | wDcvFaultVal | 0.1V | uint16 | 1 | |
| 0x0068 | wOverLoadFaultval | 1W | uint16 | 1 | |
| 0x0069 | wBatteryVoltFaultVal | 0.1V | uint16 | 1 | |

#### X3 Three-Phase Grid

| Register | Variable | Unit | Format | Len |
|---|---|---|---|---|
| 0x006A | GridVoltage_R | 0.1V | uint16 | 1 |
| 0x006B | GridCurrent_R | 0.1A | int16 | 1 |
| 0x006C | GridPower_R | 1W | int16 | 1 |
| 0x006D | GridFrequency_R | 0.01Hz | uint16 | 1 |
| 0x006E | GridVoltage_S | 0.1V | uint16 | 1 |
| 0x006F | GridCurrent_S | 0.1A | int16 | 1 |
| 0x0070 | GridPower_S | 1W | int16 | 1 |
| 0x0071 | GridFrequency_S | 0.01Hz | uint16 | 1 |
| 0x0072 | GridVoltage_T | 0.1V | uint16 | 1 |
| 0x0073 | GridCurrent_T | 0.1A | int16 | 1 |
| 0x0074 | GridPower_T | 1W | int16 | 1 |
| 0x0075 | GridFrequency_T | 0.01Hz | uint16 | 1 |

#### X3 Off-grid

| Register | Variable | Unit | Format | Len |
|---|---|---|---|---|
| 0x0076 | Off-grid_Volt_R | 0.1V | uint16 | 1 |
| 0x0077 | Off-grid_Current_R | 0.1A | uint16 | 1 |
| 0x0078 | Off-grid_PowerActive_R | 1W | int16 | 1 |
| 0x0079 | Off-grid_PowerS_R | 1VA | uint16 | 1 |
| 0x007A | Off-grid_Volt_S | 0.1V | uint16 | 1 |
| 0x007B | Off-grid_Current_S | 0.1A | uint16 | 1 |
| 0x007C | Off-gridPowerActive_S | 1W | int16 | 1 |
| 0x007D | Off-gridPowerS_S | 1VA | uint16 | 1 |
| 0x007E | Off-grid_Volt_T | 0.1V | uint16 | 1 |
| 0x007F | Off-grid_Current_T | 0.1A | uint16 | 1 |
| 0x0080 | Off-gridPowerActive_T | 1W | int16 | 1 |
| 0x0081 | Off-gridPowerS_T | 1VA | uint16 | 1 |

#### X3 Feedin Power Per Phase

| Register | Variable | Unit | Format | Len |
|---|---|---|---|---|
| 0x0082–0x0083 | FeedinPower_Rphase | 1W | int32 | 2 |
| 0x0084–0x0085 | FeedinPower_Sphase | 1W | int32 | 2 |
| 0x0086–0x0087 | FeedinPower_Tphase | 1W | int32 | 2 |

#### Runtime & Yield

| Register | Variable | Unit | Format | Len |
|---|---|---|---|---|
| 0x0088–0x0089 | On-gridRunTime | 0.1h | int32 | 2 |
| 0x008A–0x008B | Off-gridRunTime | 0.1h | int32 | 2 |
| 0x008E–0x008F | Off-gridYieldTotal | 0.1kWh | uint32 | 2 |
| 0x0090 | Off-gridYieldToday | 0.1kWh | uint16 | 1 |
| 0x0091 | EchargeToday | 0.1kWh | uint16 | 1 |
| 0x0092–0x0093 | EchargeTotal | 0.1kWh | uint32 | 2 |
| 0x0094–0x0095 | SolarEnergyTotal | 0.1kWh | uint32 | 2 |
| 0x0096 | SolarEnergyToday | 0.1kWh | uint16 | 1 |
| 0x0098–0x0099 | feedin_energy_today | 0.01kWh | uint32 | 2 |
| 0x009A–0x009B | consum_energy_today | 0.01kWh | uint16 | 2 |

#### Inverter Voltage (X3)

| Register | Variable | Unit | Format | Len |
|---|---|---|---|---|
| 0x009C | InvVoltR | 0.1V | uint16 | 1 |
| 0x009D | InvVoltS | 0.1V | uint16 | 1 |
| 0x009E | InvVoltT | 0.1V | uint16 | 1 |

#### Meter2 Data

| Register | Variable | Unit | Format | Len |
|---|---|---|---|---|
| 0x00A8–0x00A9 | feedin_power_Meter2 | 1W | int32 | 2 |
| 0x00AA–0x00AB | feedin_energy_total_Meter2 | 0.01kWh | uint32 | 2 |
| 0x00AC–0x00AD | consum_energy_total_Meter2 | 0.01kWh | uint32 | 2 |
| 0x00AE–0x00AF | feedin_energy_today_Meter2 | 0.01kWh | uint16 | 2 |
| 0x00B0–0x00B1 | consum_energy_today_Meter2 | 0.01kWh | uint16 | 2 |
| 0x00B2–0x00B3 | FeedinPower_Rphase_Meter2 | 1W | int32 | 2 |
| 0x00B4–0x00B5 | FeedinPower_Sphase_Meter2 | 1W | int32 | 2 |
| 0x00B6–0x00B7 | FeedinPower_Tphase_Meter2 | 1W | int32 | 2 |
| 0x00B8 | Meter1CommunicationState | 1 | uint16 | 1 | 0:Com Error 1:Normal |
| 0x00B9 | Meter2CommunicationState | 1 | uint16 | 1 | 0:Com Error 1:Normal |

#### BMS Extended

| Register | Variable | Unit | Format | Len |
|---|---|---|---|---|
| 0x00BA | Battery_Tem_High | 0.1°C | int16 | 1 |
| 0x00BB | Battery_Tem_Low | 0.1°C | int16 | 1 |
| 0x00BC | Cell_Voltage_High | 0.001V | uint16 | 1 |
| 0x00BD | Cell_Voltage_Low | 0.001V | uint16 | 1 |
| 0x00BE | BMS_UserSOC | 1% | uint16 | 1 |
| 0x00BF | BMS_UserSOH | 1% | uint16 | 1 |

#### Grid Meter Extended

| Register | Variable | Unit | Format | Len |
|---|---|---|---|---|
| 0x00C0 | GridReactivePower_Total_Meter | 1Var | int16 | 1 |
| 0x00C1 | GridReactivePower_R_Meter | 1Var | int16 | 1 |
| 0x00C2 | GridReactivePower_S_Meter | 1Var | int16 | 1 |
| 0x00C3 | GridReactivePower_T_Meter | 1Var | int16 | 1 |
| 0x00C4 | GridPowerFactor_Total_Meter | 0.01 | int16 | 1 |
| 0x00C5 | GridPowerFactor_R_Meter | 0.01 | int16 | 1 |
| 0x00C6 | GridPowerFactor_S_Meter | 0.01 | int16 | 1 |
| 0x00C7 | GridPowerFactor_T_Meter | 0.01 | int16 | 1 |
| 0x00C8 | GridFrequency_Meter | 0.01Hz | uint16 | 1 |
| 0x00C9 | GridVoltage_Total_Meter | 0.1V | uint16 | 1 |
| 0x00CA | GridVoltage_R_Meter | 0.1V | uint16 | 1 |
| 0x00CB | GridVoltage_S_Meter | 0.1V | uint16 | 1 |
| 0x00CC | GridVoltage_T_Meter | 0.1V | uint16 | 1 |
| 0x00CD | GridCurrent_Total_Meter | 0.1A | int16 | 1 |
| 0x00CE | GridCurrent_R_Meter | 0.1A | int16 | 1 |
| 0x00CF | GridCurrent_S_Meter | 0.1A | int16 | 1 |
| 0x00D0 | GridCurrent_T_Meter | 0.1A | int16 | 1 |

#### Remote Control (Input Registers 0x0100+)

| Register | Variable | Unit | Format | Len | Description |
|---|---|---|---|---|---|
| 0x0100 | ModbusPowerControl | 1 | uint16 | 1 | 0:disable 1:powerCtrl 2:electricQtyCtrl 3:SOCTargetCtrl 4:PushPower±Mode 5:PushPowerZeroMode 6:SelfConsume Charge-Discharge 7:SelfConsume ChargeOnly |
| 0x0101 | TargetFinishFlag | — | uint16 | 1 | 0:unfinished 1:finish |
| 0x0102–0x0103 | ActivePowerTarget | 1W | int32 | 2 | |
| 0x0104–0x0105 | wReactivePowerTarget | 1Var | int32 | 2 | |
| 0x0106–0x0107 | wActivePowerReal | 1W | int32 | 2 | |
| 0x0108–0x0109 | wReactivePowerReal | 1Var | int32 | 2 | |
| 0x010A–0x010B | wActivePower_Upper | 1W | int32 | 2 | |
| 0x010C–0x010D | wActivePower_Lower | 1W | int32 | 2 | |
| 0x010E–0x010F | wReactivePower_Upper | 1Var | int32 | 2 | |
| 0x0110–0x0111 | wReactivePower_Lower | 1Var | int32 | 2 | |
| 0x0112–0x0113 | TargetEnergy | 1Wh | int32 | 2 | |
| 0x0114–0x0115 | Charge_Discharg_Power | 1W | int32 | 2 | |
| 0x0116–0x0117 | ChargeableElectricCapacity | 1Wh | uint32 | 2 | |
| 0x0118–0x0119 | DischargeableElectricCapacity | 1Wh | uint32 | 2 | |
| 0x011A | Time_of_Duration | 1s | uint16 | 1 | |
| 0x011B | TargetSoc | 1% | uint16 | 1 | |
| 0x011C | SocUpper | 1% | uint16 | 1 | |
| 0x011D | SocLower | 1% | uint16 | 1 | |
| 0x011E | RemoteCtrlTimeOut | 1s | uint16 | 1 | 4–65535 |
| 0x011F | wBatteryForceChargeFlag | 1 | uint16 | 1 | 0:NoAction 1:ForceCharge |
| 0x0120 | wBMSRelayState | 1 | uint16 | 1 | 0:OFF 1:ON |
| 0x0121 | BMS_RestartFlag | 1 | uint16 | 1 | 0:Initial 1:Restart |

---

### Self-Test Input Registers (0x04, base 0x0180)

| Register | Variable | Unit | Format |
|---|---|---|---|
| 0x0180 | wSelfTest_step | 1 | uint16 |
| 0x0181 | wSelfTest_Time | 1s | uint16 |
| 0x0182 | wSelfTest_State | 1 | uint16 |
| 0x0183 | Ovp_Threshold_Target (59.S2) | 0.1V | uint16 |
| 0x0184 | Ovp_Threshold_Time | 1ms | uint16 |
| 0x0185 | Ovp_Outcome_Sample_R | 0.1V | uint16 |
| 0x0186 | Outcome_TripValue_R | 0.1V | uint16 |
| 0x0187 | Ovp_Outcome_Time_R | 1ms | uint16 |
| 0x0188–0x018A | Ovp_Outcome_*_S (X3) | 0.1V/0.1V/1ms | uint16 |
| 0x018B–0x018D | Ovp_Outcome_*_T (X3) | 0.1V/0.1V/1ms | uint16 |
| 0x018E | Uvp_Threshold_Target (27.S1) | 0.1V | uint16 |
| 0x018F | Uvp_Threshold_Time | 1ms | uint16 |
| 0x0190–0x0192 | Uvp_Outcome_*_R | 0.1V/0.1V/1ms | uint16 |
| 0x0193–0x0195 | Uvp_Outcome_*_S (X3) | 0.1V/0.1V/1ms | uint16 |
| 0x0196–0x0198 | Uvp_Outcome_*_T (X3) | 0.1V/0.1V/1ms | uint16 |
| 0x0199 | UvpRestric_Threshold_Target (27.S2) | 0.1V | uint16 |
| 0x019A | UvpRestric_Threshold_Time | 1ms | uint16 |
| 0x019B–0x019D | UvpRestric_Outcome_*_R | 0.1V/0.1V/1ms | uint16 |
| 0x019E–0x01A0 | UvpRestric_Outcome_*_S (X3) | 0.1V/0.1V/1ms | uint16 |
| 0x01A1–0x01A3 | UvpRestric_Outcome_*_T (X3) | 0.1V/0.1V/1ms | uint16 |
| 0x01A4 | Ofp_Threshold_Target (81>.S1) | 0.01Hz | uint16 |
| 0x01A5 | Ofp_Threshold_Time | 1ms | uint16 |
| 0x01A6–0x01A8 | Ofp_Outcome_*_R | 0.01Hz/0.01Hz/1ms | uint16 |
| 0x01A9–0x01AB | Ofp_Outcome_*_S (X3) | 0.01Hz/0.01Hz/1ms | uint16 |
| 0x01AC–0x01AE | Ofp_Outcome_*_T (X3) | 0.01Hz/0.01Hz/1ms | uint16 |
| 0x01AF | Ufp_Threshold_Target (81<.S1) | 0.01Hz | uint16 |
| 0x01B0 | Ufp_Threshold_Time | 1ms | uint16 |
| 0x01B1–0x01B3 | Ufp_Outcome_*_R | 0.01Hz/0.01Hz/1ms | uint16 |
| 0x01B4–0x01B6 | Ufp_Outcome_*_S (X3) | 0.01Hz/0.01Hz/1ms | uint16 |
| 0x01B7–0x01B9 | Ufp_Outcome_*_T (X3) | 0.01Hz/0.01Hz/1ms | uint16 |
| 0x01BA | OfpRestric_Threshold_Target (81>.S2) | 0.01Hz | uint16 |
| 0x01BB | OfpRestric_Threshold_Time | 1ms | uint16 |
| 0x01BC–0x01BE | OfpRestric_Outcome_*_R | 0.01Hz/0.01Hz/1ms | uint16 |
| 0x01BF–0x01C1 | OfpRestric_Outcome_*_S (X3) | 0.01Hz/0.01Hz/1ms | uint16 |
| 0x01C2–0x01C4 | OfpRestric_Outcome_*_T (X3) | 0.01Hz/0.01Hz/1ms | uint16 |
| 0x01C5 | UfpRestric_Threshold_Target (81<.S2) | 0.01Hz | uint16 |
| 0x01C6 | UfpRestric_Threshold_Time | 1ms | uint16 |
| 0x01C7–0x01C9 | UfpRestric_Outcome_*_R | 0.01Hz/0.01Hz/1ms | uint16 |
| 0x01CA–0x01CC | UfpRestric_Outcome_*_S (X3) | 0.01Hz/0.01Hz/1ms | uint16 |
| 0x01CD–0x01CF | UfpRestric_Outcome_*_T (X3) | 0.01Hz/0.01Hz/1ms | uint16 |
| 0x01D0 | Ovp10mAvg_Threshold_Target (59.S1) | 0.1V | uint16 |
| 0x01D1 | Ovp10mAvg_Threshold_Time | 1s | uint16 |
| 0x01D2–0x01D4 | Ovp10mAvg_Outcome_*_R | 0.1V/0.1V/1s | uint16 |
| 0x01D5–0x01D7 | Ovp10mAvg_Outcome_*_S (X3) | 0.1V/0.1V/1s | uint16 |
| 0x01D8–0x01DA | Ovp10mAvg_Outcome_*_T (X3) | 0.1V/0.1V/1s | uint16 |

`wSelfTest_State` bits (1 = finished, 0 = testing): bit0=Ovp, bit1=Uvp, bit2=UvpRestric, bit3=Ofp, bit4=Ufp, bit5=OfpRestric, bit6=UfpRestric, bit7=Ovp10mAvg.

---

### Parallel-State Input Registers (0x04, base 0x01DD)

`0x01DD` = `SystemInvNum`. Aggregated values for the cluster begin at `0x01E0`; per-slave blocks (slave 1..9) follow from `0x0204` and repeat in 26-register chunks.

#### Cluster Aggregate (0x01E0–0x0201)

| Register | Variable | Unit | Format |
|---|---|---|---|
| 0x01E0–0x01E1 | InvActivePower_R_All | 1W | int32 |
| 0x01E2–0x01E3 | InvActivePower_S_All | 1W | int32 |
| 0x01E4–0x01E5 | InvActivePower_T_All | 1W | int32 |
| 0x01E6–0x01E7 | InvReactiveOrApparentPower_R_All | 1VA | int32 |
| 0x01E8–0x01E9 | InvReactiveOrApparentPower_S_All | 1VA | int32 |
| 0x01EA–0x01EB | InvReactiveOrApparentPower_T_All | 1VA | int32 |
| 0x01EC–0x01ED | InvCurrent_R_All | 0.1A | int32 |
| 0x01EE–0x01EF | InvCurrent_S_All | 0.1A | int32 |
| 0x01F0–0x01F1 | InvCurrent_T_All | 0.1A | int32 |
| 0x01F2–0x01F3 | PvPower_ChannelA_All | 1W | uint32 |
| 0x01F4–0x01F5 | PvPower_ChannelB_All | 1W | uint32 |
| 0x01F6–0x01F7 | PvCurrent_ChannelA_All | 0.1A | uint32 |
| 0x01F8–0x01F9 | PvCurrent_ChannelB_All | 0.1A | uint32 |
| 0x01FA–0x01FB | BatPower_All | 1W | int32 |
| 0x01FC–0x01FD | BatCurrent_All | 0.1A | int32 |
| 0x01FE–0x01FF | ChargePowerLimit_All | 1W | int32 |
| 0x0200–0x0201 | DischargePowerLimit_All | 1W | int32 |

#### Per-Slave Blocks

Each slave occupies a 26-register block. The first slave starts at `0x0204` and subsequent slaves are spaced `+26 (0x1A)` apart:

| Slave | Block start |
|---|---|
| 1 | 0x0204 |
| 2 | 0x021E |
| 3 | 0x0238 |
| 4 | 0x0252 |
| 5 | 0x026C |
| 6 | 0x0286 |
| 7 | 0x02A0 |
| 8 | 0x02BA |
| 9 | 0x02D4 |

Layout per block (offsets relative to block start):

| Offset | Variable | Unit | Format |
|---|---|---|---|
| +0 | InvActivePower_R | 1W | int16 |
| +1 | InvActivePower_S | 1W | int16 |
| +2 | InvActivePower_T | 1W | int16 |
| +3 | InvReactiveOrApparentPower_R | 1VA | int16 |
| +4 | InvReactiveOrApparentPower_S | 1VA | int16 |
| +5 | InvReactiveOrApparentPower_T | 1VA | int16 |
| +6 | InvCurrent_R | 0.1A | int16 |
| +7 | InvCurrent_S | 0.1A | int16 |
| +8 | InvCurrent_T | 0.1A | int16 |
| +9 | PvPower_ChannelA | 1W | uint16 |
| +10 | PvPower_ChannelB | 1W | uint16 |
| +11 | PvVoltage_ChannelA | 0.1V | uint16 |
| +12 | PvVoltage_ChannelB | 0.1V | uint16 |
| +13 | PvCurrent_ChannelA | 0.1A | uint16 |
| +14 | PvCurrent_ChannelB | 0.1A | uint16 |
| +15 | BatPower | 1W | uint16 |
| +16 | BatVoltage | 0.1V | uint16 |
| +17 | BatCurrent | 0.1A | uint16 |
| +18 | ChargePowerLimit | 1W | uint16 |
| +19 | DischargePowerLimit | 1W | uint16 |
| +20 | BatFaultMessage | 1 | uint16 |
| +21 | BatCapacity | 1% | uint16 |
| +22..+25 | reserved | — | uint32 ×2 |

---

### Data Hub Input Registers (0x04, internal use only)

| Register | Variable | Description |
|---|---|---|
| 0x06DF | total_length | |
| 0x06E0 | PallerLen | |
| 0x06E1 | bDHWakeUpSlaver | |
| 0x06E2 | bDHMasterBmsSwitchState | |
| 0x06E3 | bDHMasterBmsComState | |
| 0x06E4 | bDHMasterBypassConfig | |
| 0x06E5 | bDHMasterBypassWorkState | |
| 0x06E6 | bDHExternalGen | |
| 0x06E7 | bDHMasterRunMode | |
| 0x06E8 | bDHMasterCom485State | X1 only |
| 0x06E9 | bDHBatteryChargeMaxSoc | X1 only |
| 0x0700 | ChargeLen | EV Charge |
| 0x0701 | RefPowerToEV | |
| 0x0702–0x0703 | PowerToEV | uint32 |
| 0x0704 | PvRef | |
| 0x0705–0x0706 | FeedinPower_Rphase (X3) / FeedinPower (X1) | uint32 |
| 0x0707–0x0708 | FeedinPower_Sphase (X3) | uint32 |
| 0x0709–0x070A | FeedinPower_Tphase (X3) | uint32 |
| 0xEF00 | bGetChargePower | X3 current charging power |
| 0xF000–0xF01D | Error / Warning data | uint16×30 |
| 0xF01E | RealTime Length | |
| 0xF01F+ | RealTime Data | |

> Note: Internal device communication only.

---

### EV Charger Input Registers (0x04)

Refer to companion document **`(Solax)EVC ModbusRTU V3.3`**.

---

## Function Code 0x06: Write Single Register

### Request Format
| Field | Size | Description |
|---|---|---|
| Slave ID | 1 byte | Default `0x01` |
| Function code | 1 byte | `0x06` |
| Register address | 2 bytes | |
| Value | 2 bytes | |
| CRC | 2 bytes | |

Error code: `0x86`.

**Example:** Write CheckingTime 60s (0x0002)
- Request: `01 06 00 02 00 3C 28 1B`
- Response: `01 06 00 02 00 3C 28 1B`

### Write Registers

#### Unlock & System

| Register | Variable | Unit | Range | EE | Description |
|---|---|---|---|---|---|
| 0x0000 | UnlockPassword | 1 | — |  | Required before writing |
| 0x0001 | ReconnectionTime | 1s | 15–600 | ★ | |
| 0x0002 | CheckingTime | 1s | X1: 0–1500, X3: 0–1000 | ★ | |
| 0x001C | SystemON_OFF | 1 | 0/1 | ★ | 0:OFF 1:ON |
| 0x001D | FactoryReset | 1 | 1 |  | Write 1 to reset |
| 0x001E | Inverter_Clear_History | 1 | 1 |  | Write 1 to clear |
| 0x001B | MatchResistanceSet | 1 | 0/1 | ★ | 0:disable 1:enable |
| 0x0028 | EpsDcvAdjEn (X3) | 1 | 0/1 |  | |
| 0x002F | ClearEnergy_Meter/CT_1 | 1 | 1 |  | Write 1 to clear |
| 0x0034 | Adjust_CT_Zero (X3) | 1 | 1 |  | Write 1 |
| 0x0038 | EpsPhaseSeqDetect | 1 | 0/1 |  | |
| 0x008C | InvVoltZeroAdj (X3) | 1 | 1/2/3 |  | 1:Prepare 2:Start 3:Check result |
| 0x009B | ATE_Test | 1 | 1 |  | Write 1 to trigger |
| 0x00A3 | Reset_Meter2_Energy | 1 | 1 |  | Write 1 to clear |
| 0x00DB | ResetErrorLog | 1 | 1 |  | Write 1 |
| 0x00DC | ResetINVEnergy | 1 | 1 |  | Write 1 |
| 0x00DF | ResetINV | 1 | 1 |  | Write 1 |
| 0x00E2 | BMS_Restart | 1 | 1 |  | Write 1 |

#### Battery Adjustment

| Register | Variable | Unit | Range | EE | Description |
|---|---|---|---|---|---|
| 0x0003 | Adjust_Battery_U | 0.1V | 0–3900 | ★ | |
| 0x0004 | Adjust_Battery_I | 0.1A | -350 – 350 | ★ | Positive=charge, Negative=discharge |

#### Grid Protection Write

| Register | Variable | Unit | Range (X1 / X3) | EE |
|---|---|---|---|---|
| 0x0005 | Vac_Min | 0.1V | X1: 230–3000 / X3: 250–2300 | ★ |
| 0x0006 | Vac_Max | 0.1V | X1: 1000–3000 / X3: 1270–3000 | ★ |
| 0x0007 | Fac_Min | 0.01Hz | 4000–6500 | ★ |
| 0x0008 | Fac_Max | 0.01Hz | X1: 4500–7000 / X3: 4000–7000 | ★ |
| 0x0009 | SafetyCode | — | See [Safety Codes](#safety-codes) | ★ |
| 0x000A | MateBoxEnable | 1 | 0/1 | ★ |
| 0x000B | Grid_10Min_high | 0.1V | 1500–3000 | ★ |
| 0x000C | Vac_Min_slow_protect | 0.1V | X1: 1500–3000 / X3: 250–2300 | ★ |
| 0x000D | Vac_Max_slow_protect | 0.1V | X1: 1000–3120 / X3: 1270–3000 | ★ |
| 0x000E | Fac_Min_slow_Protect | 0.01Hz | 4000–6500 | ★ |
| 0x000F | Fac_Max_slow_Protect | 0.01Hz | X1: 4500–7000 / X3: 4000–7000 | ★ |

#### Calibration

| Register | Variable | Unit | Range | EE |
|---|---|---|---|---|
| 0x0010 | DCI_Limit | 1mA | 20–1000 | ★ |
| 0x0011 | active_Power_Limit | — | 0–100 | ★ |
| 0x0012 | Adjust_Pv1_Current | 0.01A | 10–3000 | ★ |
| 0x0013 | Adjust_Pv2_Current | 0.01A | 10–3000 | ★ |
| 0x0014 | Adjust_Pv1_Volt | 0.1V | 100–10000 | ★ |
| 0x0015 | Adjust_Pv2_Volt | 0.1V | 100–10000 | ★ |
| 0x0016 | Adjust_AC_Current_R | 0.1A | 10–300 | ★ |
| 0x0017 | Adjust_AC_Volt_R | 0.1V | X1: 1500–3000 / X3: 500–3000 | ★ |
| 0x0027 | CtType (X3) | 1 | 0:100A 1:200A | ★ |
| 0x0029 | CalibGainInvVoltR (X3) | 0.1V | 500–3000 | ★ |
| 0x002A | CalibGainInvVoltS (X3) | 0.1V | 500–3000 | ★ |
| 0x002B | CalibGainInvVoltT (X3) | 0.1V | 500–3000 | ★ |
| 0x002C | CalibEPSDcvAdjR (X3) | 0.01V | 500–3000 | ★ |
| 0x002D | CalibEPSDcvAdjS (X3) | 0.01V | 500–3000 | ★ |
| 0x002E | CalibEPSDcvAdjT (X3) | 0.01V | 500–3000 | ★ |
| 0x0030 | Adjust_AC_Current_S (X3) | 0.1A | 10–300 | ★ |
| 0x0031 | Adjust_AC_Volt_S (X3) | 0.1V | X1: 1500–3000 / X3: 500–3000 | ★ |
| 0x0032 | Adjust_AC_Current_T (X3) | 0.1A | 10–300 | ★ |
| 0x0033 | Adjust_AC_Volt_T (X3) | 0.1V | X1: 1500–3000 / X3: 500–3000 | ★ |
| 0x0035 | Adjust_CT_Power_R (X3) | 1W | 0–65535 | ★ |
| 0x0036 | Adjust_CT_Power_S (X3) | 1W | 0–65535 | ★ |
| 0x0037 | Adjust_CT_Power_T (X3) | 1W | 0–65535 | ★ |

#### Charger Mode & Battery

| Register | Variable | Unit | Range | EE | Description |
|---|---|---|---|---|---|
| 0x001F | SolarChargerUseMode | — | 0–5 | ★ | 0:SelfUse 1:FeedIn 2:BackUp 3:Manual 4:PeakShaving 5:TOU |
| 0x0020 | ManualMode | 1 | 0–2 |  | 0:Stop 1:ForceCharge 2:ForceDischarge |
| 0x0021 | wBattery1_Type | 1 | 0/1 | ★ | 0:LeadAcid 1:Lithium |
| 0x0022 | Charge_floatVolt | 0.1V | X1: 850–4000 / X3: 1600–8000 | ★ | |
| 0x0023 | Discharge_CutVolt | 0.1V | X1: 850–4000 / X3: 1600–8000 | ★ | |
| 0x0024 | Battery1_ChargeMaxCurrent | 0.1A | 0–300 | ★ | |
| 0x0025 | Battery1_DischargeMaxCurrent | 0.1A | 0–300 | ★ | |
| 0x0026 | wBatteryDischargeBackupVoltage | 0.1V | X1: 850–4000 / X3: 1600–8000 | ★ | |
| 0x0060 | absorpt_voltage | 0.1V | X1: 850–4000 / X3: 1600–8000 | ★ | |

#### SoC & Night Charge Write

| Register | Variable | Unit | Range | EE |
|---|---|---|---|---|
| 0x0061 | SelfUse_Discharge_MinSoC | 1% | 10–100 | ★ |
| 0x0062 | SelfUse_NightCharge_Enable | 1 | 0/1 | ★ |
| 0x0063 | SelfUse_NightCharge_UpperSoC | 1% | 10–100 | ★ |
| 0x0064 | Feedin_NightCharge_UpperSoC | 1% | 10–100 | ★ |
| 0x0065 | Feedin_Discharge_MinSoC | 1% | 10–100 | ★ |
| 0x0066 | BackUp_NightCharge_UpperSoC | 1% | 30–100 | ★ |
| 0x0067 | BackUp_Discharge_MinSoC | 1% | 15–100 | ★ |

#### Charge / Discharge Period Write

Time registers: Hi byte = Hour (0–23), Lo byte = Minute (0–59). Value = `(hour << 8) | minute`.

| Register | Variable | EE |
|---|---|---|
| 0x0068 | NightCharge_Period1_StartTime | ★ |
| 0x0069 | NightCharge_Period1_EndTime | ★ |
| 0x006A | Discharge_Period1_StartTime | ★ |
| 0x006B | Discharge_Period1_EndTime | ★ |
| 0x006C | Set_Chrg&DischrgPeriod2_Enable (0/1) | ★ |
| 0x006D | NightCharge_Period2_StartTime | ★ |
| 0x006E | NightCharge_Period2_EndTime | ★ |
| 0x006F | Discharge_Period2_StartTime | ★ |
| 0x0070 | Discharge_Period2_EndTime | ★ |

#### Off-grid Write

| Register | Variable | Unit | Range | EE |
|---|---|---|---|---|
| 0x0043 | Off-grid_Mute | 1 | 0/1 | ★ |
| 0x0044 | Off-grid_MinSoC | 1% | 10–25 | ★ |
| 0x0045 | Off-grid_Frequency | 1 | 0:50Hz 1:60Hz |  |

#### Export Control Write

| Register | Variable | Unit | Range (X1 / X3) | EE |
|---|---|---|---|---|
| 0x0041 | ExportControl_Factory_Limit | 1W(X1)/10W(X3) | X1: 0–60000 / X3: 0–30000 | ★ |
| 0x0042 | ExportControl_User_Limit | 1W(X1)/10W(X3) | X1: 0–60000 / X3: 0–30000 | ★ |

#### Language & Misc Write

| Register | Variable | Unit | Range | EE | Description |
|---|---|---|---|---|---|
| 0x0046 | AgeingMode | 1 | 0/1 |  | For ATE use |
| 0x0047 | Language | 1 | 0–9 | ★ | 0:EN 1:DE 2:FR 3:PL 4:ES 5:PT 6:IT 7:CN(BAN) 8:UA 9:BR |
| 0x0048 | EnableMPPT | 1 | 0/1 |  | |

#### Trip Time Write

| Register | Variable | Unit | Range (X1 / X3) | EE |
|---|---|---|---|---|
| 0x0049 | wTuvp_L2 | 1ms(X1)/10ms(X3) | 0–10000 | ★ |
| 0x004A | wTovp_L2 | 1ms(X1)/10ms(X3) | 0–10000 | ★ |
| 0x004B | wTufp_L2 | 1ms(X1)/10ms(X3) | 0–10000 | ★ |
| 0x004C | wTofp_L2 | 1ms(X1)/10ms(X3) | 0–10000 | ★ |
| 0x004D | wTuvp_L1 | 1ms(X1)/10ms(X3) | X1: 0–50000 / X3: 0–10000 | ★ |
| 0x004E | wTovp_L1 | 1ms(X1)/10ms(X3) | X1: 0–60000 / X3: 0–10000 | ★ |
| 0x004F | wTufp_L1 | 1ms(X1)/10ms(X3) | 0–10000 | ★ |
| 0x0050 | wTofp_L1 | 1ms(X1)/10ms(X3) | 0–10000 | ★ |

#### Feature Control Write

| Register | Variable | Range | EE | Description |
|---|---|---|---|---|
| 0x0051 | PVConnection | 0:MULTI 1:COMM | ★ | X1 only |
| 0x0052 | ShutDown | 0:Disable 1:Enable | ★ | |
| 0x0053 | MicroGrid | 0:Disable 1:Enable | ★ | |
| 0x0054 | SelfTestStart | 0:stop 1–8:individual tests 10:all |  | |
| 0x0055 | ClearOverloadFault | 1 |  | Write 1 |
| 0x0056 | Bat_Awaken | 1 |  | Lead-acid, Write 1 |

#### Over/Under Frequency Load Response Write

| Register | Variable | Unit | Range (X1 / X3) | EE |
|---|---|---|---|---|
| 0x0057 | OFPL_CurveType | 1 | 0:Symmetry 1:Asymmetry | ★ |
| 0x0058 | OFPL_Tstop | 1s | 0–600 | ★ |
| 0x0059 | OFPL_RemovePoint | 0.01Hz | X1: 4955–5200 / X3: 5000–6200 | ★ |
| 0x005A | OFPL_StartPoint | 0.01Hz | X1: 5010–5200 / X3: 5000–6200 | ★ |
| 0x005B | OFPL_SetRate | 1% | 2–12 | ★ |
| 0x005C | OFPL_DelayTime | 1ms | X1: 0–2000 / X3: 0–1000 | ★ |
| 0x005D | OFPL_fstop_disch | 0.01Hz | X1: 5050–5200 / X3: 5050–6200 | ★ |
| 0x005E | OFPL_fPmin | 0.01Hz | X1: 5100–5300 / X3: 5100–6300 | ★ |
| 0x005F | Reset_Mgr_EE | 1 | 1 |  | 1:Reset |
| 0x009F | OFPL_Wgra | 0.0001 | 500–10000 | ★ |
| 0x0092 | UFPL_StartPoint | 0.01Hz | X1: 4600–4990 / X3: 4600–6000 | ★ |
| 0x0093 | UFPL_SetRate | 1% | 2–12 | ★ |
| 0x0094 | UFPL_DelayTime | 1ms | 0–1000 | ★ |
| 0x0095 | UFPL_RemovePoint | 0.01Hz | X1: 4600–5045 / X3: 4600–5000 | ★ |
| 0x0096 | UFPL_fstop_ch | 0.01Hz | X1: 4800–4950 / X3: 4800–5950 | ★ |
| 0x0097 | UFPL_fPmax | 0.01Hz | X1: 4700–4900 / X3: 4700–5900 | ★ |

#### Power Factor Write

| Register | Variable | Unit | Range (X1 / X3) | EE |
|---|---|---|---|---|
| 0x0072 | PowerfactorMode | 1 | 0–5 | ★ |
| 0x0073 | PowerfactorData | 0.01 | 80–100 | ★ |
| 0x0074–0x0077 | PowerFactor_Curve_PF1–4 | 0.01 | 80–100 | ★ |
| 0x0078–0x007B | PowerFactor_Curve_Power1–4 | 1% | 0–100 | ★ |
| 0x007C | PowerFactor_Curve_PfLockInPoint | 0.01 | 105–110 | ★ |
| 0x007D | PowerFactor_Curve_PfLockOutPoint | 0.01 | 90–98 | ★ |
| 0x007E | PowerFactor_Curve_3Tau | 1s | 6–180 | ★ |
| 0x007F | PowerFactor_Qu_VoltRatio1 | 1% | 0–60 | ★ |
| 0x0080 | PowerFactor_Qu_VoltRatio4 | 1% (int16) | X1: -60…-30 / X3: -60…0 | ★ |
| 0x0081 | PowerFactor_Qu_QuResponseV1 | 0.1V | X1: 1800–2530 / X3: 250–2300 | ★ |
| 0x0082 | PowerFactor_Qu_QuResponseV2 | 0.1V | X1: 1800–2530 / X3: 250–2300 | ★ |
| 0x0083 | PowerFactor_Qu_QuResponseV3 | 0.1V | X1: 2070–2650 / X3: 1270–3000 | ★ |
| 0x0084 | PowerFactor_Qu_QuResponseV4 | 0.1V | X1: 2070–2650 / X3: 1270–3000 | ★ |
| 0x0085 | PowerFactor_Qu_K | 1% (int16) | -100–100 | ★ |
| 0x0086 | PowerFactor_Qu_3Tau | 1s | 6–180 | ★ |
| 0x0087 | PowerFactor_Qu_QuDelayTimer | 1s | X1: 0–30 / X3: 0–200 | ★ |
| 0x0088 | PowerFactor_Qu_QuLockEn | 1 | 0/1 | ★ |
| 0x0089 | PowerFactor_Qu_QuLockIn | 1% | 0–20 | ★ |
| 0x008A | PowerFactor_Qu_QuLockOut | 1% | 0–20 | ★ |
| 0x008B | PowerFactor_FixQPower | 1Var(X1)/10Var(X3) | Min..Max | ★ |
| 0x0114 | PowerFactor_Qu_VoltRatio2 | 1% | 0–60 |  |
| 0x0115 | PowerFactor_Qu_VoltRatio3 | 1% | 0–60 |  |

#### PU Function Write

| Register | Variable | Unit | Range (X1 / X3) | EE |
|---|---|---|---|---|
| 0x00AE | PuFuncEnable | 1 | 0/1 | ★ |
| 0x00AF | PuFunc_ResponseV1 | 0.1V | X1: 2070–2760 / X3: 250–2300 | ★ |
| 0x00B0 | PuFunc_ResponseV2 | 0.1V | X1: 2070–2760 / X3: 250–2300 | ★ |
| 0x00B1 | PuFunc_ResponseV3 | 0.1V | X1: 2070–2760 / X3: 1270–3000 | ★ |
| 0x00B2 | PuFunc_ResponseV4 | 0.1V | X1: 2070–2760 / X3: 1270–3000 | ★ |
| 0x00B3 | PuFunc_3Tau | 1s | X1: 6–180 / X3: 3–180 | ★ |
| 0x00CB | SetPuPower1 | 1% | 0–20 | ★ |
| 0x00CC | SetPuPower2 | 1% | 0–100 | ★ |
| 0x00CD | SetPuPower3 | 1% | 0–100 | ★ |
| 0x00CE | SetPuPower4 | 1% | 0–20 | ★ |

#### Misc Write

| Register | Variable | Unit | Range | EE | Description |
|---|---|---|---|---|---|
| 0x0071 | MainBreakerCurrentLimit | 1A | X1: 10–100 / X3: 10–250 | ★ | |
| 0x008D | PgridBias | — | 0–2 | ★ | 0:Disable 1:Grid 2:INV |
| 0x008E | EpsRestartSoc | 1% | 10–100 | ★ | |
| 0x008F | 485CommFunSelect | 1 | 0–6 | ★ | 0:Modbus485 1:EVCharge 2:DataHub 3:AdaptBoxG2 4:EVC&AdaptBoxG2 5:AdaptBoxG2&Meter 6:EVC&AdaptBoxG2&Meter |
| 0x0090 | ConnectSlop (X3) | 1% | 1–10000 | ★ | |
| 0x0091 | ReconnectSlop (X3) | 1% | 1–10000 | ★ | |
| 0x0098 | ShadowFixFuncEnable2 | 1 | 0–3 | ★ | 0:Off 1:Low 2:Middle 3:High |
| 0x0099 | HotStandbyEN | 1 | 0/1 | ★ | 0:enable 1:disable |
| 0x009A | ExtendBmsSetting | 1 | 0/1 | ★ | |
| 0x009C | wShadowFixFuncEnable | 1 | 0–3 | ★ | |
| 0x009D | ExternalSignal | 1 | — | ★ | |
| 0x009E | PhasePowerBalance (X3) | 1 | 0/1 | ★ | |
| 0x00A0 | MeterFunction | 1 | 0/1 | ★ | |
| 0x00A1 | Meter1_ID | 1 | 1–200 | ★ | |
| 0x00A2 | Meter2_ID | 1 | 1–200 | ★ | |
| 0x00A4 | DirectionMeterCT1 | 1 | 0/1 | ★ | 0:Positive 1:Negative |
| 0x00A5 | DirectionMeter2 | 1 | 0/1 | ★ | |
| 0x00A6 | DischCutOffPoint_DifferentEN | 1 | 0/1 | ★ | Lead-acid |
| 0x00A7 | ExternalInv | 1 | 0/1 | ★ | 0:Enable 1:Disable |
| 0x00A8 | DischCutOffVoltage_GridMode | 0.1V | DischargeCutVoltage..8000 | ★ | Lead-acid |
| 0x00A9 | DRMFunctionEnable | 1 | 0/1 | ★ | |
| 0x00AA | Meter/CT_Select | 1 | 0/1 | ★ | 0:Meter 1:CT |
| 0x00AB | FVRT_Function | 1 | 0/1 | ★ | |
| 0x00AC | FVRT_VacUpper | 1V | X1: 230–288 / X3: 230–276 | ★ | |
| 0x00AD | FVRT_VacLower | 1V | X1: 46–240 / X3: 30–230 | ★ | |
| 0x00C9 | ModBusRTU_Address | 1 | — | ★ | |
| 0x00CA | ModBusRTU_BaudRate | bit/s | 0–6 | ★ | See baud rate table |

#### Battery Heating Write

| Register | Variable | Unit | EE |
|---|---|---|---|
| 0x00CF | BatteryHeatingEn | 1 | ★ |
| 0x00D0 | HeatingPeriod1_StartTime (Hi:Hour Lo:Minute) | — | ★ |
| 0x00D1 | HeatingPeriod1_EndTime | — | ★ |
| 0x00D2 | HeatingPeriod2_StartTime | — | ★ |
| 0x00D3 | HeatingPeriod2_EndTime | — | ★ |

#### Dry Contact Write

| Register | Variable | Range | EE | Description |
|---|---|---|---|---|
| 0x00B4 | LeaseModeEnable | 0/1 | ★ | |
| 0x00B5 | DeviceLockFlag | 0/1 | ★ | 0:UnLock 1:Lock |
| 0x00B6 | ManualModeControl | 0/1 | ★ | 0:OFF 1:ON |
| 0x00B7 | FeedinOnPower | 0–8000 | ★ | 1W |
| 0x00B8 | SwitchOnSoc | 0–100 | ★ | 1% |
| 0x00B9 | ConsumeOffPower | 0–8000 | ★ | 1W |
| 0x00BA | SwitchOffSoc | 0–100 | ★ | 1% |
| 0x00BB | MinimumPerOnSignal | 5–100 | ★ | 1min |
| 0x00BC | MaximumPerDayOn | 5–1200 | ★ | 1min |
| 0x00BD | ScheduleEnable | 0/1 | ★ | |
| 0x00BE | WorkStartTime1 | Hi:Hour Lo:Minute | ★ | |
| 0x00BF | WorkEndTime1 | Hi:Hour Lo:Minute | ★ | |
| 0x00C0 | WorkStartTime2 | Hi:Hour Lo:Minute | ★ | |
| 0x00C1 | WorkEndTime2 | Hi:Hour Lo:Minute | ★ | |
| 0x00C2 | LoadManagementWorkMode | 0–2 | ★ | 0:Disable 1:Manual 2:SmartSave |
| 0x00C3 | DryContactMode | 0/1 | ★ | 0:LoadMgmt 1:GenControl |
| 0x00C4 | SelfuseModeBackupEn | 0/1 | ★ | |
| 0x00C5 | SelfUse_BackupSoc | 10–100 | ★ | 1% |
| 0x00C6 | ParallelSetting | 0/1 | ★ | 0:Free 1:Master |
| 0x00C7 | ExternalGenEn | 0–2 | ★ | 0:Disable 1:ATS 2:DryContact |
| 0x00C8 | ExternalGenMaxCharge | — | ★ | 1W(X1)/10W(X3) |

#### Export Limit Write

| Register | Variable | Range | EE | Description |
|---|---|---|---|---|
| 0x00D4 | ExportSoftLimitEn | 0/1 | ★ | |
| 0x00D5 | ExportHardLimitEn | 0/1 | ★ | |
| 0x00D6 | HardExportPower | 0–15000 | ★ | 1W(X1)/10W(X3) |
| 0x00D7 | GeneralSoftLimitEn | 0/1 | ★ | |
| 0x00D8 | GeneralHardLimitEn | 0/1 | ★ | |
| 0x00D9 | SoftAcPowerLimit | 0–15000 | ★ | 1VA(X1)/10VA(X3) |
| 0x00DA | HardAcPowerLimit | 0–15000 | ★ | 1VA(X1)/10VA(X3) |

#### Battery / EV Settings Write

| Register | Variable | Range | EE |
|---|---|---|---|
| 0x00E0 | BatteryChargeMaxSoc | 10–100 (1%) | ★ |
| 0x00E1 | bBatterToEVCharge | 0/1 | ★ |

#### Generator Control Write

| Register | Variable | Unit | Range | EE | Description |
|---|---|---|---|---|---|
| 0x00E3 | StartGenMethod | 1 | 0/1 | ★ | 0:reference SoC 1:immediately |
| 0x00E4 | SwitchOnSoC | 1% | — | ★ | reference SoC |
| 0x00E5 | SwitchOffSoC | 1% | — | ★ | reference SoC |
| 0x00E6 | MaxRunTime | 1min | 1–60000 | ★ | |
| 0x00E7 | MinRestTime | 1min | 1–60000 | ★ | |
| 0x00E8 | AllowWorkStartTime | — | Hi:Hour Lo:Minute | ★ | |
| 0x00E9 | AllowWorkStopTime | — | Hi:Hour Lo:Minute | ★ | |

#### Peak Shaving Write

| Register | Variable | Unit | Range (X1 / X3) | EE |
|---|---|---|---|---|
| 0x00EA | PeakShavingDischarPeriod.bP1_StartTime (Hi:Hour Lo:Min) | — | 0–23 / 0–59 | ★ |
| 0x00EB | PeakShavingDischarPeriod.bP1_StopTime | — | 0–23 / 0–59 | ★ |
| 0x00EC | PeakShavingDischarPeriod.bP2_StartTime | — | 0–23 / 0–59 | ★ |
| 0x00ED | PeakShavingDischarPeriod.bP2_StopTime | — | 0–23 / 0–59 | ★ |
| 0x00EE | PeakShaving.PeriodBPeakLimits1 | 1W(X1)/10W(X3) | X1: 0–60000 / X3: 0–3000 | ★ |
| 0x00EF | PeakShaving.PeriodDPeakLimits2 | 1W(X1)/10W(X3) | X1: 0–60000 / X3: 0–3000 | ★ |
| 0x00F0 | PeakShaving.PeriodAChargeFromGridEn | 1 | 0/1 | ★ |
| 0x00F1 | PeakShaving.PeriodAChargePowerLimits | 1W | X1: 0–7500 / X3: 0–15000 | ★ |
| 0x00F2 | PeakShaving.PeriodAMax_SOC | 1% | 10–100 | ★ |
| 0x00F3 | PeakShaving.PeriodCReserved_SOC | 1% | 10–100 | ★ |

#### VPP / EVC / AdaptBox Write

| Register | Variable | Range | EE |
|---|---|---|---|
| 0x00F4 | VPPExitIdleEn | 0/1 | ★ |
| 0x00F5 | FastCtCheckEn | 0/1 | ★ |
| 0x00F9 | EVChargerAddr | 0–255 | ★ |
| 0x00FB | AdaptBoxG2Addr | 0–255 | ★ |
| 0x00FD | CTFalutEn | 0/1 | ★ |
| 0x00FE | SuperBuckUpEn | 0/1 | ★ |
| 0x00FF | SmartScheduleWorkMode | 0–2 | ★ |

`SmartScheduleWorkMode`: 0:SelfUse 1:Feedin Priority 2:Battery not discharge.

#### Generator Schedule Write (0x0100–0x010A)

| Register | Variable | Description | EE |
|---|---|---|---|
| 0x0100 | GenCharge_StartTime | Hi:Hour Lo:Minute | ★ |
| 0x0101 | GenCharge_EndTime | Hi:Hour Lo:Minute | ★ |
| 0x0102 | GenDischarge_StartTime | Hi:Hour Lo:Minute | ★ |
| 0x0103 | GenDischarge_EndTime | Hi:Hour Lo:Minute | ★ |
| 0x0104 | GenP2_SetEnable | 0/1 | ★ |
| 0x0105 | GenP2Charge_StartTime | Hi:Hour Lo:Minute | ★ |
| 0x0106 | GenP2Charge_EndTime | Hi:Hour Lo:Minute | ★ |
| 0x0107 | GenP2Discharge_StartTime | Hi:Hour Lo:Minute | ★ |
| 0x0108 | GenP2Discharge_EndTime | Hi:Hour Lo:Minute | ★ |
| 0x0109 | ChargeFromGenEnable | 0/1 | ★ |
| 0x010A | ChargeFromGen_ChargeSoC | 10–100 (1%) | ★ |
| 0x010B | GenMinPower | 0–60000 (1W) | ★ |

#### EPS / Misc Write

| Register | Variable | Range | EE |
|---|---|---|---|
| 0x010C | FastInEPS | 0/1 |  |
| 0x010D | CTCutDownINVEn | 0/1 |  |
| 0x0113 | bShotoffEn | 0/1 |  |

#### Password Write

| Register | Variable | Range | EE |
|---|---|---|---|
| 0x0039 | UserPassword | 0000–9999 | ★ |
| 0x003A | AdvancedPassword | 0000–9999 | ★ |

---

## Function Code 0x10: Write Multiple Registers

Error code: `0x90`. Register count limited to `0x0001–0x007B`.

**Example:** Write RTC time (0x0000–0x0005)
- Request: `01 10 00 00 00 06 0C 00 38 00 15 00 0E 00 0C 00 01 00 15 42 E9`
- Response: `01 10 00 00 00 06 40 0B`

### RTC

| Register | Variable | Unit | Range |
|---|---|---|---|
| 0x0000 | RTC-Seconds | 1s | 0–59 |
| 0x0001 | RTC-Minutes | 1min | 0–59 |
| 0x0002 | RTC-Hours | — | 0–23 |
| 0x0003 | RTC-Days | — | 1–31 |
| 0x0004 | RTC-Months | — | 1–12 |
| 0x0005 | RTC-Years | — | 0–99 |

### Charge / Discharge Period 1 (Hi:Hour Lo:Minute)

| Register | Variable |
|---|---|
| 0x001B | NightCharge_P1_StartTime |
| 0x001C | NightCharge_P1_EndTime |
| 0x001D | DisCharge_P1_StartTime |
| 0x001E | DisCharge_P1_EndTime |

### Remote Power Control (V3.33)

| Register | Variable | Unit | Format | Description |
|---|---|---|---|---|
| 0x007C | ModbusPowerControl | 1 | uint16 | 0:disable 1:powerCtrl 2:electricQty 3:SOCTarget 4:PushPower±Mode 5:PushPowerZeroMode 6:SelfConsume Charge-Discharge 7:SelfConsume ChargeOnly |
| 0x007D | TargetSetType | 1 | uint16 | 1:set 2:update |
| 0x007E–0x007F | RemoteControl_ActivePower | 1W | int32 | LSB at 0x007E. Positive=charge, Negative=discharge |
| 0x0080–0x0081 | RemoteControl_ReactivePower | 1Var | int32 | LSB at 0x0080. Positive=Inductive, Negative=Capacitive |
| 0x0082 | Time_of_Duration | 1s | uint16 | Power-control mode duration |
| 0x0083 | TargetSoc | 1% | uint16 | |
| 0x0084–0x0085 | TargetEnergy | 1Wh | uint32 | LSB at 0x0084 |
| 0x0086–0x0087 | Charge_Discharg_Power | 1W | int32 | Positive=charge, Negative=discharge |
| 0x0088 | RemoteCtrlTimeOut | 1s | uint16 | |
| 0x0089–0x008A | PushModePower | 1W | int32 | LSB at 0x0089. Positive=discharge, Negative=charge |

### Data Hub Write Multiple (internal use)

| Register | Variable | Description |
|---|---|---|
| 0xF000–0xF013 | WriteSetValue | Write the value of the setting item (★ EE save) |

### EV Charger Write Multiple

Refer to companion document **`(Solax)EVC ModbusRTU V3.3`**.

---

## Firmware Upgrade Registers (0x03 / 0x10)

| Register | Variable | W/R | Description |
|---|---|---|---|
| 0x3000–0x3001 | BootloaderVersion | R | uint16 ×2 |
| 0x3002 | IAP_Protocol | WR | bit0:data transfer; bit1:high-power upgrade |
| 0x3003 | UpgradeModule | WR | 0:Rev 1:ARM 2:MDSP 3:SDSP 4:ARC 5:ARM+DSP 6:BMS_M 7:BMS_S 10:EVCharger |
| 0x3004 | UpgradeTimeOut | WR | 1s |
| 0x3005–0x3006 | UpgradeKey | WR | uint16 ×2 |
| 0x3007–0x3008 | UpgradeSeed | R | uint16 ×2 |
| 0x3010 | UpgradeMachineType | WR | |
| 0x3011–0x3012 | FileCheckSum | WR | uint16 ×2 |
| 0x3013 | DownLoadBlockNum | WR | data-mode:1; high-power:DownLoadBlockNum |
| 0x3014–0x3015 | EraseStartAddr | WR | uint16 ×2 |
| 0x3016–0x3017 | EraseLength | WR | uint16 ×2 |
| 0x3018–0x3019 | BlockStartAddr | WR | uint16 ×2 |
| 0x301A–0x301B | BlockLength | WR | uint16 ×2 |
| 0x301C | CurrentBlockNum | WR | |
| 0x301D–0x301E | BlockCheckSum | WR | uint16 ×2 |
| 0x301F | UpgradeDataPackageNum | WR | |
| 0x3020–0x3097 | UpgradeData | WR | uint16 ×120 |
| 0x3098 | BlockCheckResult | R | |
| 0x3099 | McuDownLoadCheckResult | R | |
| 0x30A4 | ToBeDownloadMcuInfor | R | |
| 0x30A5 | DownloadedMcuInfor | R | |
| 0x30A6 | UpgradeMcuInfor | R | |
| 0x30A7 | IapState | R | 0x0000:AppCommonRunStatus 0x0001:AppResumeWaitStatus 0x0002:EraseProgramStatus 0x0003:ProgramDownloadStatus 0x0004:UpgradeSuccessStatus 0x0005:UpgradeFailStatus 0x8000:bootloaderCommonRunStatus 0x8001:BootloaderResumeWaitStatus |
| 0x30A8 | DownloadedBlockNum | R | |
| 0x30A9 | DownloadedPackageNum | R | |
| 0x30AA–0x30C2 | File_Name | WR | uint16 ×25 |

Notes from PDF (V3.34):
- Suggested baud for upgrade: 19200 or 38400
- Step 2 erases flash — allow ~10 s response timeout
- Modbus CRC16 used for file verification
- 0x10 used for write, 0x03 used to query progress
- For X1G4 / X3G4 the supported `UpgradeModule` values are 1, 2, 5, 6, 7

---

## Enumerations

### Safety Codes

#### X3 Safety Codes

| Code | Standard |
|---|---|
| 0 | VDE0126 |
| 1 | VDE4105 |
| 2 | AS 4777_2020_A |
| 3 | G98/1 |
| 4 | C10/11 |
| 5 | TOR |
| 6 | EN50438_NL |
| 7 | Denmark2019_W |
| 8 | CEB |
| 9 | CEI021 |
| 10 | NRS097_2_1 |
| 11 | VDE0126_Gr_Is |
| 12 | UTE_C15_712 |
| 13 | IEC61727 |
| 14 | G99/1 |
| 15 | VDE0126_Gr_Co |
| 16 | Guyana |
| 17 | C15_712_is_50 |
| 18 | C15_712_is_60 |
| 19 | New Zealand |
| 20 | RD1699 |
| 21 | Chile |
| 22 | Israel |
| 23 | Czech_PPDS_2020 |
| 24 | RD1699_Island |
| 25 | EN50549_Poland |
| 26 | EN50438_Portugal |
| 27 | PEA |
| 28 | MEA |
| 29 | EN50549_Sweden |
| 30 | Philippines |
| 31 | EN50438_Slovenia |
| 32 | Denmark2019_E |
| 33 | EN50549_EU |
| 34 | AS 4777_2020_B |
| 35 | AS 4777_2020_C |
| 36 | User-Defined |
| 37 | EN50549_Romania |
| 38 | CEI016 |
| 39 | ACEA |
| 40 | Chile2021 MT_R |
| 41 | Chile2021 MT_U |
| 42 | Czech_2022_2 (write: Czech_2021_2) |
| 43 | G98/NI-1 |
| 44 | G99/NI-1 |
| 45 | G99/NI_Type B |
| 46 | CQC |
| 47 | LA_3P_380 |
| 48 | LA_3P_220 |

#### X1 Safety Codes

| Code | Standard |
|---|---|
| 0–21 | Same as X3 |
| 22 | EN50438_Ireland |
| 23 | Philippines |
| 24 | Czech_PPDS_2020 |
| 25 | Czech_50438 |
| 26 | EN50549_EU |
| 27 | Denmark2019_E |
| 28 | RD1699_Island |
| 29 | EN50549_Poland |
| 30 | MEA_Thailand |
| 31 | PEA_Thailand |
| 32 | ACEA |
| 33 | AS 4777_2020_B |
| 34 | AS 4777_2020_C |
| 35 | User Define |
| 36 | EN50549_Romania |
| 37 | G98/NI-1 |
| 38 | G99/NI-1 |
| 39 | Chile2021 MT_R |
| 40 | Chile2021 MT_U |
| 41 | Slovenia |

### Run Modes

| Code | Description |
|---|---|
| 0 | Waiting |
| 1 | Checking |
| 2 | Normal |
| 3 | Fault |
| 4 | Permanent Fault |
| 5 | Update |
| 6 | Off-grid waiting |
| 7 | Off-grid |
| 8 | Self Testing |
| 9 | Idle |
| 10 | Standby |

### Inverter Error Codes (X3) — 32-bit bitmask

| Bit | Fault |
|---|---|
| 0 | TZ Protect Fault |
| 1 | Grid Lost Fault |
| 2 | Grid Volt Fault |
| 3 | Grid Freq Fault |
| 4 | PV Volt Fault |
| 5 | Bus Volt Fault |
| 6 | Bat Volt Fault |
| 7 | AC10mins Volt Fault |
| 8 | DCI OCP Fault |
| 9 | DCV OCP Fault |
| 10 | SW OCP Fault |
| 11 | RC OCP Fault |
| 12 | Isolation Fault |
| 13 | Temp Over Fault |
| 14 | BatConnDir Fault |
| 15 | Off-grid Overload |
| 16 | Overload |
| 17 | Bat Power Low |
| 18 | BMS Lost |
| 19 | Fan Fault |
| 20 | Low Temp Fault |
| 21 | Parallel Fault |
| 22 | Hard Limit Fault |
| 23 | INV Volt Sample Fault |
| 24 | Inner Comm Fault |
| 25 | INV EEPROM Fault |
| 26 | RCD Fault |
| 27 | Grid Relay Fault |
| 28 | Off-grid Relay Fault |
| 29 | PV ConnDir Fault |
| 30 | Charger Relay Fault |
| 31 | Earth Relay Fault |

### Inverter Error Codes (X1) — 32-bit bitmask

| Bit | Fault |
|---|---|
| 0 | TZ Protect Fault |
| 1 | Grid Lost Fault |
| 2 | Grid Volt Fault |
| 3 | Grid Freq Fault |
| 4 | PV Volt Fault |
| 5 | Bus Volt Fault |
| 6 | Bat Volt Fault |
| 7 | AC10mins Volt Fault |
| 8 | DCI OCP Fault |
| 9 | Reserve |
| 10 | SW OCP Fault |
| 11 | RC OCP Fault |
| 12 | Isolation Fault |
| 13 | Temp Over Fault |
| 14 | BatConnDir Fault |
| 15 | Missed CT Fault |
| 16 | Off-grid Overload Fault |
| 17 | Overload Fault |
| 18 | PV ConnDir Fault |
| 19 | Bat Power Low |
| 20 | Low Temp Fault |
| 21 | Parallel Fault |
| 22 | Charger Relay Fault |
| 23 | BMS Lost |
| 24 | Inner Comm Fault |
| 25 | Fan Fault |
| 26 | Earth Relay Fault |
| 27 | INV EEPROM Fault |
| 28 | RCD Fault |
| 29 | Off-grid Relay Fault |
| 30 | Grid Relay Fault |
| 31 | Other Device Fault |

### Manager Error Codes — 16-bit bitmask

| Bit | Fault |
|---|---|
| 0 | Power Type Fault |
| 1 | Port OC Warning |
| 2 | Mgr EEPROM Fault |
| 3 | Reserve |
| 4 | NTC Sample Invalid |
| 5 | Bat Temp Low |
| 6 | Bat Temp High |
| 7 | Reserve |
| 8 | Reserve |
| 9 | Meter Fault |
| 10 | Bypass Relay Fault |
| 11 | Fan 2 Fault |
| 12–15 | Reserve |

### BMS Warning Codes — 32-bit bitmask

| Bit | Fault |
|---|---|
| 0 | BMS_External_Err |
| 1 | BMS_Internal_Err |
| 2 | BMS_OverVoltage |
| 3 | BMS_LowerVoltage |
| 4 | BMS_ChargeOCP |
| 5 | BMS_DischargeOCP |
| 6 | BMS_TemHigh |
| 7 | BMS_TemLow |
| 8 | BMS_CellImbalance |
| 9 | BMS_Hardware_Protect |
| 10 | BMS_Circuit_Fault |
| 11 | BMS_ISO_Fault |
| 12 | BMS_VolSen_Fault |
| 13 | BMS_TempSen_Fault |
| 14 | BMS_CurSen_Fault |
| 15 | BMS_Relay_Fault |
| 16 | BMS_Type_Unmatch |
| 17 | BMS_Ver_Unmatch |
| 18 | BMS_MFR_Unmatch |
| 19 | BMS_SW_Unmatch |
| 20 | BMS_M&S_Unmatch |
| 21 | BMS_CR_NORespond |
| 22 | BMS_SW_Protect |
| 23 | BMS_536_Fault |
| 24 | BMS_SelfcheckErr |
| 25 | BMS_TempdiffErr |
| 26 | MS_BreakFault |
| 27 | BMS_Flash_Fault |
| 28 | BMS_Precharge_Fault |
| 29 | BMS_AirSwitch_Break |
| 30–31 | Reserve |
