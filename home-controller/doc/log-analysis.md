# Reading the application log (PIR / sensor activity)

How to answer questions like "which motion sensors fired between 05:30 and
07:15 this morning" from the running Pi. The pitfalls below are the reason this
file exists -- the log format itself is straightforward, but the sensor
semantics are not.

## Where the log is

On the Pi (`pi.local`), under the deploy directory:

| Path | What |
| --- | --- |
| `/usr/local/bin/homeAutomation/out/app.log` | today's log |
| `/usr/local/bin/homeAutomation/out/app-YYYY-MM-DD-N.log.gz` | rotated, gzipped |

Rotation happens at midnight. The `-N` suffix is a same-day rollover counter, so
a busy day can produce `-0`, `-1`, `-2`; check for extras before assuming one
file covers the whole day. **After midnight, "this morning" is no longer in
`app.log`** -- it has moved to `app-<yesterday>-0.log.gz`.

SSH uses the `pi` account, not your local username: `ssh pi@pi.local`. The
`script/deploy-pi.sh` helper picks whichever of `pi` / `pi.local` answers on
port 22.

Which configurator is running is decided by `system.application.configuration.name`
in `cfg/app.properties` (`chuma` -> `PiConfigurator`, see `Main.getConfigurator`).
The sensor names below come from that configurator.

## Anatomy of a sensor event

Every pin edge produces a pair of adjacent lines:

```
INFO  [o.c.h.b.n.Node] input 'pinC3' HIGH (493308ms)
DEBUG [o.c.h.c.n.NodeListener] Executing ActionBinding: pirA2Patro:2.in1(node7.pinC3)
```

- The **INFO** line (`Node.packetReceivedImpl`) carries the electrical edge but
  *not* the node id -- on its own it is ambiguous, since several nodes have a
  `pinC3`.
- The **DEBUG** line (`NodeListener.onInputChange`) carries the identity
  (`<device>:<connector>.in<n>(node<id>.pin<XY>)`) but not the edge.

You need both. Any `-> action: <actor> of action type <Class>` lines that follow
are the actions that fired; the `PirStatus` action is an anonymous class, so its
action type prints empty.

Note that `Executing **OnInit**ActionBinding` is a state replay after node
initialization, not real activity. Match on `Executing ActionBinding` exactly.

### Gotcha 1 -- pair by thread, not by adjacency

Packet processing is multi-threaded (`pool-2-thread-NNNN`). Two threads can
interleave so that another thread's INFO line lands between an INFO/DEBUG pair.
Naive "remember the previous INFO line" pairing silently mis-attributes the edge.
Key the pairing on the `[thread-name]` field.

This is rare but real: over one full day it happened once, which is enough to
corrupt a count. Always keep the pin cross-check described below.

### Gotcha 2 -- PIRs and magnetic sensors have opposite polarity

Both go through `AbstractConfigurator.setupSensor`, but with
`logicalOneIsActivate` inverted:

| Sensor kind | Helper | Active edge |
| --- | --- | --- |
| PIR (motion) | `setupPir` | **HIGH** = motion detected |
| Magnetic (urinals, garage, pump) | `setupMagneticSensor` | **LOW** = contact active / occupied |

Getting this backwards turns the gaps between urinal visits into the visits
themselves, and the result still looks superficially plausible.

### Gotcha 3 -- a urinal fires the valve on *both* edges

`setupMagneticSensor` for the urinals passes an action to each edge:

- arrival (LOW): `SwitchOnActionWithTimer(pisoarX, 3)` downstairs / `2` upstairs -- pre-rinse
- departure (HIGH): `SwitchOnActionWithTimer(pisoarX, 7)` downstairs / `4` upstairs -- the flush

So counting `-> action: pisoarHore` lines **double-counts flushes**. One visit =
one `occupied` + one `LEFT -> FLUSH`. The valve-actor lines are still useful as
an independent cross-check of the sensor-derived count.

### Verification: the pin cross-check

The DEBUG line names the pin (`node7.pinC3`) and so does the INFO line
(`'pinC3'`). Compare them on every event and count disagreements. A non-zero
mismatch count means the pairing broke (see gotcha 1) and the numbers cannot be
trusted. A clean run reports zero.

Separately, when a sensor shows zero events, check whether it is idle or dead by
counting its edges on other days before concluding anything.

## Sensor map (node 7, `PirNodeA`)

`PiConfigurator` is the source of truth; this table is a convenience copy and
can go stale.

| Binding | Name | Kind |
| --- | --- | --- |
| `pirA1Prizemi:1.in1` | Pradelna dvere | PIR |
| `pirA1Prizemi:1.in2` | Pradelna pracka | PIR |
| `pirA1Prizemi:1.in3` | Pisoar Dole | magnetic |
| `pirA1Prizemi:1.in4` | Vchod hore | PIR |
| `pirA1Prizemi:1.in5` | Schodiste | PIR |
| `pirA1Prizemi:1.in6` | Pisoar Hore | magnetic |
| `pirA2Patro:2.in1` | Chodba pred WC | PIR |
| `pirA2Patro:2.in2` | Chodba | PIR |
| `pirA2Patro:2.in3` | WC | PIR |
| `pirA2Patro:2.in4` | Chodba nad Markem | PIR |
| `pirA2Patro:2.in5` | Zadveri hore vchod | PIR |
| `pirA2Patro:2.in6` | Zadveri hore chodba | PIR |
| `pirA3Prizemi:3.in1` | Jidelna | PIR |
| `pirA3Prizemi:3.in2` | Obyvak | PIR |
| `pirA3Prizemi:3.in3` | Chodba dole | PIR |
| `pirA3Prizemi:3.in4` | Koupelna dole | PIR |
| `pirA3Prizemi:3.in5` | Spajza | PIR |
| `pirA3Prizemi:3.in6` | Zadveri dole | PIR |

Magnetic sensors on other nodes -- `cidlaGaraz` (Garaz hore/dole) and
`cidlaRozvadec` (Cerpadlo) -- follow the same LOW-is-active rule but are not
covered by the script below.

## The script

`script/sensor_events.py` implements all of the above. It prints one line per
edge plus a summary: per-sensor activation counts, urinal visits with their
durations, and the mismatch count.

It reads stdin by default, so the log can be streamed straight off the Pi
without copying anything:

```sh
# a window of today's log
ssh pi@pi.local "cat /usr/local/bin/homeAutomation/out/app.log" \
    | ./script/sensor_events.py --from 05:30:00 --to 07:15:00

# a rotated day
ssh pi@pi.local "zcat /usr/local/bin/homeAutomation/out/app-2026-09-09-0.log.gz" \
    | ./script/sensor_events.py --from 05:00:00 --to 08:30:00
```

A local path also works and `.gz` is unpacked automatically. Useful flags:
`-q` prints only the summary, `--gaps SECONDS` lists quiet stretches longer
than the given number of seconds.

`--from`/`--to` are `HH:MM:SS` and compare lexically against the timestamp, so
they only select within a single log file -- they do not span days. Both
default to the whole file.
