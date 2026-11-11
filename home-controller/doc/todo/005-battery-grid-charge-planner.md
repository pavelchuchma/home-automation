---
id: 005
title: Charge the battery from the grid to a target SoC by a deadline in the cheapest slots
type: enhancement
status: open
priority: medium
component: inverter
created: 2026-11-08
---

# Charge the battery from the grid to a target SoC by a deadline in the cheapest slots

## Summary

The inverter dialog should get a one-shot "charge from the grid" schedule, the same way the floor heating
has "Schedule morning heating": the user sets a target SoC and a target time (e.g. 80 % by 6:00) and the
manager charges the battery from the grid in the cheapest 15-minute spot price slots before that time.

Requirements agreed in the discussion:

- **One-shot**, like the floor heating: it turns itself off after the deadline.
- **No needless battery cycling.** Pushing energy through the battery costs about 1 CZK/kWh (wear plus
  conversion losses). On a night with cheap, flat prices the battery must not supply the house and be
  recharged later; the house runs from the grid for the whole window and the battery only takes the
  planned charge.
- The battery may still supply the house in a slot that is expensive enough to pay for the extra charge
  and the wear (typically the evening peak at the start of the window).

## Measured inverter behavior

Solax X3-Hybrid G4, 10 kWh battery, battery charge current limited to 20 A. Measured on 2026-11-08 (local
time) with a one-off test against the real inverter and with the `home-inverter` series in InfluxDB.

| Situation | Observed |
| --- | --- |
| SelfUse, SoC below `SelfUse_Discharge_MinSoC` | inverter goes `Idle` within ~1 min, battery 0 W, the house runs from the grid (grid import = house load). It does **not** recharge itself from the grid. SoC stays flat for 7–12 h (nights of 2–5 Nov). |
| Raising min SoC above SoC (42 % → 60 %) | `Idle` within ~1 min |
| Lowering min SoC below SoC while `Idle` (60 % → 40 % at SoC 43 %) | wakes: `Checking` within a minute, then `Normal` (~5 min) |
| Writing ManualMode=ForceCharge, then SolarChargerUseMode=Manual while `Idle` | `Checking` after ~25 s, `Normal` after 324 s, charging after **330 s**. One write was enough, no retry needed. |
| ForceCharge at SoC 42 % | **4845–4880 W** into the battery, grid import 5540–5590 W at ~485 W house load, i.e. ~96 % AC→battery |
| Back to SelfUse (min SoC still above SoC) | charging stops immediately, `Idle` again ~28 s later |
| Manual / Stop | stays `Normal`, battery −71 W (inverter self-consumption), house from the grid |

Side notes:

- Right after entering `Idle` at low SoC the SoC once dropped 15 % → 11 % in 1.5 h at 0 W battery power.
  It is most likely the BMS re-estimating SoC at the bottom of the LFP curve, not a real energy loss, and is
  not expected at a charged battery. The re-planning tolerance below covers it if it happens.
- The quiet house load is about 400 W.
- Registers written by this feature and whether the inverter stores them in EEPROM (spec "Write" section,
  `doc/inverter/solax_modbus_spec_3.34.md`): `SolarChargerUseMode` 0x001F ★, `ManualMode` 0x0020 (not
  stored), `SelfUse_Discharge_MinSoC` 0x0061 ★. The spec says ★ registers are written to EEPROM on change.
- The 324 s from the write to `Normal` is the grid checking time (`CheckingTime`, 0x0002, X3 0–1000 s) plus
  ~25 s before `Checking`. It was measured once. The client does not read 0x0002 today, so `WAKE_LEAD` is a
  constant with a margin rather than a value read from the inverter.
- The nights of 2–5 Nov ran with `inverter.manager.turnoff.on.negative.price=true`, so `doPowerManagement()`
  wrote `setInverterOn(true)` every 15 minutes while the inverter was `Idle` (see Ownership below). The
  inverter stayed `Idle` for 7–12 h, so these writes do not wake it.

## Design

### Slot states

The window runs from the activation to the deadline. Every 15-minute slot gets one of three states:

