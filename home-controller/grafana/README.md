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
| *(none yet)* | to be created once the first data is in the bucket |

Conventions (same as the chajda dashboard in `mps-inverter-controller`): `spanNulls: false`
and `createEmpty: true` wherever a gap is meaningful; `home-boiler` (10 min) and
`home-price` (15 min) use `stepAfter` and a bounded `spanNulls` instead.
