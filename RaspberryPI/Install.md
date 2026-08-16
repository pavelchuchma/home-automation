# Raspberry Pi Installation & Configuration Guide

Manual configuration steps applied on top of a stock Raspberry Pi OS image
(target: Pi 4 Model B, 2 GB RAM, Bookworm) so the machine can be rebuilt from
scratch.

Only steps that must be done **by hand on the Pi** belong here. Anything
deployed from a repo (home-controller distribution and its JVM options, the
Grafana/InfluxDB compose stack and its container limits) is configuration-as-code
in git and is installed by the respective `deploy*.sh` scripts — do not
duplicate it in this document, and do not edit those copies on the Pi.

> Scope note: this guide currently covers only the memory-management hardening
> added after the August 2026 incident. It will be extended into a complete
> installation guide (base image, OS setup, Samba, MPD, Docker, cron jobs, ...).

## Background — why the memory tuning exists

On 2026-08-03 the Pi degraded into a "livelock": a large Samba file transfer
onto the USB-attached disk filled the page cache with dirty pages while
InfluxDB + Grafana + the home-controller JVM already occupied most of the
1.34 GB usable RAM. The kernel never reached the OOM-killer threshold; instead
it kept reclaiming executable pages, so every *new* process (sshd, cron jobs,
Docker health checks) took minutes to hours to start while long-running hot
processes kept working. Only a power cycle recovered the system.

The steps below give the kernel room to breathe (zram), stop bulk writes from
eating RAM (dirty limits), add a guard that kills a hog before the system
freezes (earlyoom), and make container memory limits actually enforceable
(memory cgroup).

## 1. zram swap

zram creates a compressed block device in RAM used as swap. Swapped-out pages
are compressed (~3:1 for typical data) instead of written to the SD card:
swap-in/out latency drops from SD-card milliseconds to CPU microseconds, and
the SD card suffers no write wear. With `PERCENT=25` on a 2 GB Pi the swap
device advertises ~460 MB and physically costs ~150 MB RAM at 3:1 compression.

```bash
sudo apt install zram-tools
```

Set `/etc/default/zramswap` to:

```
ALGO=zstd
PERCENT=25
```

Disable the legacy 200 MB SD-card swap file (zram replaces it):

```bash
sudo systemctl disable --now dphys-swapfile
sudo systemctl restart zramswap
```

Verify:

```bash
swapon --show     # expect /dev/zram0, ~460M, prio 100; no /var/swap
zramctl           # shows algorithm, compressed vs. uncompressed size
```

Note: zram is not extra RAM — the compressed pool itself consumes memory, and
incompressible pages compress ~1:1. It widens the safety margin; it does not
remove the need for the OOM guard in section 3.

## 2. Dirty page cache limits

By default Linux allows dirty (written but not yet flushed) file data to grow
to 20% of RAM (~270 MB on this Pi) before throttling writers. A bulk write
over Samba onto a slow USB disk therefore floods RAM with dirty pages faster
than the disk drains them — this was the direct trigger of the 2026-08-03
incident.

Create `/etc/sysctl.d/99-writeback.conf`:

```
# Cap dirty page cache so bulk writes to slow USB disks cannot exhaust RAM.
# Background writeback starts at 32 MB; writers are blocked at 128 MB.
vm.dirty_background_bytes = 33554432
vm.dirty_bytes = 134217728
```

Apply without reboot:

```bash
sudo sysctl --system
```

Verify:

```bash
sysctl vm.dirty_background_bytes vm.dirty_bytes
# during a large transfer, "Dirty:" in /proc/meminfo should stay <= ~128 MB
grep Dirty /proc/meminfo
```

Trade-off: sequential bulk writes are throttled earlier, so a huge transfer
takes somewhat longer end-to-end. Interactive responsiveness of the rest of
the system is preserved, which is the priority on this machine.

## 3. earlyoom — userspace OOM guard

When memory pressure builds up gradually, the kernel OOM killer may never
fire: the system livelocks (reclaiming executable pages over and over) while
technically still having a few free pages. `earlyoom` watches free memory +
swap from userspace and SIGTERMs (then SIGKILLs) the process with the highest
memory usage before the machine becomes unresponsive.

```bash
sudo apt install earlyoom
```