| State | Inverter | Battery | House | Cost of the slot |
| --- | --- | --- | --- | --- |
| `CHARGE` | Manual / ForceCharge | +4.8 kW | grid | price × (P + L) + wear × P |
| `HOLD` | SelfUse, min SoC 100 → `Idle` | 0 | grid | price × L |
| `DISCHARGE` | SelfUse, normal min SoC | −L | battery | nothing in the slot; L is bought back in a `CHARGE` slot, where it pays its price and wear |

P and L are energies per slot (P = 1.2 kWh, L = 0.1 kWh). The wear cost is per kWh of a full
charge–discharge cycle and is charged once, in the `CHARGE` slot that supplies the energy. A `DISCHARGE`
slot therefore pays off when `price > marginal charge price + wear` (step 2 below).

"Normal min SoC" is `getDesiredMinimalSoc()`, the configured minimum plus the battery reserve outside the
high tariff hours, not the bare option value.

`HOLD` uses min SoC 100 for the whole window instead of Manual / Stop: the inverter sleeps and nothing
is taken from the battery (Stop costs 71 W), only two 0x0061 writes per night are needed, and morning PV
can still charge the battery in SelfUse.

### `BatteryChargePlanner` (extensions, pure functions)

Same style as `FloorHeaterPlanner`, unit tested, reuses its `nextDeadline()` and `nextSlotStart()`.

The window starts with the slot in progress (now rounded down to a 15-minute boundary), so the activation
has a state to apply. The rest of that slot is shorter than the wake lead, so it is never `CHARGE`, and its
house energy in the SoC simulation is prorated by the remaining minutes.

Input: prices, now, deadline, current SoC, target SoC, normal min SoC, battery capacity, charge power P,
house load L, wear cost. Output: the slots with their states, or null when a slot up to the deadline has no
published price (OTE publishes the next day around 14:00).

1. All slots start as `HOLD`. Energy to charge E = (target − SoC) × capacity. Take the
   ⌈E / (P × ¼ h)⌉ cheapest slots as `CHARGE`, ties go to the later slot. Slots with a negative price are
   not candidates: the inverter is turned off then (see Ownership) and the PV covers the house and charges
   the battery anyway.
2. Go through the remaining slots from the most expensive one. A slot becomes `DISCHARGE` when
   `price > marginal charge price + wear`. Each one adds L × ¼ h (0.1 kWh) to E; the marginal charge price
   is the price of the slot that supplies that energy: the unused part of the last `CHARGE` slot, then the
   next cheapest `HOLD` slot. A simulation of SoC over the slots (bounded by the normal min SoC and 100 %)
   checks that the target is reached at the deadline. The decision is made slot by slot and the block rule
   comes only in step 3, so the result is good, not optimal.
3. `DISCHARGE` costs two 0x0061 writes, so only blocks of at least `MIN_DISCHARGE_MINUTES` are kept
   (proposed 60); shorter ones go back to `HOLD`. Their energy no longer needs to be bought back, so E
   shrinks and step 1 is repeated. The cheapest-slot set is monotone in E, so only the most expensive
   `CHARGE` slots drop out and the earlier choices stay.
4. Adjacent `CHARGE` slots are merged into blocks.

On a flat night step 2 selects nothing and the whole window is `HOLD` except the charge blocks.

### Execution in `InverterManager`

A new `manageGridCharge()` runs every minute (next to `manageForceDischarge()`); all writes happen only
when the value differs from what the inverter reports, so the method is idempotent and a restart is
reconciled within a minute.

- **Activation:** plan, then apply the state of the current slot. `HOLD` → min SoC 100.
- **Charge block:** `WAKE_LEAD` (330 s measured once, CheckingTime plus ~25 s, rounded up to 6 min)
  before the block starts, write
  ManualMode=ForceCharge and then SolarChargerUseMode=Manual, so the charging starts at the slot boundary.
- **Stop:** at the end of the block, or as soon as SoC ≥ target, back to SelfUse; with min SoC 100 the
  inverter goes `Idle`. In `Idle` the SoC does not fall, so the target needs no reserve.
