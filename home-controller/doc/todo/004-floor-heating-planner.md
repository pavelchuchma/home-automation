---
id: 004
title: Plan the electric floor heating before the morning from spot prices
type: enhancement
status: done
priority: medium
component: app
created: 2026-11-03
resolved: 2026-11-03
---

# Plan the electric floor heating before the morning from spot prices

## Summary

The 12-relay board switches the electric floor heating of eleven rooms (commit 7647cc2 added the
`/floorHeaters` page with a plain toggle per room). The heating is used only on a few winter days, with
expensive electricity and no solar surplus, so a room should be pre-heated during the cheapest 15-minute
slots of the night and early morning. The room has a large thermal inertia: it is wanted warm around 9:00,
heating that ends at 6:00 is fine, heating at midnight is mostly lost by the morning.

Wanted:

- the room tile opens a dialog (the `SettingsSheet` of the boiler) instead of toggling the relay;
- an explicit on/off switch, a heating duration in 15-minute steps and a one-shot "Schedule morning
  heating" switch that plans the cheapest slots before a target time, later slots preferred;
- a price chart from now to the end of the target day with the heating ranges drawn in;
- the planned ranges editable like the boiler operating times; a switch-on by hand creates a range of at
  most 6 hours, so nothing is ever left on;
- no change on the floor plan page.

The agreed UI is the interactive mockup `Podlahovka Morning Planner` (claude.ai artifact, private).

## Design

### State: `StateMap`, not `Options`

Ranges and the one-shot deadline are runtime state: they change by the planner, by the switch and by the
clean-up, and they exist only in the deployment with the relay board. They go to `StateMap`
(`cfg/app-state.properties`, delayed write), not to `app.properties`, so `default-app.properties` and the
other deployments stay untouched. The three numeric settings live there too, as integers, because only the
floor heating dialog edits them.

`StateMap` today holds only `int` values, so it gets string values:

- `StateMap`: `void setValue(String key, String value)`, `String getString(String key)`; the int methods
  stay and wrap them.
- `PersistentStateMap`: the map becomes `String -> String`, `load()` splits a line at the first `=` only.

Keys (`<id>` is the actor id, e.g. `pdlKoupH`):

```
floorHeater.<id>.times=P2026-11-04T02:15/2026-11-04T02:30;M2026-11-03T21:45/2026-11-04T03:45
floorHeater.<id>.minutes=180
floorHeater.<id>.scheduleUntil=2026-11-04T10:00
floorHeater.targetMinutes=600
floorHeater.laterBonusHaler=20
```

- `times`: absolute ranges, `P` planned by the planner, `M` made by hand (the switch, "+ Add range", or an
  edited planned range keeps its kind). The kind drives the color in the dialog and the schedule switch,
  which removes only planned ranges.
- `scheduleUntil`: present while the one-shot schedule is on; it is the deadline the ranges were planned
  for. Missing key = schedule off.
- `minutes`, `targetMinutes`, `laterBonusHaler`: duration, target time of day and the later-is-better
  bonus (CZK/kWh per hour before the target, in halers). Defaults 180, 600 (10:00) and 20 when the key is
  missing. The manual range length is a constant, 6 hours.

### `FloorHeaterPlanner` (extensions, pure functions)

Package `org.chuma.homecontroller.extensions.external.floorheater`. No I/O, unit-tested.

- `nextDeadline(LocalDateTime now, LocalTime target, int minutes)`: the next target time after now; when
  fewer than `minutes` remain until it, the target time one day later.
- `plan(IntervalPrice[] prices, long nowMillis, long deadlineMillis, int minutes, double bonusPerHour)`:
  slots are the 15-minute intervals from the next slot boundary to the deadline. When any slot has no
  price, returns `null` (prices for the window are not published). Otherwise the slot key is
  `price + bonusPerHour * hoursFromSlotEndToDeadline`; the `minutes / 15` slots with the lowest key are
  taken (ties go to the later slot) and merged into ranges.
- `HeatingRange` record `(LocalDateTime from, LocalDateTime to, Kind kind)` with `parse`/`format` of the
  `times` string, `normalize` (sort, merge overlaps; a merge with a manual range is manual) and
  `validate` (end after start, no overlaps) with messages for the dialog.

### `FloorHeaterManager` (extensions)

One instance for all rooms. Constructor takes the rooms (`id`, `IOnOffActor`, power in W),
`ElectricitySpotPriceMonitor` and `StateMap`. It is the only writer of the floor heating relays.

- A single-thread `ScheduledExecutorService` runs `tick()` every minute and right after every change.
  `tick()` drops ranges that ended, clears `scheduleUntil` once the deadline passed, and for each room
  switches the relay only when `actor.isOn()` differs from "now is inside a range". Idempotent, so a
  restart of the application or a reboot of the relay node is corrected within a minute, and a relay
  toggled through the generic `/rest/onOff` is reverted. Switching is logged at INFO with the room and
  the range.
- `switchOn(id)`: adds a manual range `[now, now + 6 h)` and normalizes. `switchOff(id)`: shortens the
  running range to now (removes it when it just started).
- `setSchedule(id, on)`: removes the planned ranges; when `on`, plans from `priceMonitor.getDayPrices()`
  and stores the ranges and `scheduleUntil`; throws `IllegalStateException("Tomorrow's prices are not
  published yet")` when the planner returns `null`, so the state stays unchanged.
- `setMinutes(id, m)`, `setTarget(minutesOfDay)`, `setLaterBonus(haler)`: store and replan the rooms
  whose schedule is on.
