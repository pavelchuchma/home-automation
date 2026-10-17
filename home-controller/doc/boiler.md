# Boiler (Coolwex DSW-300 heat pump water heater)

How the boiler is driven from the controller, how the Modbus gateway behaves,
and how to read the boiler lines in the app log. Companion to
`doc/todo/001-boiler-turnon-no-retry.md`, which tracks the open reliability
work.

## Hardware chain

```
home-controller (Pi)  --Modbus TCP :502-->  ESP32 gateway  --SPI sniff / key emulation-->  boiler display board
```

- The boiler has no native interface. The gateway is an ESP32 retro-fitted
  between the boiler CPU and its display controller (reads the display frame
  over SPI) and between the CPU and the keyboard matrix (emulates key presses).
- Gateway firmware: <https://github.com/pavelchuchma/coolwex-remote-control>.
  The Modbus register map and enums are its `include/types.h`; the Java side
  mirrors them in `BoilerController.ModbusRegisters`, `State.MODE` and
  `KeySequence` (ordinal mapped, so the order must match).
- The gateway registers itself as `boiler.local` via mDNS; the host is
  configured as `boiler.ip`.

### Every write is a key sequence

A Modbus write to the gateway does not set a value, it starts a sequence of
emulated key presses on the boiler panel and blocks until that sequence ends:

| Write | What the gateway does | Max duration |
| --- | --- | --- |
| coil 200 `refreshStatus` | unlock display, probe power state, read target temp, walk the six info screens, return to `unlocked` | 15 s |
| coil 210 `powerOn` | unlock, press OnOff if needed, probe the power state again | 10 s |
| holding 300 `targetTemp` | unlock, step the set-temp screen to the value, confirm | 15 s |
| coil 211 `eHeatOn` | unlock, press EHeater + Enter, read the blinking icon | ~5 s |

Each sequence verifies its own end state and the gateway answers a plain
Modbus success only when it matched. Otherwise it answers Modbus exception 4
(`Slave Device Failure`), which j2mod raises as `ModbusSlaveException`.

Consequences:

- **Repeating a rejected write is safe.** Sequences are "ensure state"
  operations; if the key press did land and only the verification failed, the
  repeated write ends as a no-op on the gateway.
- **Writes are serialised in the gateway.** A request arriving while a
  sequence runs waits for it. The Java TCP timeout is therefore 30 s.
- **Key emulation glitches are expected**, not exceptional: the ESP services
  each keyboard scan pulse from an interrupt, and Wi-Fi or other background
  work can delay it so that the boiler misses or misreads a press. The
  firmware recovers from most of them (waits for the expected display screen,
  leaves info / set screens before acting), the controller retries the rest.

### Failure diagnostics registers

Input registers 109–111 hold the last key sequence that failed in the gateway
since its boot: sequence (`KeySequence` ordinal), step, and display mode
(`State.MODE` ordinal) at that moment. `0/0/0` means no failure since boot.
They are overwritten by every new failure, so the controller logs them with
every state read (see below).

## Controller side

Classes in `extensions/.../external/boiler/`:

- `BoilerManager` — owns the desired state and the schedule. Reads
  `boiler.ip`, `boiler.times` (e.g. `12:00-17:00`, several ranges separated by
  `;`), `boiler.target.temp` (38–60), `boiler.disinfect`, `boiler.eheat` from
  the options (`cfg/app.properties`, changeable at runtime). An
  `IntervalScheduler` fires `turnOn()` at each interval start and `turnOff()`
  at each end, once. `turnOn()` refreshes the state and writes only what
  differs (target temp, power, eHeat). `turnOff()` powers down and clears the
  one-shot `disinfect` / `eheat` flags. Changing any of the options re-applies
  the state for the current time.
- `BoilerMonitor` — `AbstractStateMonitor` refreshing the state every 10 min,
  but only while somebody asked for it within the last hour (the web UI
  `/rest/boiler`). At night it sleeps, so its log lines are not a heartbeat.
- `BoilerController` — the Modbus client. Every write goes through
  `writeWithRetry()`: up to 3 attempts, repeated only for `ModbusSlaveException`
  (5 s pause) and `ModbusIOException` (15 s pause). Anything else, or the third
  failure, propagates to `BoilerManager`, which logs it and gives up for that
  interval edge.

Known gap (todo 001): if all three attempts fail, nothing retries later and
the boiler stays in the wrong state until the next interval edge. There is no
periodic reconciliation yet.

## Reading the log

Logs live on the Pi as described in `doc/log-analysis.md`. Boiler-relevant
lines, all under `o.c.h.e.e.b.*` or `o.c.h.e.e.i.InverterManager` (the
`BoilerManager` logger is misnamed after `InverterManager`):

```
DEBUG [BoilerController] Refreshing status
DEBUG [ModbusClient]     writing coil register: 0x00C8=true
DEBUG [BoilerController] Refreshing state
DEBUG [BoilerController] Mode: unlocked, statusAge: 0, tSU: 41, tSL: 27, tT3: 12, tT4: 9, tTP: 17, tTh: 16, tTarget: 42, flags[on, pump], lastFail: none
DEBUG [InverterManager]  Turn on: targetTemp already set to 42
DEBUG [InverterManager]  Turn on: OFF -> ON
DEBUG [BoilerController] Setting power on: true
```

- **`Mode:` line** — one per state read. `tSU` / `tSL` are the upper / lower
  tank temperatures, `tTarget` the set point, `flags[...]` the status icons
  (`on`, `hot`, `eHeat`, `pump`, `vacation`). `Mode` should be `unlocked`
  after a refresh; anything else means the gateway's end-state check did not
  hold. `lastFail` is `none` or `<sequence>/<step>/<mode>`, e.g.
  `refreshStatus/0/locked`. A `lastFail` that changes between two reads is a
  glitch the gateway survived on its own.
- **Did the boiler heat today?** Look for `Turn on: OFF -> ON` at the interval
  start, `flags[on` in the `Mode:` lines inside the interval, and
  `Turn off: ON -> OFF` at the end. `Turn off: already OFF` at the end means it
  never ran.
- **Retry lines** (WARN):
  `Boiler set power on true failed (attempt 1/3): ...ModbusSlaveException: Slave Device Failure; gateway lastFail: powerOn/4/infoTh; next attempt in 5000 ms`
  followed by either `Boiler set power on true succeeded on attempt 2/3` or,
  after the third failure, `ERROR [InverterManager] Turn on failed` with the
  stack trace. After a network error the diagnostics read is skipped
  (`gateway lastFail: not read`).
- The `[cron4j::...]` thread is the interval edge, `[BoilerMonitor]` the
  periodic read, `[IntervalScheduler]` a re-apply after an options change.

Quick checks over SSH:

```sh
# today's edges, retries and failures
grep -a -E 'Turn on|Turn off|Boiler .* attempt|lastFail: [^n]' /usr/local/bin/homeAutomation/out/app.log

# display modes seen at state reads (anything but "unlocked" is suspicious)
grep -a -o 'BoilerController\] Mode: [A-Za-z0-9]*' /usr/local/bin/homeAutomation/out/app.log | sort | uniq -c
```

## Testing

`BoilerControllerTest` talks to the real boiler and changes its state (power,
target temperature). Never run it unattended; see the test safety section in
`CLAUDE.md`. The firmware repo ships `tools/boiler_test.py`, which drives the
gateway over Modbus while capturing its serial log, for testing changes on the
gateway side.