- **Re-planning:** at every slot boundary from the real SoC. A top-up block is added only when the
  shortfall against the plan is at least 3 % (`TOP_UP_TOLERANCE`); SoC is an integer and a top-up costs a
  wake-up and two 0x001F writes.
- **Deadline:** `WAKE_LEAD` before the deadline restore the normal min SoC, so the inverter is awake and the
  battery takes over the house at the deadline. Then clear the one-shot.

### Ownership of the battery settings

Three features write the same registers today. While a plan is active it owns them:

- the high tariff scheduler (`applyMinBatterySoc`, 0x0061) is skipped; at the end of the plan
  `highTariffScheduler.applyCallback()` restores the normal value;
- the export priority timer (`applyBatteryMode`, 0x001F, re-asserted every 15 min) is skipped;
- force discharge is suppressed.

The boolean `forceDischargeActive` becomes one owner state, e.g. `NONE / GRID_CHARGE / FORCE_DISCHARGE`.
It is reported to `home-inverter-manager` as the string field `battery_owner`, next to the existing
`force_discharge` bool; the planned state of the current slot (`charge` / `hold` / `discharge` / none) goes
there too as `grid_charge_slot`, so the first real night can be read from Grafana against `home-inverter`.
Document both in `doc/metrics.md`.

`doPowerManagement()` with `inverter.manager.turnoff.on.negative.price=true` calls `setInverterOn(true)`
whenever the mode is not `Normal`, i.e. every 15 minutes while `Idle`. The nights of 2–5 Nov show that
this does not wake the inverter, and SystemON_OFF (0x001C ★) is already 1, so per the spec the write does
not reach the EEPROM. Each one is still an INFO line `setInverterOn(true)`, about 30 per plan night, so
`Idle` and `Checking` should count as "on" there and the write is not issued. The negative-price turn-off
itself does not conflict with the plan: negative prices come only with full sun, when the PV covers the
house and charges the battery, so there is nothing to take from the grid.

### Failure modes

- App down while min SoC is 100: the battery stays idle and the house runs from the grid until the app is
  back. Safe.
- App down during ForceCharge: the battery charges from the grid up to 100 %. Acceptable, the inverter
  stops at full.
- A write ignored while the inverter is waking up: the next minute's run repeats it.

### State and parameters

`InverterManager` gets `StateMap` and `Clock` constructor parameters, like `FloorHeaterManager`, wired in
`PiConfigurator` and `PiPeConfigurator`; the `Clock` makes the manager unit-testable.

Runtime one-shot state goes to `StateMap` (`cfg/app-state.properties`), like the floor heating:

```
inverter.gridCharge.targetSoc=80
inverter.gridCharge.targetMinutes=360
inverter.gridCharge.scheduleUntil=2026-11-10T06:00
```

Deployment constants are constructor parameters set in `PiConfigurator` (next to the existing
`batteryCapacityWh`): charge power 4800 W, house load 400 W. The wear cost is an option,
`inverter.manager.battery.wear.cost=1.0` (CZK/kWh). The planner is disabled when the charge power is 0
(`PiPeConfigurator`).

### REST and UI

- `InverterHandler`: the status gets `gridCharge` (scheduleUntil, targetSoc, target, canPlan, the slots as
  ranges `{from, to, kind}`, estimated cost); the action takes `gridCharge=on|off`, `targetSoc`, `target`.
  Like `FloorHeaterHandler`, it runs synchronously and a rejected change answers 400 with the message
  (e.g. "Prices until … are not published yet").
- `canPlan` is false until the prices up to the deadline are published: `getDayPrices()` returns today
  and tomorrow once OTE publishes tomorrow (~14:00), otherwise yesterday and today. The dialog shows this
  next to the switch before the user flips it, not only as the 400 afterwards.
- Inverter dialog: a "Charge from grid" section with the one-shot switch, a target SoC stepper (5 %) and a
  target time stepper (15 min, as `floorHeaterItem.js` does), and a summary such as
  `Charge 1:00 h in 2:00–3:00, battery idle 22:00–6:00, est. 13 CZK`. The price chart with ranges from the
  floor heating dialog can show the plan later.
