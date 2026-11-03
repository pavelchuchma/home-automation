#! /bin/bash
# Packs forecast files of finished months into weather-archive/weather-YYYYMM.tar.gz
# and deletes the originals once the archive is verified.
set -euo pipefail

weatherDir="${WEATHER_DIR:-/usr/local/bin/homeAutomation/out/weather}"
archiveDir="${ARCHIVE_DIR:-/usr/local/bin/homeAutomation/out/weather-archive}"
currentMonth=$(date +%Y%m)
mkdir -p "$archiveDir"

for month in $(ls "$weatherDir" | grep -E "^[0-9]{8}-" | cut -c1-6 | sort -u); do
    [ "$month" -ge "$currentMonth" ] && continue
    archive="$archiveDir/weather-$month.tar.gz"
    work=$(mktemp -d)
    trap "rm -rf $work" EXIT

    # Merge with an existing archive so an earlier partial one is extended, not replaced.
    [ -f "$archive" ] && tar -xzf "$archive" -C "$work"
    ls "$weatherDir" | grep "^$month" > "$work.list"
    xargs -a "$work.list" -I{} cp -p "$weatherDir/{}" "$work/"

    (cd "$work" && ls | nice -n 19 tar -czf "$archive.tmp" -T -)
    gzip -t "$archive.tmp"
    expected=$(ls "$work" | wc -l)
    archived=$(tar -tzf "$archive.tmp" | wc -l)
    if [ "$expected" -ne "$archived" ]; then
        echo "$(date -Is) $month: archive has $archived files, expected $expected - keeping originals" >&2
        rm -f "$archive.tmp" "$work.list"; rm -rf "$work"
        continue
    fi
    mv "$archive.tmp" "$archive"
    (cd "$weatherDir" && xargs -a "$work.list" rm -f --)
    echo "$(date -Is) $month: archived $archived files, removed $(wc -l < "$work.list") originals"
    rm -f "$work.list"; rm -rf "$work"
done
