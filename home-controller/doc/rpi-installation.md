# Raspberry Pi installation guide

Tested on
* Raspberry Pi 4
* [Raspberry Pi OS Lite](https://www.raspberrypi.com/software/operating-systems/#raspberry-pi-os-64-bit),
Debian version: 12 (bookworm)
* January 2024

## Update OS and install software
```
sudo apt update
sudo apt upgrade
sudo apt install openjdk-17-jre python3-pip authbind samba samba-common-bin mpd
```

## Allow run on port 80
[source](http://www.2ality.com/2010/07/running-tomcat-on-port-80-in-user.html)  
setup `authbind` and allow access to port `80` to user `pi`
```
sudo touch /etc/authbind/byport/80
sudo chmod 500 /etc/authbind/byport/80
sudo chown pi /etc/authbind/byport/80
```
## Assign fixed USB port names
[source](https://www.freva.com/assign-fixed-usb-port-names-to-your-raspberry-pi/)  
Default mapping of USB devices to /dev/ttyUSB* is random and may change after boot.  
Expected devices:
  * `ttyUSB_CH340` - USB/Serial for PIC bridge (set in )
  * `ttyUSB_FT232R` - RS485 for Hvac control (set in options)  

list usb device command to find usable attributes to specify binding:
```
dmesg | grep ttyUSB
ls -l /dev/ttyUSB*
```
list properties of selected port
```
udevadm info --name=/dev/ttyUSB0 --attribute-walk
```
set rules
```
sudo nano /etc/udev/rules.d/10-usb-serial.rules
```
and put configuration like this:
```
SUBSYSTEM=="tty", ATTRS{idProduct}=="7523", ATTRS{idVendor}=="1a86", SYMLINK+="ttyUSB_CH340"
SUBSYSTEM=="tty", ATTRS{idProduct}=="6001", ATTRS{idVendor}=="0403", SYMLINK+="ttyUSB_FT232R"
```
apply and verify mapping
```
sudo udevadm trigger
ls -l /dev/ttyUSB*
```

## Install Home automation
Initial `/etc/init.d/homeAutomation` is [here](../../RaspberryPI/etc/init.d/), referenced bellow
```
sudo mkdir /usr/local/bin/homeAutomation/
sudo chmod 2777 /usr/local/bin/homeAutomation/
```
copy `/RaspberryPI/etc/init.d/homeAutomation` from this repo to `/etc/init.d/homeAutomation` on rpi
```
sudo chmod 755 /etc/init.d/homeAutomation
```
setup restart daemon and start the service
```
sudo chown root:root /etc/init.d/homeAutomation
sudo update-rc.d homeAutomation defaults
sudo systemctl restart homeAutomation.service
```

## Auto-mounted USB drives
[source](https://raspberrytips.com/mount-usb-drive-raspberry-pi/)  
list attached drives by:  
```
sudo ls -l /dev/disk/by-uuid/
```
make target folders like `/mnt/External4T` and bind them
```
sudo nano /etc/fstab
```
attach there config like this:
```
UUID=9C96890C9688E7DA /mnt/External4T ntfs    defaults,auto,users,rw,nofail 0 0
UUID=B640F12640F0EDCD /mnt/External1T ntfs    defaults,auto,users,rw,nofail 0 0
```
and apply:
```
sudo mount -a
```

## Samba server
[source](https://pimylifeup.com/raspberry-pi-samba/)
Set password for user `pi` and set password to prompt
```
sudo smbpasswd -a pi
```
Update samba config
```
sudo nano /etc/samba/smb.conf
```
by:
```
[External1T]
path = /mnt/External1T
writeable=Yes
create mask=0777
directory mask=0777
public=no

[External4T]
path = /mnt/External4T
writeable=Yes
create mask=0777
directory mask=0777
public=no
```

## MPD
Enable mpd service:
```
sudo systemctl enable --now mpd
```
copy your playlist to `/var/lib/mpd/playlists/` and update permissions
```
cp /tmp/cro1-128.mp3.m3u /var/lib/mpd/playlists/
sudo chmod 666 /var/lib/mpd/playlists/cro1-128.mp3.m3u
```

update mpd configuration to use external sound card and change server binding to all network cards
```
sudo nano /etc/mpd.conf
```
add `audio_input` and replace `bind_to_address`:
```
audio_output {
        type            "alsa"
        name            "Kuchyn"
        device          "front:CARD=carino,DEV=0"        # optional
        format          "44100:16:2"    # optional
        mixer_device    "default"       # optional
        mixer_control   "PCM"           # optional
        mixer_index     "0"             # optional
}

#replace existing value
bind_to_address         "any"
```
and restart mpd
```
sudo systemctl restart mpd.service
```