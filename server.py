"""
Pegasus local device-status server.

This server intentionally supports device registration and basic status reporting only.
It does not queue or execute remote commands, expose files, collect location, or provide
process/package/notification controls.

Run (PowerShell):
    $env:PEGASUS_STATUS_TOKEN = "replace-with-a-long-random-secret"
    python server.py

Open the dashboard at http://<this-PC-LAN-IP>:3000/
Use the same token in the Android client's status request configuration. Do not expose
this development server directly to the public internet.
"""
from __future__ import annotations

import json
import os
import secrets
import socket
import time
from datetime import datetime, timezone
from threading import Lock
from typing import Any

from flask import Flask, jsonify, request, render_template_string

app = Flask(__name__)
app.config["MAX_CONTENT_LENGTH"] = 64 * 1024

TOKEN_ENV = "PEGASUS_STATUS_TOKEN"
devices: dict[str, dict[str, Any]] = {}
lock = Lock()

DASHBOARD_HTML = r"""
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Pegasus · Device Status</title>
<style>
:root{color-scheme:dark;--bg:#0b1020;--panel:#121a2d;--line:#26334d;--text:#e7edf8;--muted:#96a6c1;--accent:#7c9cff;--good:#54d6a0}
*{box-sizing:border-box}body{margin:0;background:radial-gradient(ellipse at top,#172441,var(--bg) 55%);color:var(--text);font:15px/1.5 system-ui,-apple-system,Segoe UI,sans-serif}
main{max-width:1000px;margin:auto;padding:28px 18px}header{display:flex;align-items:center;justify-content:space-between;gap:16px;flex-wrap:wrap;margin-bottom:24px}
h1{font-size:25px;margin:0;letter-spacing:.2px}.sub{color:var(--muted);margin-top:4px}.pill{border:1px solid var(--line);border-radius:999px;padding:6px 10px;color:var(--muted);font-size:12px}
.stats{display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:12px;margin-bottom:18px}.card{background:linear-gradient(145deg,#151f35,#10182a);border:1px solid var(--line);border-radius:16px;padding:18px;box-shadow:0 10px 30px #0002}
.k{color:var(--muted);font-size:12px;text-transform:uppercase;letter-spacing:1px}.v{font-size:26px;font-weight:650;margin-top:5px}
.section-title{font-size:16px;margin:0 0 12px}.device{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:12px;align-items:center;padding:15px 0;border-top:1px solid var(--line)}.device:first-of-type{border-top:0}
.name{font-weight:650;overflow-wrap:anywhere}.meta{color:var(--muted);font-size:13px;margin-top:4px}.online{color:var(--good);font-size:12px}.empty{color:var(--muted);padding:22px 0}.foot{color:var(--muted);font-size:12px;margin-top:18px}
@media(max-width:500px){main{padding:20px 12px}.device{grid-template-columns:1fr}.v{font-size:23px}}
</style>
</head>
<body><main>
<header><div><h1>Pegasus <span style="color:var(--accent)">/</span> Device Status</h1><div class="sub">Local dashboard · status reporting only</div></div><div class="pill" id="updated">Connecting…</div></header>
<div class="stats"><div class="card"><div class="k">Registered devices</div><div class="v" id="count">—</div></div><div class="card"><div class="k">Online now</div><div class="v" id="online">—</div></div><div class="card"><div class="k">Refresh interval</div><div class="v">2 sec</div></div></div>
<section class="card"><h2 class="section-title">Devices</h2><div id="devices" class="empty">Waiting for a device to register…</div></section>
<div class="foot">Only basic device status is displayed. This dashboard does not offer remote-control actions.</div>
</main>
<script>
const esc = s => String(s ?? "—").replace(/[&<>"']/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
async function refresh(){
 try{
  const r=await fetch("/api/status",{cache:"no-store"});
  if(!r.ok) throw new Error("HTTP "+r.status);
  const data=await r.json(), list=data.devices||[], now=Date.now()/1000;
  document.getElementById("count").textContent=list.length;
  document.getElementById("online").textContent=list.filter(d=>now-(d.last_seen||0)<30).length;
  document.getElementById("updated").textContent="Updated "+new Date().toLocaleTimeString();
  const root=document.getElementById("devices");
  if(!list.length){root.className="empty";root.textContent="No devices connected yet. Open the Android app and enable status reporting.";return}
  root.className="";
  root.innerHTML=list.sort((a,b)=>(b.last_seen||0)-(a.last_seen||0)).map(d=>{
   const info=d.info||{}, alive=now-(d.last_seen||0)<30;
   const battery=info.battery_level==null?"Battery unavailable":esc(info.battery_level)+"% battery";
   const network=info.network_type?(" · "+esc(info.network_type)):"";
   const seen=d.last_seen?new Date(d.last_seen*1000).toLocaleString():"Never";
   return '<div class="device"><div><div class="name">'+esc(info.model||"Android device")+'</div><div class="meta">ID: '+esc(d.device_id)+' · Android '+esc(info.android_version||"unknown")+' · '+battery+network+'</div><div class="meta">Last seen: '+esc(seen)+'</div></div><div class="'+(alive?"online":"pill")+'">'+(alive?"● Online":"Offline")+'</div></div>';
  }).join("");
 }catch(e){document.getElementById("updated").textContent="Server unavailable";console.error(e)}
}
refresh();setInterval(refresh,2000);
</script></body></html>
"""


