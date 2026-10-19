# Jablotron FUTURA Modbus TCP Specification V5.0

This MD file was generated from `FU_TCP_TXT_EN.pdf` (document version 5.0, 13.10.2025), the Modbus TCP register map
of the Jablotron Futura heat recovery ventilation unit. A copy of the PDF is published at
[taphome.com](https://taphome.com/compatibility/devices/jablotron-futura/manufacturer/FU_TCP_TXT_EN.pdf).

Our unit is a **Futura L/M/S** — use the `L/M/S` address column. The `L+` column is kept for completeness; L+ has a
condensed register map and lacks many of the registers.

## Connection parameters

- Device address (unit ID): `1`
- TCP port: `502`
- Number of available sockets: **`1`** — only one client can be connected at a time, so every reader and writer
  must share one connection.
- Compatible from firmware Futura L/M/S `FUA.P.06.71`, Futura L+ `FLA.P.01.06`

> **Data integrity:** composite registers (`uint32`, `int32`, `float32`, arrays) must be read in one operation with
> function 4 (Read Input Registers) or 3 (Read Holding Registers) and written with function 16 (Write Multiple
> Holding Registers). Reading or writing only a part of such a register returns `ILLEGAL_DATA_ADDRESS`.

## Input registers (function 4, read-only)

| L/M/S | L+ | Name | Description | Type | Units |
|---|---|---|---|---|---|
| 0 | | `fact_device_id` | Product identification, Futura always 39 | uint16 | |
| 1–2 | | `fact_serial_number` | Serial number of the product (globally unique ID) | uint32 | |
| 3–5 | 2–4 | `fact_ethernet_mac` | MAC address | uint16[3] | |
| 6–7 | | `fact_hw_revision` | Hardware revision of the device | uint32 | |
| 8–9 | 0–1 | `firm_revision` | Firmware revision of the current application image | uint32 | |
| 10–11 | | `sys_build_number` | Firmware build number (obsolete) | uint32 | |
| 12–13 | | `sys_regmap_version` | Version of the register map | uint32 | |
| 14 | | `sys_options` | Device model (0–1 = Futura L, 2 = Futura M) | uint16 | |
| 15 | | `fut_config` | Available features, see [fut_config](#fut_config) | uint16 | |
| 16–17 | 6–7 | `fut_mode` | Operation modes, see [fut_mode](#fut_mode) | uint32 | |
| 18–19 | 8–9 | `fut_error` | Errors, see [fut_error](#fut_error) | uint32 | |
| 20–21 | 10–11 | `fut_warning` | Warnings, see [fut_warning](#fut_warning) | uint32 | |
| 30 | 12 | `fut_temp_ambient` | Temperature of the ambient (outdoor intake) air flow | int16 | 0.1 °C |
| 31 | 13 | `fut_temp_fresh` | Temperature of the fresh (supply) air flow | int16 | 0.1 °C |
| 32 | 14 | `fut_temp_indoor` | Temperature of the indoor (extract) air flow | int16 | 0.1 °C |
| 33 | 15 | `fut_temp_waste` | Temperature of the waste (exhaust) air flow | int16 | 0.1 °C |
| 34 | 16 | `fut_humi_ambient` | Relative humidity of the ambient air flow | int16 | 0.1 % |
| 35 | 17 | `fut_humi_fresh` | Relative humidity of the fresh air flow | int16 | 0.1 % |
| 36 | 18 | `fut_humi_indoor` | Relative humidity of the indoor air flow | int16 | 0.1 % |
| 37 | 19 | `fut_humi_waste` | Relative humidity of the waste air flow | int16 | 0.1 % |
| 38 | 20 | `fut_t_out` | Temperature of the outdoor NTC sensor (−99 = disconnected) | int16 | 0.1 °C |
| 40 | 21 | `fut_filter_wear_level` | Level of filter wearing | uint16 | % |
| 41 | 22 | `fut_power_consumption` | Accumulated power consumption | uint16 | W |
| 42 | 23 | `fut_heat_recovering` | Heat recovering | uint16 | W |
| 43 | 24 | `fut_heating_power` | Internal heating power | uint16 | W |
| 44 | 25 | `fut_air_flow` | Sum of supply and exhaust air flow | uint16 | m³/h |
| 45 | | `fut_fan_pwm_supply` | Supply fan power | uint16 | % |
| 46 | | `fut_fan_pwm_exhaust` | Exhaust fan power | uint16 | % |
| 47 | | `fut_fan_rpm_supply` | Supply fan RPM | uint16 | rpm |
| 48 | | `fut_fan_rpm_exhaust` | Exhaust fan RPM | uint16 | rpm |
| 49 | | `fut_uint1_voltage` | Universal input 1 voltage | uint16 | mV |
| 50 | | `fut_uint2_voltage` | Universal input 2 voltage | uint16 | mV |
| 51 | | `fut_dig_inputs` | Digital inputs: bit0 user button, bit1 factory button, bit2 boost input, bit3 uint1, bit4 uint2 | uint16 | |
| 52 | | `sys_battery_voltage` | Backup battery voltage | uint16 | 0.001 mV (as printed; likely mV) |
| | 26 | `fut_heat_cool_status` | Heating and cooling status, see [fut_heat_cool_status](#fut_heat_cool_status) | uint16 | |
| 60–61 | | `mbdev_stat_reads` | Modbus read count statistics | uint32 | |
| 62–63 | | `mbdev_stat_writes` | Modbus write count statistics | uint32 | |
| 64–65 | | `mbdev_stat_fails` | Modbus fail count statistics | uint32 | |
| 66 | | `mbdev_connected_mk_ui` | Connected wall mounted controllers, bits 0–2 | uint16 | |
| 67–68 | | `mbdev_connected_mk_sens` | Connected wall mounted sensors, bits 0–7 | uint32 | |
| 69 | | `mbdev_connected_coolbreeze` | Connected CoolBreeze | uint16 | 0/1 |
| 70–71 | | `mbdev_connected_valve_supply` | Connected valves (supply), bits 0–31 | uint32 | |
| 72–73 | | `mbdev_connected_valve_exhaust` | Connected valves (exhaust), bits 0–31 | uint32 | |
| 74 | | `mbdev_connected_button` | Connected buttons, bits 0–15 | uint16 | |
| 75 | | `mbdev_connected_alfa` | Connected UI ALFA, bits 0–15 | uint16 | |
| 80 | | `vzv_identify` | Supply and exhaust zones identification, see [vzv_identify](#vzv_identify) | uint16 | |

### Peripherals (input registers)

Repeated blocks, one per device; `[1 – n]` is the device index.

| L/M/S | L+ | Name | Description | Type | Units |
|---|---|---|---|---|---|
| 100, 105, 110 | | `ui_mb_address` | Wall mounted UI [1–3]: Modbus address, 0 = not connected | uint16 | |
| 101, 106, 111 | | `ui_options` | Wall mounted UI [1–3]: options, bit0 disable CO₂ eval., bit1 disable temperature eval., bit2 disable RH eval. | uint16 | |
| 102, 107, 112 | | `ui_co2` | Wall mounted UI [1–3]: measured CO₂ | uint16 | ppm |
| 103, 108, 113 | | `ui_temp` | Wall mounted UI [1–3]: measured temperature | int16 | 0.1 °C |
| 104, 109, 114 | | `ui_humi` | Wall mounted UI [1–3]: measured relative humidity | uint16 | 0.1 % |
| 115, 120, …, 150 | | `sens_mb_address` | Wall mounted sensor [1–8]: Modbus address, 0 = not connected | uint16 | |
| 116, 121, …, 151 | | `sens_options` | Wall mounted sensor [1–8]: options, same bits as `ui_options` | uint16 | |
| 117, 122, …, 152 | | `sens_co2` | Wall mounted sensor [1–8]: measured CO₂ | uint16 | ppm |
| 118, 123, …, 153 | | `sens_temp` | Wall mounted sensor [1–8]: measured temperature | int16 | 0.1 °C |
| 119, 124, …, 154 | | `sens_humi` | Wall mounted sensor [1–8]: measured relative humidity | uint16 | 0.1 % |
| 160, 170, …, 230 | | `alfa_mb_address` | ALFA [1–8]: Modbus address, 0 = not connected | uint16 | |
| 161, 171, …, 231 | | `alfa_options` | ALFA [1–8]: options, bit0 disable CO₂ eval. | uint16 | |
| 162, 172, …, 232 | | `alfa_co2` | ALFA [1–8]: measured CO₂ | uint16 | ppm |
| 163, 173, …, 233 | | `alfa_temp` | ALFA [1–8]: measured temperature | int16 | 0.1 °C |
| 164, 174, …, 234 | | `alfa_humi` | ALFA [1–8]: measured relative humidity | uint16 | 0.1 % |
| 165, 175, …, 235 | | `alfa_ntc_temp` | ALFA [1–8]: measured NTC sensor temperature | uint16 | 0.1 °C |
| | 100, 110, …, 170 | `zone_co2` | Zone [1–8]: measured CO₂ | uint16 | ppm |
| | 101, 111, …, 171 | `zone_temp` | Zone [1–8]: measured temperature | int16 | 0.1 °C |
| | 102, 112, …, 172 | `zone_humi` | Zone [1–8]: measured relative humidity | uint16 | 0.1 % |
| | 103, 113, …, 173 | `zone_ntc_temp` | Zone [1–8]: measured NTC sensor temperature | uint16 | 0.1 °C |

## Holding registers (function 3 read, function 16 write)

| L/M/S | L+ | Name | Description | Type | Units |
|---|---|---|---|---|---|
| 0 | 0 | `func_ventilation` | Current ventilation mode: 0 = Off, 1–5 = manual level, 6 = Auto | uint16 | |
| 1 | 1 | `func_boost_tm` | Boost function — read: remaining time, write: duration 0–7200 s | uint16 | s |
| 2 | 2 | `func_circulation_tm` | Circulation function — read: remaining time, write: duration 0–21600 s | uint16 | s |
| 3 | 3 | `func_overpressure_tm` | Overpressure function — read: remaining time, write: duration 0–7200 s | uint16 | s |
| 4 | | `func_night_tm` | Night function — read: remaining time, write: duration 0–28800 s | uint16 | s |
| 5 | | `func_party_tm` | Party function — read: remaining time, write: duration 0–28800 s | uint16 | s |
| 6–7 | | `func_away_begin` | Away — begin, unix timestamp in local time, 0 = off | uint32 | |
| 8–9 | | `func_away_end` | Away — end, unix timestamp in local time, 0 = off | uint32 | |
| 10 | 4 | `cfg_temp_set` | Preferred temperature | uint16 | 0.1 °C |
| 11 | | `cfg_humi_set` | Preferred humidity (dry = 25, comfort = 50, humid = 75 %) | uint16 | 0.1 % (the unit reports 500 for comfort) |
| 12 | 5 | `func_time_prog` | Time program enable | uint16 | 0/1 |
| 13 | 6 | `func_antiradon` | Antiradon enable | uint16 | 0/1 |
| 14 | 7 | `cfg_bypass_enable` | Automatic bypass enable | uint16 | 0/1 |
| 15 | 8 | `cfg_heating_enable` | Heating enable | uint16 | 0/1 |
| 16 | 9 | `cfg_cooling_enable` | Cooling enable | uint16 | 0/1 |
| 17 | | `cfg_comfort_enable` | Comfort heating enable (only if heating is enabled) | uint16 | 0/1 |
| 18 | 10 | `cfg_auto_circulation_enable` | Automatic circulation enable | uint16 | 0/1 |
| 20 | | `vzv_cb_priority_control` | Zone auto control when CoolBreeze is on (0 = temperature, 1 = CO₂) | uint16 | 0/1 |
| 21 | | `vzv_kitchehood_normally_open` | Kitchen hood is normally (0 = closed, 1 = opened) | uint16 | 0/1 |
| 22 | | `vzv_boost_volume_per_run` | Zone boost air volume increment per run: 50–150 m³/h (default 50) | uint16 | m³/h |
| 23 | | `vzv_kitchenhood_volume_per_run` | Kitchen hood air volume increment per run: 50–150 m³/h (default 120) | uint16 | m³/h |
| 900 | 900 | `access_code` | Code to get access to service registers | uint16 | |
| 920 | 920 | `user_password` | User's password, can be changed only when logged in. Default 65535 (password disabled) | uint16 | |
| 922 | 922 | `password_timeout` | Login timeout, can be changed only when logged in. Default 300 s | uint16 | s |

### Peripherals (holding registers)

| L/M/S | L+ | Name | Description | Type | Units |
|---|---|---|---|---|---|
| 100, 105, 110 | | `ui_temp_corr` | Wall mounted UI [1–3]: temperature sensor correction | int16 | 0.1 °C |
| 115, 120, …, 150 | | `se_temp_corr` | Wall mounted sensor [1–8]: temperature sensor correction | int16 | 0.1 °C |
| 160, 165, …, 195 | | `alfa_temp_corr` | ALFA [1–8]: temperature sensor correction | int16 | 0.1 °C |
| 162, 167, …, 197 | | `alfa_ntc_temp_corr` | ALFA [1–8]: NTC temperature sensor correction | int16 | 0.1 °C |
| 300, 310, …, 370 | 100, 110, …, 170 | `ext_sens_present` | External sensor [1–8] present | uint16 | 0/1 |
| 301, 311, …, 371 | 101, 111, …, 171 | `ext_sens_invalidate` | External sensor [1–8] invalidate, see [ext_sens_invalid](#ext_sens_invalid) | uint16 | |
| 302, 312, …, 372 | 102, 112, …, 172 | `ext_sens_t` | External sensor [1–8] temperature value: −20 – 100 °C | int16 | 0.1 °C |
| 303, 313, …, 373 | 103, 113, …, 173 | `ext_sens_rh` | External sensor [1–8] humidity value: 0 – 100 % | uint16 | % |
| 304, 314, …, 374 | 104, 114, …, 174 | `ext_sens_co2` | External sensor [1–8] CO₂ value: 0 – 10000 ppm | uint16 | ppm |
| 305, 315, …, 375 | 105, 115, …, 175 | `ext_sens_t_floor` | External sensor [1–8] floor temperature value: −20 – 100 °C | int16 | 0.1 °C |
| 400, 410, …, 470 | 200, 210, …, 270 | `ext_btn_present` | External button [1–8] present | uint16 | 0/1 |
| 401, 411, …, 471 | 201, 211, …, 271 | `ext_btn_mode` | External button [1–8] mode: 0 = boost, 1 = kitchen hood | uint16 | 0/1 |
| 402, 412, …, 472 | 202, 212, …, 272 | `ext_btn_tm` | External button [1–8] countdown timer 0–10800 s | uint16 | s |
| 403, 413, …, 473 | 203, 213, …, 273 | `ext_btn_active` | External button [1–8] activation (limited to 3 hours) | uint16 | 0/1 |

## Bit meanings

### fut_mode

| Bit | Meaning |
|---|---|
| 0 | Boost activated |
| 1 | Circulation activated |
| 2 | Time program activated |
| 3 | Overpressure activated |
| 4 | Away activated |
| 5 | Party activated |
| 6 | Night activated |
| 7 | Antiradon activated |
| 8 | Device is on (always 1) |
| 9 | Filter check activated |
| 10 | Drying activated |
| 11 | Bypass is opened |
| 12 | Too low temperature |
| 13 | Shutdown due to an error |
| 14 | Device is going to be started |
| 15 | Service mode activated |
| 16 | Waste temperature under −3 °C (anti-icing protection condition is met) |
| 17 | Anti-icing protection is active |
| 18 | Emergency shutdown |
| 19 | Duct system analysis |
| 20 | Stand-by |
| 21 | Zone boost |
| 22 | Zone analysis |
| 23 | Exchanger defrost |

### fut_error

| Bit | Meaning |
|---|---|
| 0 | Ambient sensor error |
| 1 | Indoor sensor error |
| 2 | Fresh sensor error |
| 3 | Waste sensor error |
| 4 | Supply fan error |
| 5 | Exhaust fan error |
| 6 | Exchanger communication error |
| 7 | Exchanger position error |
| 8 | IO board error |
| 9 | Supply fan blocked |
| 10 | Exhaust fan blocked |
| 11 | CoolBreeze communication error |
| 12 | CoolBreeze outdoor unit error |
| 13 | Declared airflow not reached on supply |
| 14 | Declared airflow not reached on exhaust |
| 15 | CoolBreeze detected, duct analysis required |
| 16 | VarioBreeze configuration changed, duct analysis required |
| 17 | VarioBreeze configuration changed, duct analysis required |
| 18 | Excessive humidity |
| 19 | CoolBreeze temperature sensor error |
| 20 | Fan out of control |
| 21 | Fresh temperature too low |

### fut_warning

| Bit | Meaning |
|---|---|
| 0 | Uninitialized filter |
| 1 | Too dusty filter |
| 2 | Too old filter |
| 3 | Low backup battery |
| 4 | Supply fan RPM too high |
| 5 | Exhaust fan RPM too high |
| 8 | Outdoor temperature is too low, ventilation function is limited |
| 9 | Incorrect configuration of a supply ventilation zone |
| 10 | Incorrect configuration of an exhaust ventilation zone |
| 11 | Emergency shutdown |
| 12 | SuperBreeze communication error |
| 13 | General SuperBreeze error |
| 14 | Power supply error |
| 15 | VarioBreeze valve error |
| 16 | Excessive humidity |
| 17 | Perform regular service maintenance |

### fut_config

| Bit | Meaning |
|---|---|
| 0 | Internal heating supported |
| 1 | CoolBreeze cooling available |
| 2 | CoolBreeze heating available |
| 3 | Bypass supported |
| 4 | VarioBreeze supported |
| 5 | Circulation supported |
| 6 | CoolBreeze supported |
| 7 | Exchanger control supported |

### vzv_identify

| Bit | Meaning |
|---|---|
| 0–7 | Identify supply zone 1–8 |
| 8–15 | Identify exhaust zone 1–8 |

### ext_sens_invalid

| Bit | Meaning |
|---|---|
| 0 | Invalid temperature value of the external sensor |
| 1 | Invalid humidity value of the external sensor |
| 2 | Invalid CO₂ value of the external sensor |
| 3 | Invalid floor temperature value of the external sensor |

### fut_heat_cool_status

| Bit | Meaning |
|---|---|
| 0 | Inner heating active |
| 1 | CoolBreeze heating active |
| 2 | CoolBreeze cooling active |

## Use in home-controller

`FuturaController` (`extensions/.../external/futura`) refreshes these ranges on every poll, one request per range:

- input `0–21, 30–38, 40–52, 60–75, 100–154, 160–165`
- holding `0–17`

Every register in these ranges is therefore already in memory after a refresh; exposing another one in `State` costs
no extra Modbus traffic.

| Register | Read (`State`) | Write (`FuturaController`) |
|---|---|---|
| HR 0 | `getVentilationSpeed()` | `setVentilationSpeed(int)` |
| HR 1–5 | `getRemainingSeconds(TimedFunction)` | `startTimedFunction(TimedFunction, int)` |
| HR 10 | `getPreferredTemperature()` | `setPreferredTemperature(double)`, 15–30 °C |
| HR 11 | `getPreferredHumidity()`, % | `setPreferredHumidity(int)`, 25 / 50 / 75 % |
| HR 12 | `getTimeProgramActive()` | `setTimeProgramActive(boolean)` |
| HR 14 | `getBypassEnabled()` | `setBypassEnabled(boolean)` |
| IR 16–21 | `getModeFlags()`, `getErrorFlags()`, `getWarningFlags()` | |
| IR 30–37 | air temperatures and humidities of the four flows | |
| IR 40–42 | `getFilterWearLevelPercent()`, `getPowerConsumption()`, `getHeatRecovering()` | |
| IR 44–48 | `getAirFlow()`, fan power and RPM | |
| IR 102–104 | wall controller (UI 1) CO₂, temperature, humidity | |

The REST action `/rest/futura/action` takes the same values as optional parameters (`vent`, `timeProg`, `tempSet`,
`humiSet`, `bypass`, and `boost`, `circulation`, `overpressure`, `night`, `party` in seconds) and
writes only the given ones. The dialog sends only the values that changed, so it does not overwrite a ventilation
level the time program has set meanwhile.

Notes:

- The unit accepts a single TCP connection, so writers use the monitor's controller (`FuturaMonitor.getController()`)
  rather than open their own.
- The spec does not state the word order of 32-bit registers. The unit sends the **low word first**: read high word
  first, `fut_mode` came out as `0x01040000` (bits 18 "emergency shutdown" and 24) instead of `0x00000104` (bit 2 time
  program, bit 8 "device is on", which is always 1). Bit 8 is the quick check of the order.
- `cfg_humi_set` (HR 11) is in 0.1 % as its unit says: the unit reports 500 for comfort, not 50. `State` and
  `FuturaController` convert, their API is in % (25 / 50 / 75).
- `fut_power_consumption` (IR 41) is described as "accumulated" but its unit is W; verify on the real unit whether
  it is an instantaneous power or a counter before treating it as energy.
- HR 18 `cfg_auto_circulation_enable` is rejected by this unit: reading holding `0–18` fails with
  `ILLEGAL_DATA_ADDRESS` and the whole refresh with it. Keep the holding range at `0–17`.
- Heating, cooling and antiradon registers are not used: the unit in this house has no heater.
