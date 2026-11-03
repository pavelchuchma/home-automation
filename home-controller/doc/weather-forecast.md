# Weather forecast collection

The Pi downloads a weather forecast every hour and keeps the history, so it can later
be matched against real PV production (see the `sunCalculator-wip` branch,
`WeatherForecast.java` parses the met.no format). The application itself does not
use the forecast yet.

## Scripts

Both live in `RaspberryPI/usr/local/bin/homeAutomation/` at the repo root (a mirror of
the Pi file system) and are installed by hand to the same path on the Pi; the deploy
scripts do not copy them. Installation steps: `RaspberryPI/Install.md`.

- `downloadForecast.sh` — saves one file per source and run into `out/weather/`:
  - `YYYYMMDD-HH_MM_SS-met-no` — [met.no Locationforecast 2.0](https://api.met.no/weatherapi/locationforecast/2.0/documentation)
    `complete`, hourly steps ~9 days ahead; cloud cover in percent.
  - `YYYYMMDD-HH_MM_SS-aladin` — Aladin model from aladinonline.oblacno.cz; the model runs
    every 6 hours, so consecutive hourly files often hold the same run; cloud cover as 0–1.

  The location comes from `location.latitude`, `location.longitude` and
  `location.altitude` in `cfg/app.properties`. A failed or empty download is not saved.
- `archiveForecast.sh` — packs every finished month into
  `out/weather-archive/weather-YYYYMM.tar.gz` and deletes the originals once the archive
  is verified. An existing archive of the same month is extended, not replaced, and
  missed months are caught up on the next run. The current month stays in `out/weather/`.

## Cron (user `pi`)

```
42 * * * * /usr/local/bin/homeAutomation/downloadForecast.sh
15 1 1 * * /usr/local/bin/homeAutomation/archiveForecast.sh >> /usr/local/bin/homeAutomation/out/weather-archive/archive.log 2>&1
```

## Data notes

- History starts on 2025-03-16.
- Files collected before the scripts were added to git may be empty (failed downloads
  were saved as-is); skip zero-size entries when reading the archives.
