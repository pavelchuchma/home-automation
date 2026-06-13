#!/usr/bin/env bash
#
# Deploy / update the Grafana + InfluxDB stack on the Raspberry Pi via scp.
#
# Copies everything the stack needs to run (compose file, .env, Grafana
# provisioning) to the Pi. Safe to run repeatedly for updates.
#
# Usage:
#   ./deploy.sh            # copy files only
#   ./deploy.sh --up       # copy files, then `docker compose up -d` on the Pi
#
# Override defaults via env vars:
#   PI_HOST=pi@pi.local REMOTE_DIR='~/grafana' ./deploy.sh
#
set -euo pipefail

PI_HOST="${PI_HOST:-pi@pi.local}"
REMOTE_DIR="${REMOTE_DIR:-~/grafana}"

# directory this script lives in = local source of truth (the repo dir)
LOCAL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$LOCAL_DIR"

# items the running stack needs (relative to this dir)
FILES=(docker-compose.yml .env backup.sh)
DIRS=(grafana)   # contains grafana/provisioning/{datasources,dashboards}

# --- preflight ---
if [[ ! -f .env ]]; then
  echo "ERROR: .env not found in $LOCAL_DIR" >&2
  echo "       Create it first:  cp .env.example .env  (then fill in secrets)" >&2
  exit 1
fi

echo "==> Deploying to ${PI_HOST}:${REMOTE_DIR}"

# ensure the remote target directory exists
ssh "$PI_HOST" "mkdir -p ${REMOTE_DIR}"

# copy top-level files
echo "--> files: ${FILES[*]}"
scp -p "${FILES[@]}" "${PI_HOST}:${REMOTE_DIR}/"

# copy directories recursively (provisioning tree)
for d in "${DIRS[@]}"; do
  echo "--> dir:   ${d}/"
  scp -rp "$d" "${PI_HOST}:${REMOTE_DIR}/"
done

echo "==> Copy done."

# --- optional: bring the stack up / apply changes ---
if [[ "${1:-}" == "--up" ]]; then
  echo "==> Starting / updating stack on the Pi..."
  ssh "$PI_HOST" "cd ${REMOTE_DIR} && docker compose up -d"
  ssh "$PI_HOST" "cd ${REMOTE_DIR} && docker compose ps"
else
  echo "    Next: ssh ${PI_HOST} 'cd ${REMOTE_DIR} && docker compose up -d'"
  echo "    (or re-run: ./deploy.sh --up)"
fi
