# Grafana dashboards

JSON of the dashboards that visualise what `MetricsReporter` writes to InfluxDB
(`doc/metrics.md`). They live here because the panels are tied to the measurement and
field names in `extensions/.../metrics/`; a field renamed there breaks a panel here, and
the two should move together.

- **Grafana:** <http://pi.local:3000> (stack in `~/work/HomeAutomation/grafana/`, the
  InfluxDB datasource is provisioned there, Flux queries, bucket `metrics`).
- The dashboards are **not** provisioned from these files. Grafana serves them from its own
  database; this copy is a backup and a review artefact. Editing a file here changes nothing
  until it is pushed.

```sh
script/grafana_dashboards.py list               # what is in Grafana
script/grafana_dashboards.py export home-energy # Grafana -> grafana/home-energy.json
script/grafana_dashboards.py push home-energy   # grafana/home-energy.json -> Grafana
```

`push` sends the `version` from the file and is refused when the dashboard was changed in
the UI in the meantime; `--overwrite` is for a deliberate restore. Credentials come from
`HC_GRAFANA_URL` / `HC_GRAFANA_USER` / `HC_GRAFANA_PASSWORD` or the stack's `.env`. Keys are sorted on export so a
re-export diffs cleanly.

## Dashboards

| uid | What |
| --- | --- |
| `home-energy` | PV / load / grid / battery flows, SoC and the minimal SoC the manager wants, day-ahead spot price, today's energy, export limit, a state timeline of inverter and battery modes with the manager's decisions, boiler and HVAC on; grid voltages, battery voltage and temperature |
| `home-climate` | recuperation temperatures, CO₂, heat recovery efficiency, speed and power; HVAC temperatures and state; boiler temperatures and state |
| `home-water` | rainwater tank level and sensor diagnostics, pump cycles and run time per hour and per day |
| `home-mower` | Robonect position (geomap), status, battery, Wi-Fi, "last seen"; empty off season |

All four use a hidden constant variable `prefix` (`home`) in the measurement names, so a
second deployment with its own `influx.measurement.prefix` can reuse them. The spot price
panel shows the future only when the dashboard range ends after now (e.g. `now-12h` to
`now+1d`); Grafana does not allow a panel-level shift into the future.

Conventions (same as the chajda dashboard in `mps-inverter-controller`): `spanNulls: false`
and `createEmpty: true` wherever a gap is meaningful; `home-boiler` (10 min) and
`home-price` (15 min) use `stepAfter` and a bounded `spanNulls` instead.
