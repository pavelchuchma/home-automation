#! /bin/bash
# Downloads the current weather forecast from met.no and Aladin into out/weather/.
# Runs hourly from cron on the Pi; the location is read from cfg/app.properties (location.*).
set -uo pipefail

baseDir="${HA_DIR:-/usr/local/bin/homeAutomation}"
weatherDir="${WEATHER_DIR:-$baseDir/out/weather}"
properties="$baseDir/cfg/app.properties"

prop() {
    grep -E "^$1=" "$properties" | tail -1 | cut -d= -f2- | tr -d '[:space:]'
}

lat=$(prop location.latitude)
lon=$(prop location.longitude)
alt=$(prop location.altitude)
if [ -z "$lat" ] || [ -z "$lon" ] || [ -z "$alt" ]; then
    echo "$(date -Is) location.latitude/longitude/altitude missing in $properties" >&2
    exit 1
fi
# met.no asks for at most 4 decimals (~10 m), finer coordinates defeat its cache.
lat=$(LC_ALL=C printf '%.4f' "$lat")
lon=$(LC_ALL=C printf '%.4f' "$lon")
mkdir -p "$weatherDir"

printf -v fileName '%(%Y%m%d-%H_%M_%S)T' -1

# Save only a successful, non-empty response.
download() {
    local target="$weatherDir/${fileName}-$1"
    if curl -fsS --max-time 60 "$2" > "$target.tmp" && [ -s "$target.tmp" ]; then
        mv "$target.tmp" "$target"
    else
        echo "$(date -Is) download of $1 failed" >&2
        rm -f "$target.tmp"
    fi
}

download met-no "https://api.met.no/weatherapi/locationforecast/2.0/complete?lat=$lat&lon=$lon&altitude=$alt"
download aladin "https://aladinonline.oblacno.cz/get_data.php?latitude=$lat&longitude=$lon"
