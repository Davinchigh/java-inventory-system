#!/usr/bin/env sh
# Builds (if needed) and opens the inventory app.
set -e
cd "$(dirname "$0")"
[ -f build/inventory.jar ] || ./build.sh
java -jar build/inventory.jar "$@"
