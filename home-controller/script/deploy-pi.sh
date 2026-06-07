#!/bin/bash

# Get the directory of the script
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

# Configuration
# Pick whichever hostname currently resolves and answers on port 22 — `pi` over VPN, `pi.local` on LAN.
host=
for candidate in pi pi.local; do
    if nc -z -w 2 "$candidate" 22 2>/dev/null; then
        host=$candidate
        break
    fi
done
if [ -z "$host" ]; then
    echo "Neither 'pi' nor 'pi.local' is reachable on port 22." >&2
    exit 1
fi
echo "Using host: $host"
version=0.1.0

# Paths
BUILD_DIR="$SCRIPT_DIR/../app/build/distributions"
DEPLOY_DIR="/usr/local/bin/homeAutomation"

# Deployment
scp "$BUILD_DIR/home-controller-$version.tar" "$SCRIPT_DIR/runJar-debug.sh" "pi@$host:$DEPLOY_DIR/" || exit
ssh -l pi "$host" < "$SCRIPT_DIR/finish-deploy.sh"
