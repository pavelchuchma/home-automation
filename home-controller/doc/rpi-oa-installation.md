# Raspberry Pi installation guide (fresh install with Java 21)

Tested on
* Raspberry Pi 4 Model B
* [Raspberry Pi OS Lite (64-bit)](https://www.raspberrypi.com/software/operating-systems/#raspberry-pi-os-64-bit),
Debian version: 13 (trixie), kernel 6.18
* OpenJDK 21
* September 2026

Differences from [rpi-installation.md](rpi-installation.md):
* Java 21 comes straight from the Debian repository (`openjdk-21-jre-headless`)
* the application runs as a native systemd service that starts `runJar-debug.sh`, the `/etc/init.d` script is kept
  only so `finish-deploy.sh` can restart it
* remote access over WireGuard, deploy by `script/deploy-pioa.sh`

## Backup the old installation
Keep the old SD card untouched as a rollback, or image it first.

### Configuration backup (over SSH)
Stream a tarball of everything worth keeping from the running Pi:
```
ssh pi@<rpi> 'sudo tar -czf - --numeric-owner \
  --exclude=/home/pi/.bash_history \
  /usr/local/bin/homeAutomation /etc/init.d/homeAutomation /etc/authbind \
  /etc/udev/rules.d /etc/wireguard /etc/ssh /home/pi \
  /var/spool/cron/crontabs /etc/sudoers.d /boot/config.txt /boot/cmdline.txt \
  /etc/hostname /etc/hosts /etc/dhcpcd.conf' > rpi-backup-$(date +%y%m%d).tar.gz
```
The tarball holds private keys (WireGuard, SSH host keys) and passwords from `app.properties`, keep it private.

Most important parts:
* `/usr/local/bin/homeAutomation/cfg/` - `app.properties` and `app-state.properties`
* `/etc/wireguard/` - reusing the same key means no change on the WireGuard hub
* `/etc/ssh/ssh_host_*` - reusing them keeps the SSH fingerprint, clients do not complain
* `/home/pi/.ssh/authorized_keys`
* `/etc/udev/rules.d/10-usb-serial.rules`

### Full SD card image (on macOS)
`dd` from `/dev/rdiskN` fails with `Operation not permitted` even under `sudo`, unless the terminal app has
*Full Disk Access*. The simplest way is Disk Utility:
1. *View → Show All Devices*, select the whole SD card device (not just the `bootfs` volume)
2. *File → New Image → Image from "…"*, format **DVD/CD master**, no encryption → raw `.cdr` image
3. compress it into a format Raspberry Pi Imager can restore:
```
xz -T0 -6 -k -c sd-card.cdr > rpi-sd.img.xz && xz -t rpi-sd.img.xz
```

## Write the new OS
Use [Raspberry Pi Imager](https://www.raspberrypi.com/software/):
* Device: Raspberry Pi 4, OS: *Raspberry Pi OS (other) → Raspberry Pi OS Lite (64-bit)*
* OS customisation:
  * hostname (e.g. `pioa`)
  * user `pi` + password (the `pi` user is no longer created by default)
  * time zone `Europe/Prague`
  * no Wi-Fi when connected by cable
  * SSH enabled, public-key authentication only, paste the content of the old `authorized_keys`

The Ethernet MAC address is fixed in hardware, so a DHCP reservation on the router keeps the same IP address
after reinstall.

## Passwordless sudo
Unlike older images, the new OS asks for a password on `sudo`. Log in once interactively and run:
```
echo "pi ALL=(ALL) NOPASSWD: ALL" | sudo tee /etc/sudoers.d/010_pi-nopasswd
sudo chmod 440 /etc/sudoers.d/010_pi-nopasswd
sudo visudo -c
```

## Update OS and install software
```
sudo apt update
sudo apt full-upgrade
sudo apt install openjdk-21-jre-headless wireguard-tools authbind
```
Allow port 80 to user `pi` by `authbind` as described in
[rpi-installation.md](rpi-installation.md#allow-run-on-port-80).
Add `mpd`, `samba` etc. only if the Pi runs them, see [rpi-installation.md](rpi-installation.md).

## Restore configuration
Upload and extract the backup into a temporary folder. Do not extract it over `/`, config files like
`sshd_config`, `dhcpcd.conf` or `cmdline.txt` from an older release do not fit the new one.
```
mkdir /tmp/restore
sudo tar -xzpf /tmp/rpi-backup-*.tar.gz -C /tmp/restore --numeric-owner
```
Run the following commands as root (`sudo bash`), `/etc/wireguard` in the backup is readable by root only,
so globs fail under `pi`.

### WireGuard
```
install -d -m 700 /etc/wireguard
cp -p /tmp/restore/etc/wireguard/* /etc/wireguard/
systemctl enable --now wg-quick@wg0
wg show wg0
```

### SSH host keys
DSA keys are not supported by OpenSSH on trixie any more, skip them.
```
for t in rsa ecdsa ed25519; do
  cp -p /tmp/restore/etc/ssh/ssh_host_${t}_key /tmp/restore/etc/ssh/ssh_host_${t}_key.pub /etc/ssh/
done
systemctl restart ssh
```
If you accepted the temporary host key of the fresh install on a client, replace it there:
```
ssh-keygen -R <rpi>
```
and verify the fingerprint on the next login matches the old one.

### Fixed USB port names
Restore `10-usb-serial.rules` (see [rpi-installation.md](rpi-installation.md#assign-fixed-usb-port-names)
for how to create it from scratch):
```
cp /tmp/restore/etc/udev/rules.d/10-usb-serial.rules /etc/udev/rules.d/
udevadm control --reload
udevadm trigger --subsystem-match=tty
ls -l /dev/ttyUSB*
```

## Install Home automation
```
install -d -o pi -g pi -m 755 /usr/local/bin/homeAutomation
cp -a /tmp/restore/usr/local/bin/homeAutomation/cfg /tmp/restore/usr/local/bin/homeAutomation/out /usr/local/bin/homeAutomation/
cp /tmp/restore/etc/init.d/homeAutomation /etc/init.d/
chmod 755 /etc/init.d/homeAutomation
```
The init script is used only as a restart entry point for `finish-deploy.sh`. It sources `/lib/lsb/init-functions`,
which redirects `/etc/init.d/homeAutomation restart` to `systemctl restart homeAutomation.service` once the native
unit below exists. Do not run `update-rc.d` for it.

### systemd service
Create `/etc/systemd/system/homeAutomation.service`:
```
[Unit]
Description=HomeAutomation controller
Wants=network-online.target
After=network-online.target

[Service]
User=pi
ExecStart=/usr/local/bin/homeAutomation/runJar-debug.sh
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```
`runJar-debug.sh` keeps doing what it did before: creates the `/dev/ttyS80` alias, sets `JAVA_OPTS` (heap limit)
and starts the app under `authbind`.

Enable it:
```
systemctl daemon-reload
systemctl enable homeAutomation
```

### Deploy the application
Deploy from the development machine the same way as to the other Pis, over WireGuard:
```
script/deploy-pioa.sh
```
It uploads `home-controller-<version>.tar` and `runJar-debug.sh`, then `finish-deploy.sh` extracts the tar and
restarts the service. Check the log:
```
journalctl -u homeAutomation -f
```

## Verify
```
systemctl is-active homeAutomation wg-quick@wg0
sudo ss -tlnp | grep java              # ports 80 and 6070 (JDWP)
ls -l /dev/ttyUSB_CH340 /dev/ttyS80
curl -s -o /dev/null -w "%{http_code}\n" http://localhost/
grep "Node1\[Bridge\] succeeded" /usr/local/bin/homeAutomation/out/app.log
```
Reboot once and check again that everything starts by itself.

Occasional `Read failed` / `CRC FAILED` errors on the serial line are normal. When the Pi is not connected to the
bus, only the bridge (Node1) answers and all other nodes report `No response`.

Clean up, the extracted backup contains private keys:
```
sudo rm -rf /tmp/restore /tmp/rpi-backup-*.tar.gz
```
