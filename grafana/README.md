# Home Automation – Grafana + InfluxDB 2.x

Monitoring of metrics from the PV power plant and home automation (temperatures,
sensors, motion, heating…) running on a **Raspberry Pi 4** (Debian 12 Bookworm,
aarch64/arm64).

Data is **pushed** directly from custom code (Java on the Pi, ESP32) via the
InfluxDB **line protocol**. Visualization in Grafana.

```
┌─────────────┐   line protocol (HTTP)   ┌────────────┐        ┌──────────┐
│ Java / ESP32 │ ───────────────────────▶ │ InfluxDB 2 │ ◀───── │ Grafana  │
│  (push)      │                          │  :8086     │  Flux  │  :3000   │
└─────────────┘                          └────────────┘        └──────────┘
```

- **Target:** ~50 metrics, written ~once/min. Negligible load for the SD card
  (~50 MB/year of used space, a few GB of writes/year).
- **Architecture:** both images are multi-arch and support `linux/arm64`.

---

## 1. Prerequisites on the Pi – install Docker

Install Docker Engine + Compose plugin from Docker's official apt repository
(`get.docker.com` was unreachable from this network, and the apt repo is the
recommended approach for Debian anyway):

```bash
# prerequisites + Docker's GPG key
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

# add the repository (arm64, bookworm)
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian bookworm stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null

# install engine, CLI, containerd, buildx and the compose plugin
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# enable the service at boot and run docker without sudo
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
# log out and back in (or: newgrp docker) so the group membership takes effect

# verify
docker version
docker compose version
docker run --rm hello-world
```

> Quick alternative via Debian's own packages: `sudo apt install docker.io docker-compose-v2`
> (older versions, but functional).

---

## 2. Configuration

```bash
cd /path/to/grafana          # this directory copied to the Pi
cp .env.example .env
```

Fill in `.env` – especially **strong passwords** and the **InfluxDB admin token**:

```bash
# generate a random token
openssl rand -hex 32
```

Put it into `INFLUXDB_ADMIN_TOKEN` in `.env`. Set the passwords as well.

> **Do not commit `.env`** – it is in `.gitignore`.

---

## 3. Start

```bash
docker compose up -d
docker compose ps          # both containers "healthy/running"
docker compose logs -f     # follow the logs
```

On the **first** start InfluxDB runs an automatic setup (org, bucket, admin user
and token from `.env`). Grafana starts only once InfluxDB is `healthy` and
registers InfluxDB as the default datasource (Flux) by itself.

| Service  | URL                       | Login                              |
|----------|---------------------------|------------------------------------|
| Grafana  | http://pi.local:3000      | `GRAFANA_ADMIN_USER` / `…_PASSWORD`|
| InfluxDB | http://pi.local:8086      | `INFLUXDB_ADMIN_USER` / `…_PASSWORD`|

---

## 4. Per-device tokens (recommended)

Use the admin token only for Grafana/administration. For **each writing device**
create a separate **write-only token** restricted to the `metrics` bucket:

1. InfluxDB UI → **Load Data → API Tokens → Generate API Token → Custom API Token**
2. Select **Write** on the `metrics` bucket, name it e.g. `esp32-fv`, `java-collector`.
3. Copy the token into the given device/application.

This way an ESP32 "in the field" only has the right to write, not to delete/read everything.

---

## 5. Writing data – line protocol

Format:
```
measurement,tag1=val1,tag2=val2 field1=1.0,field2=42i timestamp_ns
```
Examples:
```
fv,inverter=main power=2350,voltage=235.1,today_kwh=12.4
temperature,room=living value=21.4
motion,sensor=hallway detected=1i
```

### HTTP endpoint (InfluxDB 2.x)
```
POST http://pi.local:8086/api/v2/write?org=home&bucket=metrics&precision=s
Authorization: Token <DEVICE_TOKEN>
Content-Type: text/plain
```
> `precision=s` = timestamp in seconds; you can also omit the timestamp and
> InfluxDB will fill in the time of receipt.

### curl (quick test)
```bash
curl -i -XPOST "http://pi.local:8086/api/v2/write?org=home&bucket=metrics&precision=s" \
  --header "Authorization: Token $DEVICE_TOKEN" \
  --data-raw "temperature,room=living value=21.4"
```