- `setRanges(id, timesString)`: validates and stores an edited list.
- `canPlan(id)`: whether the prices cover the planning window of the room, for the dialog.
- `Status` per room: on, running range, ranges, minutes, scheduleUntil, canPlan.

### `FloorHeaterHandler` (app, REST)

`AbstractRestHandler` keyed by room id, like `OnOffHandler`:

- `GET /rest/floorHeater/status`: per room `name`, `val` (relay on), `powerW`, `minutes`,
  `scheduleUntil`, `canPlan`, `ranges` (array of `{from, to, kind}` as epoch millis); the shared
  `target` and `laterBonus` repeated in every item, as the status array has no header.
- `GET /rest/floorHeater/action?id=<id>&...`: `switch=on|off`, `schedule=on|off`, `minutes=<int>`,
  `ranges=<times string>`, `target=<minutes>`, `laterBonus=<haler>`. Several parameters in one call are
  applied in this order. A validation error returns 400 with the message, which `SettingsSheet` shows.

The price chart uses the existing `/rest/eprice/status`.

### Page and dialog (app resources)

- `FloorHeatersPage`: tiles without `onClick` toggling, scripts extended by `settingsSheet.js`,
  `items/ePriceItem.js` is not needed (the chart is drawn by the dialog from the eprice JSON), stylesheet
  `settingsSheet.css` added.
- `floorHeaters.js`: `Status` polls `/rest/floorHeater/status` and a second `Status` (or the same, with
  the eprice handler added to the page's status set) brings the prices. The tile shows the relay state and
  a badge "3:00 h in 2:15–10:00" (total heating time and the span of the ranges).
- `items/floorHeaterItem.js`: `FloorHeaterItem` with `FLOOR_HEATER_SETTINGS` for `SettingsSheet`:
  `instant: true`, a `store` calling the action endpoint (pattern of `hvacItem.js`), `live` block with
  the state row and the on/off switch, groups Electricity price (chart), Morning heating (duration
  stepper, schedule switch), Heating ranges (editable list), Planner (target time stepper, bonus
  stepper).
- `settingsSheet.js` needs two additions: a `ranges` field variant with a date, i.e. values are epoch
  millis and the row shows the day, and a stepper text formatter (`3:15` for minutes, `10:00` for a time
  of day) instead of the numeric input. The time picker is reused as is.
- The dialog re-renders only the changed control (not the whole body), so the scroll position is kept
  when ranges are added; the mockup had to work around this.

### Documentation

`doc/floor-heating.md`: the keys in `app-state.properties`, the planner algorithm with a worked example,
the semantics of the switch, how to read the INFO lines in the log. Linked from `CLAUDE.md`.

## Implementation steps

1. `controller`: string values in `StateMap` and `PersistentStateMap`; test for load/save round trip of
   a value with `=` inside.
2. `extensions`: `HeatingRange`, `FloorHeaterPlanner` with tests (deadline rollover, missing prices,
   tie to the later slot, merge of adjacent slots, the later-is-better bonus moving the choice).
3. `extensions`: `FloorHeaterManager` with tests on a fake `IOnOffActor` and an in-memory `StateMap`
   (switch on/off, tick switching at range boundaries, clean-up of ended ranges and of `scheduleUntil`,
   schedule refused without prices, replan after a change of minutes).
4. `app`: `FloorHeaterHandler`; `PiConfigurator` builds the manager from the existing `addFloorHeater`
   calls and registers the handler next to `BoilerHandler`.
5. `app` resources: page, item, settings sheet additions, styles.
6. `doc/floor-heating.md`, `CLAUDE.md` link, this issue resolved.

Build check: `./gradlew compileJava compileTestJava` and the new tests only
(`./gradlew :controller:test --tests "*StateMap*"`, `./gradlew :extensions:test --tests
"*FloorHeater*"`). None of them touches hardware.

## Open points

- On flat nights (2026-11-04: 8.07 to 8.94 CZK/kWh from midnight to 5:30) the planner picks scattered
  single slots, and the slot just before the target is often chosen because its bonus is zero. A minimal
  range length or a bonus counted from the slot start are candidates for a second iteration, after the
  behavior is seen in use.
- No limit of the total power of rooms heating at once: the heaters are spread over the three phases
  with margin.
- No metrics: the INFO log lines and the consumption graph are enough for a few rooms.

## Resolution

Implemented as planned (not yet deployed to the Pi):

- `controller`: `StateMap` / `PersistentStateMap` hold string values.
- `extensions/.../external/floorheater`: `HeatingRange`, `FloorHeaterPlanner`,
  `FloorHeaterManager`, 25 unit tests.
- `app`: `FloorHeaterHandler` (synchronous actions, 400 on a rejected change),
  `FloorHeatersPage` opens the dialog, wiring in `PiConfigurator`.
- `settingsSheet.js`: stepper `format`, field type `dateRanges`, `cfg.sync`,
  `enabledIf(state, item)`, generic time picker; `items/floorHeaterItem.js`
  with the dialog and the price chart; `floorHeaters.js` and styles.
- User documentation in `doc/floor-heating.md`, linked from `CLAUDE.md`.

The UI was exercised in Chrome against a fake backend serving the real static
files and real OTE prices; the Java side only through its unit tests. The
planner behavior on flat nights (scattered single slots, the slot right before
the target) stays an open point for a second iteration, see `doc/floor-heating.md`.

