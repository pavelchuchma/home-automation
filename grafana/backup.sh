#!/usr/bin/env bash
#
# Consistent, versioned backup of the Grafana + InfluxDB Docker volumes to the
# external HDD via rsync.
#
# Strategy: briefly stop the stack (guarantees a consistent on-disk snapshot of
# InfluxDB TSM/WAL and Grafana SQLite), rsync the volumes into a timestamped
# snapshot dir using --link-dest against the previous one (unchanged files are
# hardlinked, so each daily snapshot costs almost no extra space), then start
# the stack again. Snapshots older than RETENTION_DAYS are pruned.
#
# Must run as root (Docker volumes are root-owned; -a preserves ownership).
# Intended to be run from cron. See README for the cron entry.
#
# Override defaults via env vars:
#   STACK_DIR=/home/pi/grafana BACKUP_ROOT=/mnt/External4T/backup/pi/grafana \
#   RETENTION_DAYS=14 ./backup.sh
#
set -euo pipefail

STACK_DIR="${STACK_DIR:-/home/pi/grafana}"
BACKUP_ROOT="${BACKUP_ROOT:-/mnt/External4T/backup/pi/grafana}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
VOL_BASE=/var/lib/docker/volumes
VOLUMES=(grafana_influxdb-data grafana_grafana-data grafana_influxdb-config)

log() { echo "$(date '+%F %T') $*"; }

# --- preflight ---
if [[ $EUID -ne 0 ]]; then
  echo "ERROR: must run as root (sudo) - Docker volumes are root-owned." >&2
  exit 1
fi

# Refuse to back up if the HDD is not mounted (otherwise we'd write the backup
# onto the SD card under the mountpoint and silently fill it up).
if ! mountpoint -q /mnt/External4T; then
  log "ERROR: /mnt/External4T is not mounted - aborting."
  exit 1
fi

mkdir -p "$BACKUP_ROOT"

STAMP="$(date +%Y-%m-%d_%H%M%S)"
SNAP="$BACKUP_ROOT/$STAMP"

# previous snapshot for hardlink dedup (if any)
LINK_DEST=""
if [[ -L "$BACKUP_ROOT/latest" ]]; then
  PREV="$(readlink -f "$BACKUP_ROOT/latest")"
  [[ -d "$PREV" ]] && LINK_DEST="$PREV"
fi

# absolute source paths
SRC=()
for v in "${VOLUMES[@]}"; do SRC+=("$VOL_BASE/$v"); done

cd "$STACK_DIR"

# Always bring the stack back up, even if rsync fails midway.
trap 'log "restarting stack (cleanup trap)"; docker compose start >/dev/null 2>&1 || true' EXIT

log "=== backup start -> $SNAP ==="
log "stopping stack for a consistent snapshot..."
docker compose stop

mkdir -p "$SNAP"
if [[ -n "$LINK_DEST" ]]; then
  log "rsync (incremental, hardlink dedup vs $(basename "$LINK_DEST"))"
  rsync -a --link-dest="$LINK_DEST/" "${SRC[@]}" "$SNAP/"
else
  log "rsync (first full snapshot)"
  rsync -a "${SRC[@]}" "$SNAP/"
fi

log "starting stack..."
docker compose start
trap - EXIT   # clean shutdown reached; drop the safety trap

# point 'latest' at this snapshot
ln -sfn "$SNAP" "$BACKUP_ROOT/latest"

# prune old snapshots (removing hardlinked files frees space correctly)
log "pruning snapshots older than ${RETENTION_DAYS} days..."
find "$BACKUP_ROOT" -maxdepth 1 -type d -name '20*' -mtime +"$RETENTION_DAYS" \
  -print -exec rm -rf {} +

log "=== backup done ($(du -sh "$SNAP" | cut -f1) in this snapshot) ==="