Set `/etc/default/earlyoom` to:

```
EARLYOOM_ARGS="-m 5 -s 10 --prefer (^|/)(influxd|rsync)$ --avoid (^|/)(home-controller|java|sshd|systemd|init)$"
```

- `-m 5 -s 10` — act when free RAM < 5% and free swap < 10%.
- `--prefer` — sacrifice InfluxDB (Docker restarts it automatically thanks to
  `restart: unless-stopped`) or a running rsync backup (re-runs next night)
  first.
- `--avoid` — never kill the home-controller JVM, sshd, or systemd.
- Do NOT put shell quotes around the regexes: systemd expands
  `$EARLYOOM_ARGS` without shell quote processing, so quotes would become
  literal characters. The regexes contain no whitespace, so none are needed.

```bash
sudo systemctl enable --now earlyoom
sudo systemctl restart earlyoom
```

Verify — the startup lines must echo both regexes back:

```bash
systemctl is-active earlyoom
journalctl -u earlyoom --no-pager | grep -iE "Preferring|avoid killing"
#   Preferring to kill process names that match regex '(^|/)(influxd|rsync)$'
#   Will avoid killing process names that match regex '(^|/)(home-...)$'
```

## 4. Memory cgroup controller

Raspberry Pi OS ships with the **memory cgroup controller disabled** to save a
little RAM. Without it, container memory limits are silently ignored — a
`docker compose up` only prints a warning that is easy to miss ("Your kernel
does not support memory limit capabilities or the cgroup is not mounted.
Limitation discarded.") and `docker inspect <name> --format '{{.HostConfig.Memory}}'`
reports `0`.

Check first:

```bash
cat /sys/fs/cgroup/cgroup.controllers   # must contain "memory"
```

If `memory` is absent, append the two parameters to the **single line** in
`/boot/firmware/cmdline.txt` (keep everything on one line — a stray newline
makes the Pi unbootable, so back the file up first):

```bash
sudo cp /boot/firmware/cmdline.txt /boot/firmware/cmdline.txt.bak
sudo sed -i 's/$/ cgroup_enable=memory cgroup_memory=1/' /boot/firmware/cmdline.txt
cat /boot/firmware/cmdline.txt          # sanity-check: still ONE line
```

**A reboot is required.** After the reboot:

```bash
cat /sys/fs/cgroup/cgroup.controllers   # now includes "memory"
```

Then **force-recreate the containers**. Docker discards a memory limit at
container *creation* time, so containers created while the controller was
missing keep `Memory=0` forever — and a plain `docker compose up -d` will not
fix them: the compose file has not changed, so its config hash still matches
and compose reports the containers as up to date (verified with
`docker compose up --dry-run -d`, which prints "Running" instead of
"Recreate"). Restarting them, or rebooting again, does not help either.

```bash
cd ~/grafana && docker compose up -d --force-recreate
docker inspect influxdb grafana --format '{{.Name}} Memory={{.HostConfig.Memory}}'
# expect 419430400 (400m) for both containers, not 0
docker stats --no-stream    # LIMIT column shows the configured caps
```

Cost: memory accounting adds roughly 1% RAM overhead — negligible compared to
having an unbounded database container.

## 5. Samba change notification

`smbd` spawns a helper process, `smbd-notifyd`, which keeps the entire
change-notify database **in memory** (no tdb on disk) — one record per watched
path per client. macOS Finder registers recursive watches over whatever it has
open, and an inotify watch also fires for changes made *locally*, outside SMB,
so a nightly rsync across a watched tree floods notifyd too. With the large
media trees on the `External*` shares the record count explodes.

Two things then compound. Samba allocates those records through talloc in many
small chunks, so they all come from the `brk` heap rather than `mmap`; glibc can
only shrink a `brk` heap when its *top* is free, so a single live record pins
everything below it. The peak therefore becomes permanent — the process never
gives the memory back, even after every client disconnects.

Measured on 2026-08-16, 12 days after the previous restart, with **zero clients
connected**:

```
VmHWM:  1306664 kB    # peak RSS = 1.28 GB on a 1.8 GB host
VmRSS:   813556 kB    # still held, flat over 90 s - not a running leak
VmSwap:  409748 kB    # occupying the entire zram device
```

The 813 MB was pure `[heap]` (confirmed via `/proc/<pid>/smaps`), which is what
had filled zram to 100% and left the host with no reclaimable headroom.

Fix — disable change notify in the `[global]` section of `/etc/samba/smb.conf`:

```
   change notify = no
```

```bash
sudo cp /etc/samba/smb.conf /etc/samba/smb.conf.bak-$(date +%Y%m%d-%H%M%S)
sudo testparm -s                     # must load cleanly before restarting
sudo systemctl restart smbd
```

Verify:

```bash
testparm -s -v 2>/dev/null | grep -i 'change notify'   # change notify = No
ps -eo pid,rss,comm | grep notifyd                     # ~12 MB, and stays there
```

Trade-off: Finder/Explorer no longer auto-refresh when files change on the
server and need a manual refresh. On a home NAS that is an acceptable price for
roughly 800 MB of RAM. Note that `man smb.conf` says of this parameter *"You
should never need to change this parameter"* — that advice assumes a host with
enough RAM, not a 1.8 GB Pi where the helper claimed two thirds of it.

Alternatives considered and rejected: `MemoryMax=` on `smbd.service` caps the
whole slice, so hitting the limit could OOM-kill the main `smbd` mid-transfer
rather than notifyd; a periodic `systemctl restart smbd` treats the symptom and
still lets the peak recur between restarts.

Not yet verified: that notifyd stays small **during** a large transfer. The
mechanism says it should, since there is nothing left to register, but this
needs confirming after the next bulk copy or nightly backup.

## 6. Grafana container memory limit

The 250m cap originally given to Grafana in `grafana/docker-compose.yml` was
too tight for Grafana 13, which runs unified storage, a bleve search index and
an authz-grpc-server. The Go heap alone filled the cap:

```
memory.current  262021120 / max 262144000    # 99.95% of the limit
anon            239 MB of the 250 MB         # ~17 MB left for page cache
memory.events   max = 51758                  # times the cgroup hit its ceiling
```

With no room for page cache the kernel kept evicting and re-reading Grafana's
own binary from the SD card: ~280 major faults/s and a sustained ~40 MB/s read
on `mmcblk0`, which pushed host iowait to 55–78% and load to ~6.8 while the CPU
sat idle. Dashboard queries took minutes and returned HTTP 400 with
`context canceled`.

Note the failure mode: a `mem_limit` that is merely *too small* does not
produce a clean OOM kill and restart. The container never gets killed — it
livelocks inside its own cgroup and hammers the SD card indefinitely. Check
`memory.events` (field `max`) rather than assuming a non-zero `oom_kill` is the
only sign of trouble:

```bash
CG=$(docker inspect grafana --format '{{.Id}}')
cat /sys/fs/cgroup/system.slice/docker-$CG.scope/memory.events
```

Raised to 400m (`mem_limit` in the compose file, which is configuration-as-code
— edit it in git and redeploy, not on the Pi). Grafana then settles at ~270 MB
of 400 MB, `memory.events max` stays at 0 and major faults drop to 0.

## Change log

| Date | Change | Reason |
|------|--------|--------|
| 2026-08-04 | Applied and verified all of §1–§4: zram swap, dirty page limits, earlyoom, memory cgroup controller (rebooted; containers force-recreated, limits now enforced at 400m/250m) | Post-mortem of the 2026-08-03 memory livelock |
| 2026-08-16 | §5 `change notify = no` in smb.conf + smbd restart; §6 Grafana `mem_limit` 250m → 400m and container force-recreated | `smbd-notifyd` held 813 MB RSS + 410 MB swap (1.28 GB peak) with no clients connected, filling zram to 100%; Grafana thrashed inside its 250m cgroup at ~40 MB/s off the SD card. Available RAM 218 MB → 1051 MB, load 6.8 → 0.8, iowait 55–78% → 0–2% |

## Open hardware issue

The power supply reports `Undervoltage detected!` during the boot current
spike (seen on the 2026-08-04 morning boots; `vcgencmd get_throttled` returned
`0x50000`). It caused a failed boot — red LED only, no green activity — while
two USB disks were attached and spinning up. Replace the PSU with an official
5.1 V / 3 A unit (and its cable), and power USB disks from a self-powered hub.
Unrelated to the memory livelock above.
