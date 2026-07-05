# MPD (Music Player Daemon) on the Pi

MPD streams internet radio to the kitchen speakers on the Raspberry Pi
(`pi.local`). Initial installation is covered in
[rpi-installation.md](rpi-installation.md#mpd); this document describes the
current runtime setup and how to troubleshoot it.

## Hardware

- **DAC / speakers:** Denon CEOL carino, connected over USB. It enumerates as
  ALSA card `carino` (USB-Audio, `idVendor=154e idProduct=1004`).
- The Denon is an all-in-one hi-fi unit: for sound to come out it must be
  powered on, switched to its USB/PC input, and at a non-zero volume — none of
  which the Pi can control or observe.

## Service

- Runs as the systemd unit `mpd.service` (user `mpd`), listening on the default
  MPD port `6600`, bound to all interfaces (`bind_to_address "any"`).
- Log: `/var/log/mpd/mpd.log`.
- `mpc` is **not** installed; drive/query MPD directly over the control port
  (see below).

## Application control (home-controller)

The radio is driven from the app by the class
[`MpdRadio`](../extensions/src/main/java/org/chuma/homecontroller/extensions/external/MpdRadio.java)
(module `extensions`, package `...external`). It connects to the MPD server over
the network (via the `javampd` library) and on `start()` clears the queue, adds
the single configured stream, and calls play; `stop()` stops the player. It is
wired up as a
[`RadioOnOffActor`](../extensions/src/main/java/org/chuma/homecontroller/extensions/actor/RadioOnOffActor.java)
in
[`PiConfigurator`](../app/src/main/java/org/chuma/homecontroller/app/configurator/PiConfigurator.java)
(search for `new RadioOnOffActor`).

Its configuration comes from two app properties:

| Property               | Meaning                     | Current value                                    |
|------------------------|-----------------------------|--------------------------------------------------|
| `mpd.radio.ip`         | IP of the MPD server (Pi)   | `pi.local`                                       |
| `mpd.radio.stream.url` | Radio stream added to queue | `https://rozhlas.stream/radiozurnal_mp3_128.mp3` |

Defaults live in
[`app/src/main/resources/default-app.properties`](../app/src/main/resources/default-app.properties)
and are overridden per-deployment in `cfg/app.properties`. This is the app-side
config; it is separate from the MPD server's own `/etc/mpd.conf` on the Pi
(below).

## Configuration (`/etc/mpd.conf`)

Key non-default settings currently in use:

```
music_directory     "/var/lib/mpd/music"
playlist_directory  "/var/lib/mpd/playlists"
log_file            "/var/log/mpd/mpd.log"
user                "mpd"
bind_to_address     "any"

audio_output {
        type            "alsa"
        name            "Kuchyn"
        device          "front:CARD=carino,DEV=0"
        format          "44100:16:2"
        mixer_device    "default"
        mixer_control   "PCM"
        mixer_index     "0"
}
```

Notes:

- The ALSA output is addressed **by card name** (`CARD=carino`), not by index,
  so it survives the card getting a different index across reboots/replugs.
- `format "44100:16:2"` matches the DAC's native S16_LE / 44100 / stereo mode.
- `mixer_device "default"` points at the ALSA `default` card (usually the
  onboard one), not at `carino`. The MPD `volume:` therefore scales a different
  mixer than the DAC; the DAC's own `PCM` level is set with
  `amixer -c carino set PCM ...`.

## Control / inspection over port 6600

Read-only status, outputs and current queue:

```
printf 'status\noutputs\nplaylistinfo\nclose\n' | nc -w2 localhost 6600
```

Start / stop playback and clear a latched error:

```
printf 'clearerror\nplay\nclose\n' | nc -w2 localhost 6600
printf 'stop\nclose\n'             | nc -w2 localhost 6600
```

Confirm audio actually reaches the DAC (independent of whether you can hear it):

```
cat /proc/asound/card3/pcm0p/sub0/status      # expect: state: RUNNING
cat /proc/asound/card3/pcm0p/sub0/hw_params   # expect: S16_LE, 2ch, 44100
```

(The `card3` index is whatever `carino` currently has in `/proc/asound/cards`.)

## Troubleshooting: no sound after reconnecting USB speakers

**Symptom:** MPD reports `state: play` but the speakers stay silent. The log
repeats:

```
ALSA lib (snd_config_expand) Unknown parameters CARD=carino,DEV=0
ALSA lib (snd_pcm_open_noupdate) Unknown PCM front:CARD=carino,DEV=0
exception: Failed to open "Kuchyn" (alsa); Failed to open ALSA device
  "front:CARD=carino,DEV=0": Invalid argument
```

Two independent causes, in order of likelihood:

1. **Flaky USB link → device stuck in a reset loop.** A badly seated cable,
   wrong port, or an underpowered hub makes the DAC re-enumerate continuously,
   so the ALSA card blinks in and out and MPD can't open it.
   ```
   sudo dmesg -T | grep -i "reset full-speed"      # storms of resets on one port
   grep -i carino /proc/asound/cards               # card present / blinking?
   ```
   Fix: reseat / swap the USB cable, plug the DAC directly into a powered port
   (not through a flaky hub) until `dmesg` stops logging resets.

2. **Stale ALSA config in the long-running MPD daemon.** Even after the device
   is stable, the MPD process keeps the ALSA config it cached at startup; after
   a USB hot-replug it no longer matches, so every open fails with
   "Unknown PCM". The tell-tale sign: a **fresh** process opens the exact same
   PCM fine while the daemon keeps failing:
   ```
   # succeeds from any fresh process (even as the mpd user, even inside mpd's
   # mount namespace) — proves the device/config/permissions are all fine:
   aplay -D "front:CARD=carino,DEV=0" --dump-hw-params /dev/zero
   ```
   Fix — restart MPD so alsa-lib reloads its config:
   ```
   sudo systemctl restart mpd
   ```

**After the fix**, verify with the `/proc/asound/.../status` check above: if the
substream is `RUNNING` but it is still silent, the problem is on the Denon side
(off, wrong input, or volume down), not on the Pi.
