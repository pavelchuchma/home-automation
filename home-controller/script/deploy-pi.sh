#!/bin/bash

# Get the directory of the script
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

# Configuration
host=pi.local
version=0.1.0

# Paths
BUILD_DIR="$SCRIPT_DIR/../app/build/distributions"
DEPLOY_DIR="/usr/local/bin/homeAutomation"

# Deployment
scp "$BUILD_DIR/home-controller-$version.tar" "$SCRIPT_DIR/runJar-debug.sh" "pi@$host:$DEPLOY_DIR/" || exit
ssh -l pi "$host" < "$SCRIPT_DIR/finish-deploy.sh"
