# Electric floor heating (Podlahovka)

How the floor heating relays are driven from heating ranges, how the morning
pre-heating is planned from spot prices, where the state lives and how to read
the floor heating lines in the app log. Design and reasoning in
`doc/todo/004-floor-heating-planner.md`.

## Hardware chain

```
home-controller (Pi)  --CAN-->  12-relay board "relayFloorHeater"  -->  heating cable contactors, one per room
```

Eleven rooms, 280 W to 3000 W each, spread over the three phases. The rooms,
relays and powers are wired in `PiConfigurator.addFloorHeater(...)`; the page
`/floorHeaters` lists them in that order.

## Model: the relay follows the ranges

A relay is on exactly while the current time is inside one of the room's
`HeatingRange`s, half-open `[from, to)`. There is no other on/off state:

- **Planned ranges** (`P`, orange in the dialog) come from the planner for the
  next morning, see below.
- **Manual ranges** (`M`, green) come from the on/off switch of the dialog, from
  "+ Add range", or from editing a range by hand. Switching on by hand adds a
  range from now for `FloorHeaterManager.MANUAL_MINUTES` (6 hours), so nothing
  stays on forever. Switching off shortens the running range to now.

`FloorHeaterManager` (extensions, package `external.floorheater`) owns the
rooms and is the only writer of their relays:

- `tick()` runs every minute, right after the minute starts, and after every
  change. It drops ranges that ended, clears a finished one-shot schedule and
  switches a relay only when its state differs from what the ranges say.
  Because it is idempotent, a restart of the application or a reboot of the
  relay node is corrected within a minute, and a relay toggled through the
  generic `/rest/onOff` is reverted.
- Overlapping or touching ranges are merged (`HeatingRange.normalize`); a merge
  with a manual range is manual. An edited list is validated: every range must
  end after it starts, no overlaps.

## The planner

`FloorHeaterPlanner` is pure (no I/O) and unit-tested:

1. **Deadline**: the next target time (`Target time` in the dialog, default
   10:00). When fewer than the requested minutes remain until it, the target
   time of the following day.
2. **Slots**: the 15-minute slots from the next slot boundary to the deadline.
   When any slot has no published price, planning is refused: the dialog
   disables the schedule switch and the action answers 400 "Prices until … are
   not published yet". OTE publishes the next day around 14:00.
3. **Compared price** of a slot = spot price (CZK/kWh incl. distribution and
   VAT, as `/rest/eprice` returns it) + `bonus × hours from the end of the
   slot to the deadline`. The bonus (`Later-is-better bonus`, default 0.20
   CZK/kWh per hour) makes a later slot win over an equally priced earlier one,
   because heat produced at midnight is mostly lost by the morning.
4. The `minutes / 15` slots with the lowest compared price are taken, ties go
   to the later slot, adjacent slots are merged into ranges.

Worked example, 3 h for Wed 4 Nov 2026 planned on Tue evening with the real
prices (night 8.07–8.94 CZK/kWh, morning peak 11.69 at 7:30): the plan was
2:15–2:30, 3:00–4:15, 4:45–5:45, 6:00–6:15 and 9:45–10:00. On such a flat
night the planner picks scattered single slots, and the slot right before the
target is attractive because its bonus is zero. A minimal range length or a
bonus counted from the slot start are the candidates if this turns out to be
annoying in use.

**Schedule morning heating** is a one-shot: enabling it replaces the planned
ranges with a new plan and stores the deadline; a change of the duration,
target time or bonus replans every room whose schedule is on (nothing is
stored when any of those plans fails); after the deadline the schedule turns
itself off. Disabling it removes the planned ranges, manual ones stay.

## State in `cfg/app-state.properties`

The ranges are runtime state, so they live in the `StateMap`, not in
`app.properties`. Deployments without a `FloorHeaterManager` never see these
keys. A missing key means the default.

```
floorHeater.pdlKoupH.times=P2026-11-04T02:15/2026-11-04T02:30;M2026-11-03T21:45/2026-11-04T03:45
floorHeater.pdlKoupH.minutes=180
floorHeater.pdlKoupH.scheduleUntil=2026-11-04T10:00
floorHeater.targetMinutes=600
floorHeater.laterBonusHaler=20
```

| Key | Meaning | Default |
| --- | --- | --- |
| `floorHeater.<id>.times` | ranges, `P` planned / `M` manual, local time, minutes | none |
| `floorHeater.<id>.minutes` | heating duration for the plan | 180 |
| `floorHeater.<id>.scheduleUntil` | deadline of the one-shot schedule; missing = off | off |
| `floorHeater.targetMinutes` | target time as minutes of the day, shared | 600 (10:00) |
| `floorHeater.laterBonusHaler` | bonus in halers per kWh and hour, shared | 20 |

## REST

`FloorHeaterHandler`, mounted at `/rest/floorHeater`:

- `GET /status` — per room `name`, `val` (relay on), `powerW`, `minutes`,
  `scheduleUntil` (epoch ms, 0 = off), `canPlan`, `target`, `laterBonus`,
  `manualMinutes`, `ranges: [{from, to, kind}]` (epoch ms).
- `GET /action?id=<room>&…` — `switch=on|off`, `schedule=on|off`,
  `minutes=<int>`, `ranges=<times string>`, `target=<minutes>`,
  `laterBonus=<haler>`, applied in this order. Unlike the other handlers the
  action runs synchronously and a rejected change answers 400 with the message,
  which the dialog shows.

## The page and the dialog

`/floorHeaters` polls the status every 750 ms and the prices once a minute. A
tile shows the relay state and a badge like `3:00 h in 2:15–10:00` (total
heating time and the span of the ranges). A click opens the `SettingsSheet`
dialog in instant mode: state row with the on/off switch, the price chart from
the current hour to the end of the target day with the ranges drawn in (dashed
line = target, optional dashed curve = compared price), Duration, Schedule
morning heating, the editable Heating ranges with a summary (heating time,
energy and the estimated cost from the known prices) and the shared Planner
settings.

Values the user has not touched follow the server (`cfg.sync`), so a plan or
a finished one-shot shows up while the dialog is open. The store sends only
values that differ from the server, and never the stale ranges together with
a schedule change.

## Reading the log

`FloorHeaterManager` logs at INFO:

```
Floor heating Koupelna hore: planned P2026-11-04T03:00/2026-11-04T04:00 until 2026-11-04T10:00
Floor heating Koupelna hore: ON until 2026-11-04T04:00 (planned range from 2026-11-04T03:00)
Floor heating Koupelna hore: OFF
Floor heating Koupelna hore: morning schedule until 2026-11-04T10:00 finished
```

A relay switched by hand outside the dialog shows up as an `ON`/`OFF` line on
the next tick, because the tick puts it back. `Floor heating tick failed` at
ERROR means the tick itself threw; the next one runs a minute later.

## Tests

All pure, no hardware:

```
./gradlew :controller:test --tests "*PersistentStateMapTest*"
./gradlew :extensions:test --tests "*floorheater*"
./gradlew :app:test --tests "*FloorHeaterHandlerTest*"
```