### Java (official client)
`build.gradle`:
```groovy
implementation 'com.influxdb:influxdb-client-java:7.2.0'
```
```java
import com.influxdb.client.*;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import java.time.Instant;

InfluxDBClient client = InfluxDBClientFactory.create(
        "http://localhost:8086",
        deviceToken.toCharArray(), "home", "metrics");

// recommended: batch – one HTTP request per minute for all 50 metrics
try (WriteApi writeApi = client.makeWriteApi()) {
    Point p = Point.measurement("fv")
            .addTag("inverter", "main")
            .addField("power", 2350.0)
            .addField("voltage", 235.1)
            .time(Instant.now(), WritePrecision.S);
    writeApi.writePoint(p);
}
```

### ESP32 (the `ESP8266 Influxdb` library by Tobias Schürg)
Arduino Library Manager → *"ESP8266 Influxdb"* (works on ESP32 too).
```cpp
#include <InfluxDbClient.h>

InfluxDBClient client("http://pi.local:8086", "home", "metrics", DEVICE_TOKEN);

void loop() {
  Point p("temperature");
  p.addTag("room", "bedroom");
  p.addField("value", readTemp());
  client.writePoint(p);   // the library also supports batching (setWriteOptions)
  delay(60000);           // once/min
}
```

---

## 6. Tips for the SD card / long-term operation

- **Batch the writes** – send all ~50 metrics in a single request per minute,
  not 50 separate ones (fewer fsyncs → less wear).
- **Downsampling** – for long-term history create an InfluxDB **Task** that
  aggregates raw data over time (e.g. minute → hourly). UI → *Tasks*.
- A quality card (Samsung PRO / SanDisk Max Endurance) is a cheap insurance,
  but not necessary for this load.

---

## 7. Management

```bash
docker compose pull && docker compose up -d   # update images
docker compose down                           # stop (data stays in volumes)
docker compose down -v                        # ⚠️ also deletes data (volumes)!
```

### Backups

`backup.sh` makes a consistent, versioned backup of the Docker volumes to the
external HDD (`/mnt/External4T/backup/pi/grafana`). It briefly stops the stack
(~a few seconds) for a clean snapshot, rsyncs the volumes into a timestamped
directory with `--link-dest` hardlink dedup against the previous snapshot, then
starts the stack again. Snapshots older than `RETENTION_DAYS` (default 14) are
pruned.

```bash
sudo /home/pi/grafana/backup.sh          # run a backup now (briefly stops the stack)
```

Scheduled daily via cron at **02:57** (3 min before the phone backup to the same
disk at 03:00) — `/etc/cron.d/grafana-backup`:
```cron
57 2 * * * root /home/pi/grafana/backup.sh >> /mnt/External4T/backup/pi/grafana/backup.log 2>&1
```

Backup layout on the HDD:
```
/mnt/External4T/backup/pi/grafana/
├── 2026-06-13_231628/        # timestamped snapshot
│   ├── grafana_influxdb-data/
│   ├── grafana_grafana-data/
│   └── grafana_influxdb-config/
├── latest -> 2026-06-13_231628
└── backup.log
```

**Restore** (stack must be down): stop the stack, copy a snapshot's volume
contents back into `/var/lib/docker/volumes/`, then start:
```bash
cd ~/grafana && docker compose down
SNAP=/mnt/External4T/backup/pi/grafana/latest
for v in grafana_influxdb-data grafana_grafana-data grafana_influxdb-config; do
  sudo rsync -a --delete "$SNAP/$v/" "/var/lib/docker/volumes/$v/"
done
docker compose up -d
```

---

## Directory structure
```
grafana/
├── docker-compose.yml
├── .env.example            # configuration template (commit this)
├── .env                    # actual secret values (do NOT commit)
├── .gitignore
├── deploy.sh               # scp the stack to the Pi (./deploy.sh [--up])
├── backup.sh               # consistent versioned backup to the external HDD
├── README.md
└── grafana/
    └── provisioning/
        ├── datasources/
        │   └── influxdb.yml    # auto-registration of the InfluxDB datasource
        └── dashboards/
            └── dashboards.yml  # auto-loading of dashboards (put JSON here)
```
