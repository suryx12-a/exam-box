import os
import sys
import json
import time
import uuid
import random
import socket
import threading
import traceback
import subprocess
from http.server import ThreadingHTTPServer, SimpleHTTPRequestHandler

PORT = 8080
WORKSPACE_DIR = os.path.dirname(os.path.abspath(__file__))
APK_PATH = os.path.join(WORKSPACE_DIR, "SmartExamSecurity-debug.apk")
APP_PACKAGE = "com.smartexambox.security"
APP_ACTIVITY = "com.smartexambox.security/.MainActivity"

def find_adb():
    import shutil
    adb_which = shutil.which("adb")
    if adb_which:
        return adb_which
    user_home = os.path.expanduser("~")
    candidates = [
        os.path.join(user_home, "AppData", "Local", "Android", "Sdk", "platform-tools", "adb.exe"),
        r"C:\Users\suryx\AppData\Local\Android\Sdk\platform-tools\adb.exe",
        r"C:\Android\Sdk\platform-tools\adb.exe"
    ]
    for c in candidates:
        if os.path.exists(c):
            return c
    return "adb"

ADB_PATH = find_adb()

def get_local_ip():
    """Returns the primary LAN IPv4 address reachable on local Wi-Fi / subnet."""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        try:
            return socket.gethostbyname(socket.gethostname())
        except Exception:
            return "127.0.0.1"

# --- Thread-Safe Pairing & Device State Store ---
state_lock = threading.Lock()

server_state = {
    "lan_ip": get_local_ip(),
    "port": PORT,
    "pairing_state": "UNPAIRED", # UNPAIRED, CODE_GENERATED, PENDING_CONFIRMATION, PAIRED
    "active_pairing_code": None,
    "pairing_code_expiry": 0,
    "pairing_token": None,
    "pending_request": None,    # { device_id, model, timestamp, client_ip }
    "paired_device": None,      # { device_id, model, auth_token, paired_at, last_seen, client_ip, telemetry }
    "synced_events": [],        # list of deduplicated events from the phone
    "seen_event_ids": set(),    # deduplication set
    "last_heartbeat": 0
}

def clean_expired_pairing():
    """Cleans up expired pairing codes."""
    now = time.time()
    if server_state["pairing_state"] in ("CODE_GENERATED", "PENDING_CONFIRMATION"):
        if now > server_state["pairing_code_expiry"]:
            print(f"[*] Pairing code {server_state['active_pairing_code']} expired.", flush=True)
            server_state["pairing_state"] = "UNPAIRED"
            server_state["active_pairing_code"] = None
            server_state["pairing_token"] = None
            server_state["pending_request"] = None

def run_adb(args):
    cmd = [ADB_PATH] + args
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, timeout=15)
        return {
            "success": res.returncode == 0,
            "stdout": res.stdout.strip(),
            "stderr": res.stderr.strip(),
            "returncode": res.returncode
        }
    except Exception as e:
        return {"success": False, "stdout": "", "stderr": str(e), "returncode": -1}

class SecurityBridgeHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=WORKSPACE_DIR, **kwargs)

    def log_message(self, format, *args):
        # Clean terminal logging
        sys.stdout.write(f"[{time.strftime('%H:%M:%S')}] {self.client_address[0]} - {format%args}\n")
        sys.stdout.flush()

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        self.end_headers()

    def send_json(self, data, code=200):
        body = json.dumps(data, indent=2).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def read_json_body(self):
        try:
            content_length = int(self.headers.get("Content-Length", 0))
            if content_length > 0:
                raw_body = self.rfile.read(content_length).decode("utf-8")
                return json.loads(raw_body)
        except Exception as e:
            print(f"[!] Error parsing JSON body: {e}", flush=True)
        return {}

    def get_auth_token(self):
        auth = self.headers.get("Authorization", "")
        if auth.startswith("Bearer "):
            return auth[7:].strip()
        return None

    def verify_paired_auth(self):
        """Returns True if request has a valid Bearer token matching the paired device."""
        token = self.get_auth_token()
        with state_lock:
            paired = server_state.get("paired_device")
            if paired and token and paired.get("auth_token") == token:
                return True
        return False

    def do_GET(self):
        try:
            path = self.path.split("?")[0]
            if path == "/api/ping":
                self.handle_ping()
            elif path == "/api/status":
                self.handle_status()
            elif path == "/api/pairing/status":
                self.handle_pairing_status()
            elif path == "/api/events":
                self.handle_get_events()
            elif path == "/api/devices":
                self.handle_adb_devices()
            elif path == "/api/phone-status":
                self.handle_phone_status()
            else:
                super().do_GET()
        except Exception as e:
            traceback.print_exc()
            self.send_error(500, str(e))

    def do_POST(self):
        try:
            path = self.path.split("?")[0]
            if path == "/api/pairing/generate":
                self.handle_pairing_generate()
            elif path == "/api/pairing/request":
                self.handle_pairing_request()
            elif path == "/api/pairing/confirm":
                self.handle_pairing_confirm()
            elif path == "/api/pairing/unpair":
                self.handle_pairing_unpair()
            elif path == "/api/heartbeat":
                self.handle_heartbeat()
            elif path == "/api/events/sync":
                self.handle_events_sync()
            elif path == "/api/install-apk":
                self.handle_install_apk()
            elif path == "/api/launch-app":
                self.handle_launch_app()
            elif path == "/api/lock":
                self.handle_remote_lock()
            elif path == "/api/unlock":
                self.handle_remote_unlock()
            elif path == "/api/reverse":
                self.handle_adb_reverse()
            else:
                self.send_error(404, "Endpoint Not Found")
        except Exception as e:
            traceback.print_exc()
            self.send_error(500, str(e))

    # --- API HANDLERS ---

    def handle_ping(self):
        lan_ip = get_local_ip()
        self.send_json({
            "pong": True,
            "system": "Smart Exam Paper Security - Central Monitor",
            "lan_ip": lan_ip,
            "port": PORT,
            "timestamp": int(time.time() * 1000)
        })

    def handle_status(self):
        with state_lock:
            clean_expired_pairing()
            lan_ip = get_local_ip()
            now = time.time()
            expires_in = max(0, int(server_state["pairing_code_expiry"] - now)) if server_state["active_pairing_code"] else 0
            
            # Check connection heartbeat freshness (connected if heartbeat received in last 20 seconds)
            is_connected = False
            last_seen_sec_ago = None
            if server_state["paired_device"]:
                last_seen = server_state["paired_device"].get("last_seen", 0)
                if last_seen > 0:
                    last_seen_sec_ago = int(now - last_seen)
                    is_connected = (last_seen_sec_ago <= 20)

            resp = {
                "system": "Smart Exam Paper Security System",
                "lan_ip": lan_ip,
                "port": PORT,
                "base_url": f"http://{lan_ip}:{PORT}",
                "apk_url": f"http://{lan_ip}:{PORT}/SmartExamSecurity-debug.apk",
                "status": server_state["pairing_state"],
                "pairing_state": server_state["pairing_state"],
                "is_paired": server_state["pairing_state"] == "PAIRED",
                "is_connected": is_connected,
                "last_seen_seconds_ago": last_seen_sec_ago,
                "active_pairing_code": server_state["active_pairing_code"],
                "pairing_code_expires_in_sec": expires_in,
                "pending_request": server_state["pending_request"],
                "paired_device": {
                    "device_id": server_state["paired_device"]["device_id"],
                    "model": server_state["paired_device"]["model"],
                    "paired_at": server_state["paired_device"]["paired_at"],
                    "telemetry": server_state["paired_device"].get("telemetry", {})
                } if server_state["paired_device"] else None,
                "synced_event_count": len(server_state["synced_events"]),
                "security_protocol": "DEMO_HTTP_PROTOTYPE (Plain HTTP on private Wi-Fi; TLS/HTTPS recommended for production)"
            }
            self.send_json(resp)

    def handle_pairing_generate(self):
        with state_lock:
            # Generate a 6-digit numeric pairing code (easy to type)
            code = f"{random.randint(100000, 999999)}"
            pairing_token = str(uuid.uuid4())
            expiry = time.time() + 300 # 5 minutes expiry
            lan_ip = get_local_ip()

            server_state["pairing_state"] = "CODE_GENERATED"
            server_state["active_pairing_code"] = code
            server_state["pairing_code_expiry"] = expiry
            server_state["pairing_token"] = pairing_token
            server_state["pending_request"] = None

            qr_payload = f"exambox://pair?server=http://{lan_ip}:{PORT}&code={code}&token={pairing_token}"

            print(f"[*] Generated new pairing code: {code} (Expires in 300s)", flush=True)
            self.send_json({
                "success": True,
                "pairing_code": code,
                "pairing_token": pairing_token,
                "expires_in": 300,
                "expires_in_seconds": 300,
                "server_url": f"http://{lan_ip}:{PORT}",
                "lan_ip": lan_ip,
                "port": PORT,
                "qr_payload": qr_payload
            })

    def handle_pairing_request(self):
        """Phone submits pairing code to initiate pairing."""
        body = self.read_json_body()
        device_id = body.get("device_id", "UNKNOWN_DEVICE")
        model = body.get("model", "Android Smartphone")
        code = str(body.get("pairing_code", "")).strip()

        with state_lock:
            clean_expired_pairing()

            if not server_state["active_pairing_code"]:
                self.send_json({
                    "success": False,
                    "error": "No active pairing code on laptop dashboard. Click 'Generate Pairing Code' on laptop first."
                }, code=400)
                return

            if time.time() > server_state["pairing_code_expiry"]:
                self.send_json({
                    "success": False,
                    "error": "Pairing code has expired. Please generate a new code on the laptop dashboard."
                }, code=408)
                return

            if code != server_state["active_pairing_code"]:
                print(f"[!] Incorrect pairing code from {self.client_address[0]}: received '{code}', expected '{server_state['active_pairing_code']}'", flush=True)
                self.send_json({
                    "success": False,
                    "error": f"Invalid pairing code '{code}'. Check the 6-digit code shown on the laptop dashboard."
                }, code=401)
                return

            # Code matches! Set state to PENDING_CONFIRMATION
            server_state["pairing_state"] = "PENDING_CONFIRMATION"
            server_state["pending_request"] = {
                "device_id": device_id,
                "model": model,
                "code": code,
                "client_ip": self.client_address[0],
                "requested_at": int(time.time() * 1000)
            }

            print(f"[+] Pairing code verified from {model} ({device_id}). Waiting for laptop admin confirmation...", flush=True)
            self.send_json({
                "success": True,
                "status": "PENDING_CONFIRMATION",
                "message": "Pairing code verified! Please approve the pairing request on the laptop dashboard."
            })

    def handle_pairing_confirm(self):
        """Laptop administrator approves or rejects the pending phone pairing."""
        body = self.read_json_body()
        action = body.get("action", "").upper() # APPROVE or REJECT

        with state_lock:
            if not server_state["pending_request"]:
                self.send_json({"success": False, "error": "No pending pairing request to confirm."}, code=400)
                return

            if action == "APPROVE":
                pending = server_state["pending_request"]
                auth_token = str(uuid.uuid4())
                paired = {
                    "device_id": pending["device_id"],
                    "model": pending["model"],
                    "auth_token": auth_token,
                    "paired_at": int(time.time() * 1000),
                    "last_seen": time.time(),
                    "client_ip": pending["client_ip"],
                    "telemetry": {}
                }

                server_state["paired_device"] = paired
                server_state["pairing_state"] = "PAIRED"
                # Burn the pairing code so it cannot be reused!
                server_state["active_pairing_code"] = None
                server_state["pairing_token"] = None
                server_state["pending_request"] = None

                print(f"[OK] PAIRING APPROVED: {paired['model']} ({paired['device_id']}) is now authenticated!", flush=True)
                self.send_json({
                    "success": True,
                    "status": "PAIRED",
                    "device": {
                        "device_id": paired["device_id"],
                        "model": paired["model"],
                        "paired_at": paired["paired_at"]
                    }
                })
            else:
                # REJECT
                print("[X] Pairing request rejected by administrator.", flush=True)
                server_state["pending_request"] = None
                server_state["pairing_state"] = "CODE_GENERATED" if server_state["active_pairing_code"] else "UNPAIRED"
                self.send_json({"success": True, "status": "REJECTED", "message": "Pairing request rejected."})

    def handle_pairing_status(self):
        """Polled by Android app to check if admin approved the pairing request."""
        with state_lock:
            clean_expired_pairing()
            state = server_state["pairing_state"]
            paired = server_state.get("paired_device")
            
            if state == "PAIRED" and paired:
                self.send_json({
                    "status": "PAIRED",
                    "auth_token": paired["auth_token"],
                    "device_id": paired["device_id"],
                    "model": paired["model"],
                    "server_url": f"http://{get_local_ip()}:{PORT}"
                })
            elif state == "PENDING_CONFIRMATION":
                self.send_json({
                    "status": "PENDING_CONFIRMATION",
                    "message": "Awaiting laptop administrator approval."
                })
            elif state == "CODE_GENERATED":
                self.send_json({
                    "status": "CODE_GENERATED",
                    "message": "Pairing code active on dashboard."
                })
            else:
                self.send_json({
                    "status": "UNPAIRED",
                    "message": "Device is not currently paired."
                })

    def handle_pairing_unpair(self):
        """Unpairs the current device and invalidates session token."""
        with state_lock:
            dev = server_state.get("paired_device")
            dev_name = dev["model"] if dev else "device"
            server_state["paired_device"] = None
            server_state["pairing_state"] = "UNPAIRED"
            server_state["active_pairing_code"] = None
            server_state["pairing_token"] = None
            server_state["pending_request"] = None
            print(f"[*] Unpaired {dev_name}. Session token invalidated.", flush=True)
            self.send_json({
                "success": True,
                "status": "UNPAIRED",
                "message": f"Successfully unpaired {dev_name}."
            })

    def handle_heartbeat(self):
        """Receives live telemetry from paired phone and updates connection status."""
        if not self.verify_paired_auth():
            self.send_json({"success": False, "error": "Unauthorized: Device is not paired or token invalid."}, code=401)
            return

        body = self.read_json_body()
        with state_lock:
            if server_state["paired_device"]:
                server_state["paired_device"]["last_seen"] = time.time()
                server_state["paired_device"]["client_ip"] = self.client_address[0]
                server_state["paired_device"]["telemetry"] = {
                    "battery_level": body.get("battery_level", 100),
                    "is_charging": body.get("is_charging", False),
                    "wifi_ssid": body.get("wifi_ssid", "Unknown"),
                    "latitude": body.get("latitude"),
                    "longitude": body.get("longitude"),
                    "is_inside_geofence": body.get("is_inside_geofence", True),
                    "threat_risk_score": body.get("threat_risk_score", 12),
                    "vault_status": body.get("vault_status", "LOCKED"),
                    "unsynced_event_count": body.get("unsynced_event_count", 0),
                    "updated_at": int(time.time() * 1000)
                }

        self.send_json({
            "success": True,
            "acknowledged": True,
            "server_time": int(time.time() * 1000),
            "pairing_status": "PAIRED"
        })

    def handle_events_sync(self):
        """Receives offline/online security events from the phone and deduplicates by event ID."""
        if not self.verify_paired_auth():
            self.send_json({"success": False, "error": "Unauthorized: Valid pairing auth_token required."}, code=401)
            return

        body = self.read_json_body()
        events = body.get("events", [])
        acknowledged_ids = []
        new_count = 0

        with state_lock:
            for ev in events:
                ev_id = ev.get("id")
                if not ev_id:
                    continue
                acknowledged_ids.append(ev_id)
                # Deduplication check
                if ev_id not in server_state["seen_event_ids"]:
                    server_state["seen_event_ids"].add(ev_id)
                    server_state["synced_events"].insert(0, {
                        "id": ev_id,
                        "eventType": ev.get("eventType", "SECURITY_EVENT"),
                        "deviceId": ev.get("deviceId", "SMARTPHONE"),
                        "timestamp": ev.get("timestamp", int(time.time() * 1000)),
                        "formattedTime": ev.get("formattedTime", time.strftime("%Y-%m-%d %H:%M:%S")),
                        "description": ev.get("description", "Security Event"),
                        "severity": ev.get("severity", "INFO"),
                        "latitude": ev.get("latitude"),
                        "longitude": ev.get("longitude"),
                        "isSimulated": ev.get("isSimulated", False),
                        "receivedAt": int(time.time() * 1000)
                    })
                    new_count += 1

        print(f"[OK] Event Sync: Received {len(events)} event(s) ({new_count} new, {len(events) - new_count} duplicates avoided).", flush=True)
        self.send_json({
            "success": True,
            "synced_count": new_count,
            "acknowledged_ids": acknowledged_ids,
            "total_synced_events": len(server_state["synced_events"])
        })

    def handle_get_events(self):
        with state_lock:
            events_copy = list(server_state["synced_events"])
        self.send_json({
            "success": True,
            "count": len(events_copy),
            "events": events_copy
        })

    # --- ADB USB CONTROLLER FALLBACKS ---

    def handle_adb_devices(self):
        res = run_adb(["devices", "-l"])
        lines = res["stdout"].splitlines()
        devices = []
        for line in lines[1:]:
            parts = line.strip().split()
            if len(parts) >= 2:
                dev_id = parts[0]
                status = parts[1]
                model = "Android Device"
                for p in parts[2:]:
                    if p.startswith("model:"):
                        model = p.split(":", 1)[1]
                devices.append({"id": dev_id, "status": status, "model": model})
        self.send_json({
            "success": True,
            "devices": devices,
            "count": len(devices),
            "raw": res["stdout"]
        })

    def handle_phone_status(self):
        batt = run_adb(["shell", "dumpsys", "battery"])
        level = 100
        for l in batt["stdout"].splitlines():
            if "level:" in l:
                try:
                    level = int(l.split(":")[1].strip())
                except Exception:
                    pass
        self.send_json({
            "success": batt["success"],
            "battery_level": level,
            "raw_battery": batt["stdout"]
        })

    def handle_install_apk(self):
        res = run_adb(["install", "-r", APK_PATH])
        self.send_json({
            "success": "Success" in res["stdout"],
            "output": res["stdout"],
            "error": res["stderr"]
        })

    def handle_launch_app(self):
        res = run_adb(["shell", "am", "start", "-n", APP_ACTIVITY])
        self.send_json({
            "success": res["success"],
            "output": res["stdout"],
            "error": res["stderr"]
        })

    def handle_remote_lock(self):
        res = run_adb(["shell", "am", "broadcast", "-a", "com.smartexambox.security.ACTION_REMOTE_LOCK"])
        self.send_json({
            "success": True,
            "action": "REMOTE_LOCK",
            "message": "Vault lockdown broadcast sent to phone over USB",
            "output": res["stdout"]
        })

    def handle_remote_unlock(self):
        res = run_adb(["shell", "am", "broadcast", "-a", "com.smartexambox.security.ACTION_REMOTE_UNLOCK"])
        self.send_json({
            "success": True,
            "action": "REMOTE_UNLOCK",
            "message": "Vault authorization unlock broadcast sent to phone over USB",
            "output": res["stdout"]
        })

    def handle_adb_reverse(self):
        res = run_adb(["reverse", f"tcp:{PORT}", f"tcp:{PORT}"])
        self.send_json({
            "success": res["success"],
            "output": res["stdout"],
            "message": f"Reverse USB tunnel configured (tcp:{PORT})"
        })

if __name__ == "__main__":
    local_ip = get_local_ip()
    print("=" * 75, flush=True)
    print("  SMART EXAM PAPER SECURITY - WI-FI PAIRING & MONITORING BACKEND", flush=True)
    print("=" * 75, flush=True)
    print(f"  [OK] Primary LAN IPv4:     {local_ip}", flush=True)
    print(f"  [OK] Listening Interface:  0.0.0.0:{PORT} (All local interfaces)", flush=True)
    print(f"  [OK] Dashboard URL:        http://{local_ip}:{PORT}/web_demo/", flush=True)
    print(f"  [OK] Mobile APK Download:  http://{local_ip}:{PORT}/SmartExamSecurity-debug.apk", flush=True)
    print(f"  [OK] ADB Engine:           {ADB_PATH}", flush=True)
    print(f"  [!] Security Protocol:    PROTOTYPE HTTP (Local private Wi-Fi)", flush=True)
    print("=" * 75, flush=True)

    server = ThreadingHTTPServer(("0.0.0.0", PORT), SecurityBridgeHandler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping server...", flush=True)
        server.server_close()
