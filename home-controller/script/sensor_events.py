#!/usr/bin/env python3
"""Extract PIR / urinal sensor events from a home-controller app log.

See doc/log-analysis.md for the log format and the reasons behind the
polarity and pairing rules implemented here.

Typical use -- stream a log straight off the Pi, no copying:

    ssh pi@pi.local "cat /usr/local/bin/homeAutomation/out/app.log" \\
        | ./script/sensor_events.py --from 05:30:00 --to 07:15:00

    ssh pi@pi.local "zcat /usr/local/bin/homeAutomation/out/app-2026-09-09-0.log.gz" \\
        | ./script/sensor_events.py --from 05:00:00 --to 08:30:00

A local path works too and .gz is unpacked automatically:

    ./script/sensor_events.py app-2026-09-09-0.log.gz --gaps 180
"""

import argparse
import gzip
import re
import sys
from collections import Counter

PIR = "PIR"
MAGNETIC = "MAGNETIC"

# Mirrors PiConfigurator.setupPir / setupMagneticSensor for node 7 (PirNodeA).
# PiConfigurator is the source of truth -- re-check it after any wiring change.
SENSORS = {
    "pirA1Prizemi:1.in1": ("Pradelna dvere", PIR),
    "pirA1Prizemi:1.in2": ("Pradelna pracka", PIR),
    "pirA1Prizemi:1.in3": ("Pisoar Dole", MAGNETIC),
    "pirA1Prizemi:1.in4": ("Vchod hore", PIR),
    "pirA1Prizemi:1.in5": ("Schodiste", PIR),
    "pirA1Prizemi:1.in6": ("Pisoar Hore", MAGNETIC),
    "pirA2Patro:2.in1": ("Chodba pred WC", PIR),
    "pirA2Patro:2.in2": ("Chodba", PIR),
    "pirA2Patro:2.in3": ("WC", PIR),
    "pirA2Patro:2.in4": ("Chodba nad Markem", PIR),
    "pirA2Patro:2.in5": ("Zadveri hore vchod", PIR),
    "pirA2Patro:2.in6": ("Zadveri hore chodba", PIR),
    "pirA3Prizemi:3.in1": ("Jidelna", PIR),
    "pirA3Prizemi:3.in2": ("Obyvak", PIR),
    "pirA3Prizemi:3.in3": ("Chodba dole", PIR),
    "pirA3Prizemi:3.in4": ("Koupelna dole", PIR),
    "pirA3Prizemi:3.in5": ("Spajza", PIR),
    "pirA3Prizemi:3.in6": ("Zadveri dole", PIR),
}

# The thread group is non-greedy but must be followed by the level, so cron4j
# names containing nested brackets still match correctly.
LINE_RE = re.compile(
    r"^(?P<ts>\S+) \[(?P<thread>.+?)\] +[A-Z]+ +\[[^\]]+\] (?P<msg>.*)$"
)
EDGE_RE = re.compile(r"^input '(?P<pin>pin[A-D][0-7])' (?P<state>HIGH|LOW) ")
# "Executing OnInitActionBinding" is a post-init state replay, not real
# activity; the literal "Executing ActionBinding" below excludes it.
BINDING_RE = re.compile(
    r"^Executing ActionBinding: "
    r"(?P<key>pirA\d[A-Za-z]*:\d\.in[1-6])\("
    r"node\d+\.(?P<pin>pin[A-D][0-7])\)$"
)


def to_seconds(hms):
    h, m, s = (int(p) for p in hms.split(":"))
    return h * 3600 + m * 60 + s