- The dialog may warn (not block) when the morning price minus the charge price is below the wear cost.

### EEPROM budget

The inverter's EEPROM endurance is not published. Typical figures give, for 20 years (7300 days):
10 000 writes (flash-emulated, pessimistic) → 1.4 a day; 100 000 → 14 a day. A plan night costs two
0x0061 writes plus two 0x001F writes per charge block, i.e. 4–6. At ~100 plan nights a year that is about
4000 writes per register in 20 years. Today's baseline is near zero: the existing code writes only on
change and the high tariff reserve is 0.

## Worked example

Activated at 20:00, SoC 30 %, target 80 % at 6:00, cheapest slots around 2:00:

| Time | Action | Inverter |
| --- | --- | --- |
| 20:00 | plan; evening slots expensive enough → `DISCHARGE` | SelfUse, `Normal` |
| ~22:00 | expensive slots over, min SoC → 100 | `Idle` within ~1 min |
| 1:54 | ForceCharge + Manual (wake lead) | `Checking` |
| 2:00 | charging 4.8 kW, +12 % per slot | `Normal` |
| ~3:15 | SoC 80 %, back to SelfUse | `Idle` within ~30 s |
| 5:54 | normal min SoC restored | `Checking` |
| 6:00 | battery supplies the house, plan off | SelfUse, `Normal` |

Without the evening discharge it is 5 kWh (~1:02 h of charging); with SoC down to 20 % by 22:00 it is
6 kWh (~1:15 h). The house takes ~2.8 kWh from the grid during the idle hours at night prices.

## Alternatives considered

- **SelfUse for the whole window** (the first idea): the house drains ~4 % SoC per hour; the plan needs
  ~2 more charge slots per night and ~2.5 kWh more passes through the battery. Rejected by the
  no-cycling requirement.
- **Manual / Stop as `HOLD`**: works and keeps the inverter awake (no wake delay), but costs 71 W from the
  battery all night and blocks morning PV charging. Kept as a fallback if min SoC 100 turns out badly.
- **Native SelfUse night charge** (0x0062/0x0063, periods 0x0068–0x0070): two fixed periods, every change
  is an EEPROM write, and the discharge periods share the settings.
- **Remote Power Control, SOCTarget** (0x007C+, not stored in EEPROM): exactly this use case, but added in
  protocol V3.33; firmware support unknown. Caution: in the single-register write table 0x007C is
  `PowerFactor_Curve_PfLockInPoint` ★; the Remote Power Control block is a separate write-multiple map.
  Never probe it with `writeSingleRegisterValue`.

## Open points

- **Surplus above the target** (SoC already above it at activation): hold it (simple), or allow
  `DISCHARGE` against the mean price of the 2–3 hours after the deadline.
- **Minimum discharge block:** 60 min proposed.
- **Charge taper above ~90 %:** not measured. Until then the planner can assume a lower power in the top
  band; the first real night shows it in the log.
- **SoC jump after falling asleep at high SoC:** not expected; check after the first real night.

## Implementation steps

1. `BatteryChargePlanner` + unit tests (flat night, evening peak, missing prices, SoC above target,
   minimum discharge block shrinking E, negative price slots, the partial slot at activation, target
   unreachable before the deadline).
2. Owner state in `InverterManager`; `StateMap` and `Clock` constructor parameters; skip the high tariff,
   export priority and force discharge writes while a plan owns the battery; `Idle` and `Checking` count as
   "on" in `doPowerManagement()`.
3. `manageGridCharge()`: one-shot state in `StateMap`, wake lead, stop at target, re-planning with
   tolerance, deadline hand-over, restart reconciliation.
4. `InverterHandler` status and action, tests.
5. Inverter dialog section and plan summary.
6. Metrics: `battery_owner` and `grid_charge_slot` in `InverterMetrics`, listed in `doc/metrics.md`.
7. Documentation: a section on the planner in an inverter doc (new `doc/inverter.md` or similar),
   referenced from `CLAUDE.md`.
8. Manual verification by the owner on the real inverter (no HW-touching tests are run automatically).