def configured_token() -> str | None:
    token = os.environ.get(TOKEN_ENV, "").strip()
    return token or None


def authorized() -> bool:
    expected = configured_token()
    if not expected:
        return False
    supplied = request.headers.get("X-Pegasus-Secret", "")
    if request.form:
        supplied = supplied or request.form.get("secret", "")
    if request.is_json:
        body = request.get_json(silent=True) or {}
        supplied = supplied or str(body.get("secret", ""))
    return bool(supplied) and secrets.compare_digest(supplied, expected)


def parse_payload() -> tuple[str | None, dict[str, Any] | None]:
    """Accept both JSON requests and the Android client's URL-encoded FormBody."""
    if request.is_json:
        data = request.get_json(silent=True) or {}
        device_id = data.get("device_id")
        payload = data.get("payload")
    else:
        device_id = request.form.get("device_id")
        payload = request.form.get("payload")
        if isinstance(payload, str):
            try:
                payload = json.loads(payload)
            except (json.JSONDecodeError, TypeError):
                return None, None
    if not isinstance(device_id, str) or not device_id.strip() or len(device_id) > 200:
        return None, None
    if not isinstance(payload, dict):
        return None, None
    # Keep only non-sensitive status fields; deliberately discard location and arbitrary data.
    allowed = ("device_model", "android_version", "app_version", "battery_level",
               "battery_plugged", "network_type", "wifi_ssid", "device_manufacturer")
    clean = {key: payload[key] for key in allowed if key in payload}
    # Older Android clients send the OS release in app_version instead.
    if "android_version" not in clean and "app_version" in clean:
        clean["android_version"] = clean["app_version"]
    return device_id.strip(), clean


def upsert_device(device_id: str, info: dict[str, Any] | None = None) -> None:
    with lock:
        item = devices.setdefault(device_id, {"device_id": device_id, "last_seen": 0, "info": {}})
        item["last_seen"] = time.time()
        if info:
            item["info"].update(info)


@app.get("/")
def dashboard():
    return render_template_string(DASHBOARD_HTML)


@app.get("/health")
def health():
    return jsonify({"status": "ok", "service": "Pegasus status server"})


@app.get("/api/status")
def api_status():
    with lock:
        snapshot = [dict(d, info=dict(d["info"])) for d in devices.values()]
    return jsonify({"devices": snapshot, "server_time": datetime.now(timezone.utc).isoformat()})


@app.get("/api/v1/commands")
def no_remote_commands():
    # Keep polling harmless for client versions that expect this route. No command
    # queue exists here and this endpoint can never dispatch remote actions.
    if not authorized():
        if not configured_token():
            return jsonify({"error": f"Set the {TOKEN_ENV} environment variable first"}), 503
        return jsonify({"error": "Unauthorized"}), 401
    device_id = request.args.get("device_id", "").strip()
    if not device_id or len(device_id) > 200:
        return jsonify([])
    upsert_device(device_id)
    return jsonify([])


@app.route("/api/v1/response", methods=["POST"])
def receive_status():
    if not configured_token():
        return jsonify({"error": f"Set the {TOKEN_ENV} environment variable first"}), 503
    if not authorized():
        return jsonify({"error": "Unauthorized"}), 401
    device_id, payload = parse_payload()
    if not device_id or payload is None:
        return jsonify({"error": "Expected device_id and a JSON-object payload"}), 400
    upsert_device(device_id, payload)
    return jsonify({"status": "received"})


def get_lan_ip() -> str:
    # Determine the preferred outbound interface without sending any packets.
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        sock.connect(("192.0.2.1", 80))
        return sock.getsockname()[0]
    except OSError:
        return "127.0.0.1"
    finally:
        sock.close()


if __name__ == "__main__":
    if not configured_token():
        raise SystemExit(
            f"Missing {TOKEN_ENV}. In PowerShell run: "
            '$env:PEGASUS_STATUS_TOKEN = "replace-with-a-long-random-secret"'
        )
    host = "0.0.0.0"
    port = int(os.environ.get("PEGASUS_PORT", "3000"))
    ip = get_lan_ip()
    print(f"Pegasus status server listening on http://{ip}:{port}")
    print("Status dashboard only; remote-command endpoints are intentionally disabled.")
    app.run(host=host, port=port, debug=False, threaded=True)