def parse(lines, start, end):
    """Return (events, mismatches).

    events is a list of (time, sensor name, kind, active) tuples.
    """
    # Packet processing is multi-threaded and the two lines that make up one
    # event can be split by another thread's output -- so remember the last
    # edge per thread rather than globally.
    last_edge = {}
    events, mismatches = [], []

    for line in lines:
        m = LINE_RE.match(line)
        if not m:
            continue
        thread, msg = m.group("thread"), m.group("msg")

        edge = EDGE_RE.match(msg)
        if edge:
            last_edge[thread] = (edge.group("pin"), edge.group("state") == "HIGH")
            continue

        binding = BINDING_RE.match(msg)
        if not binding:
            continue
        sensor = SENSORS.get(binding.group("key"))
        if sensor is None:
            continue

        hms = m.group("ts")[11:19]
        if not (start <= hms <= end):
            continue

        name, kind = sensor
        pin, high = last_edge.get(thread, (None, None))
        if pin != binding.group("pin"):
            mismatches.append((hms, name, binding.group("pin"), pin))
            continue

        # PIR: logicalOneIsActivate=true  -> HIGH means motion.
        # Magnetic: logicalOneIsActivate=false -> LOW means occupied.
        events.append((hms, name, kind, high if kind == PIR else not high))

    return events, mismatches


def main():
    ap = argparse.ArgumentParser(
        description="Extract PIR / urinal sensor events from a home-controller app log."
    )
    ap.add_argument("logfile", nargs="?", help="log file (.gz ok); default stdin")
    ap.add_argument("--from", dest="start", default="00:00:00", metavar="HH:MM:SS")
    ap.add_argument("--to", dest="end", default="23:59:59", metavar="HH:MM:SS")
    ap.add_argument("--gaps", type=int, metavar="SECONDS",
                    help="also report quiet stretches longer than SECONDS")
    ap.add_argument("-q", "--quiet", action="store_true",
                    help="print only the summary, not every event")
    args = ap.parse_args()

    if args.logfile:
        opener = gzip.open if args.logfile.endswith(".gz") else open
        stream = opener(args.logfile, "rt", errors="replace")
    else:
        stream = sys.stdin

    activations = Counter()
    visits = {}          # urinal -> time it became occupied
    completed = []       # (urinal, occupied_at, flushed_at, seconds)
    occupied = Counter()
    flushes = Counter()
    event_times = []

    events, mismatches = parse(stream, args.start, args.end)
    for hms, name, kind, active in events:
        event_times.append(hms)
        if kind == PIR:
            label = "ACTIVATE" if active else "deactivate"
            if active:
                activations[name] += 1
        elif active:
            label = "occupied"
            occupied[name] += 1
            visits[name] = hms
        else:
            label = "LEFT -> FLUSH"
            flushes[name] += 1
            began = visits.pop(name, None)
            if began:
                completed.append((name, began, hms, to_seconds(hms) - to_seconds(began)))
        if not args.quiet:
            print(f"{hms}  {'PIR' if kind == PIR else 'URINAL':7}{name:22}{label}")

    print(f"\n===== SUMMARY {args.start} - {args.end} =====")
    print("-- PIR activations --")
    for name, count in activations.most_common():
        print(f"  {name:22} {count}")
    print(f"  {'TOTAL':22} {sum(activations.values())}")

    print("-- Urinals --")
    for key, (name, kind) in SENSORS.items():
        if kind == MAGNETIC:
            print(f"  {name:22} occupied={occupied[name]}  flushes={flushes[name]}")
    for name, began, ended, seconds in completed:
        print(f"    {name}: {began} -> {ended}  ({seconds} s)")

    if args.gaps:
        print(f"-- gaps longer than {args.gaps} s --")
        for earlier, later in zip(event_times, event_times[1:]):
            delta = to_seconds(later) - to_seconds(earlier)
            if delta > args.gaps:
                print(f"  {earlier} -> {later}   ({delta // 60} min {delta % 60:02d} s)")

    # A non-zero count means edge/binding pairing broke and the numbers above
    # cannot be trusted.
    print(f"pin mismatches: {len(mismatches)}")
    for hms, name, expected, got in mismatches:
        print(f"  {hms}  {name}  binding={expected} last_edge={got}")


if __name__ == "__main__":
    main()
