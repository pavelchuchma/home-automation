#! /bin/bash
# Copies the home automation directory (cfg, logs, weather forecasts) to the external backup disk.
# No --delete: files removed from the SD card stay in the backup.
set -uo pipefail

src="${HA_DIR:-/usr/local/bin/homeAutomation}/"
destMount="${BACKUP_MOUNT:-/mnt/External4T}"
dest="$destMount/backup/pi/homeAutomation/"
marker="$destMount/backup/.backup_marker"

echo "=== $(date -Is) ==="

# The access to the marker triggers the automount; findmnt then confirms the real disk is mounted
# (with x-systemd.automount, mountpoint is true even when only the autofs placeholder is there).
if [ ! -f "$marker" ] || ! findmnt -n -t ext4 -M "$destMount" > /dev/null; then
    echo "ABORT: $destMount is not mounted or $marker is missing"
    exit 1
fi

mkdir -p "$dest"
nice -n 19 /usr/bin/rsync -a --stats \
    --exclude '/home-controller-*/' --exclude '/home-controller-*.tar' \
    "$src" "$dest"
