#!/usr/bin/env python3
"""Export or push the home-controller Grafana dashboards kept in grafana/*.json.

Usage:
  grafana_dashboards.py export <uid> [<uid> ...]   write grafana/<uid>.json from the running Grafana
  grafana_dashboards.py push <uid> [<uid> ...]     upload grafana/<uid>.json (refused if Grafana has a newer version)
  grafana_dashboards.py push --overwrite <uid>     upload regardless of the version in Grafana (restore)
  grafana_dashboards.py list                       list dashboards in Grafana

Credentials come from HC_GRAFANA_URL (default http://pi.local:3000), HC_GRAFANA_USER and
HC_GRAFANA_PASSWORD; the stack's .env (~/work/HomeAutomation/grafana/.env) has GRAFANA_ADMIN_USER /
GRAFANA_ADMIN_PASSWORD and is read when the variables are not set. The HC_ prefix keeps clear of the
GRAFANA_URL that other tooling may have in the shell. Exported JSON has sorted keys and a fixed indent so
re-exports diff cleanly.
"""
import argparse
import base64
import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

REPO_DIR = Path(__file__).resolve().parent.parent / "grafana"
STACK_ENV = Path.home() / "work/HomeAutomation/grafana/.env"


def load_credentials():
    url = os.environ.get("HC_GRAFANA_URL", "http://pi.local:3000").rstrip("/")
    user = os.environ.get("HC_GRAFANA_USER")
    password = os.environ.get("HC_GRAFANA_PASSWORD")
    if (not user or not password) and STACK_ENV.exists():
        env = {}
        for line in STACK_ENV.read_text().splitlines():
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                env[k.strip()] = v.strip()
        user = user or env.get("GRAFANA_ADMIN_USER")
        password = password or env.get("GRAFANA_ADMIN_PASSWORD")
    if not user or not password:
        sys.exit("Grafana credentials not found: set HC_GRAFANA_USER and HC_GRAFANA_PASSWORD")
    return url, user, password


def request(method, path, body=None):
    url, user, password = load_credentials()
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url + path, data=data, method=method)
    req.add_header("Authorization", "Basic " + base64.b64encode(f"{user}:{password}".encode()).decode())
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.load(resp)
    except urllib.error.HTTPError as e:
        sys.exit(f"{method} {path} failed: HTTP {e.code} {e.read().decode(errors='replace')}")


def export(uids):
    REPO_DIR.mkdir(exist_ok=True)
    for uid in uids:
        dashboard = request("GET", f"/api/dashboards/uid/{uid}")["dashboard"]
        path = REPO_DIR / f"{uid}.json"
        path.write_text(json.dumps(dashboard, ensure_ascii=False, indent=2, sort_keys=True) + "\n")
        print(f"exported {uid} v{dashboard.get('version')} -> {path.relative_to(REPO_DIR.parent)}")


def push(uids, overwrite, message):
    for uid in uids:
        path = REPO_DIR / f"{uid}.json"
        dashboard = json.loads(path.read_text())
        if overwrite:
            dashboard.pop("version", None)
        result = request("POST", "/api/dashboards/db", {"dashboard": dashboard, "overwrite": overwrite, "message": message})
        print(f"pushed {uid}: {result.get('status')} v{result.get('version')} {result.get('url')}")
        # keep the version in the file in sync so the next push is accepted
        dashboard["version"] = result.get("version")
        path.write_text(json.dumps(dashboard, ensure_ascii=False, indent=2, sort_keys=True) + "\n")


def list_dashboards():
    for d in request("GET", "/api/search?type=dash-db"):
        print(f"{d['uid']:20} {d['title']}")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    p = sub.add_parser("export")
    p.add_argument("uids", nargs="+")
    p = sub.add_parser("push")
    p.add_argument("uids", nargs="+")
    p.add_argument("--overwrite", action="store_true")
    p.add_argument("--message", default="pushed from home-controller/grafana")
    sub.add_parser("list")
    args = parser.parse_args()
    if args.command == "export":
        export(args.uids)
    elif args.command == "push":
        push(args.uids, args.overwrite, args.message)
    else:
        list_dashboards()


if __name__ == "__main__":
    main()
