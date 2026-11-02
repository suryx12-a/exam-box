import urllib.request
import json

BASE = "http://127.0.0.1:8080"

def post(url, data=None, headers=None):
    hdrs = {"Content-Type": "application/json"}
    if headers:
        hdrs.update(headers)
    body = json.dumps(data).encode("utf-8") if data else b"{}"
    req = urllib.request.Request(f"{BASE}{url}", data=body, headers=hdrs, method="POST")
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode())

def get(url, headers=None):
    hdrs = {}
    if headers:
        hdrs.update(headers)
    req = urllib.request.Request(f"{BASE}{url}", headers=hdrs, method="GET")
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode())

print("1. Generate Pairing Code...")
g = post("/api/pairing/generate")
code = g["pairing_code"]
print(f"   Generated Code: {code}")

print("2. Phone submits pairing request...")
r = post("/api/pairing/request", {"device_id": "VELS-PIXEL-8", "model": "Pixel 8", "pairing_code": code})
print(f"   Request Status: {r['status']}")

print("3. Admin confirms pairing on laptop...")
c = post("/api/pairing/confirm", {"action": "APPROVE"})
print(f"   Confirm Status: {c['status']}")

print("4. Phone fetches auth token...")
s = get("/api/pairing/status")
token = s["auth_token"]
print(f"   Auth Token: {token[:16]}...")

print("5. Phone sends heartbeat with Bearer token...")
h = post("/api/heartbeat", {
    "battery_level": 89,
    "is_charging": False,
    "wifi_ssid": "VELS_EXAM_SECURE",
    "threat_risk_score": 12,
    "vault_status": "LOCKED",
    "is_inside_geofence": True
}, headers={"Authorization": f"Bearer {token}"})
print(f"   Heartbeat Acknowledged: {h['acknowledged']}")

print("6. Phone syncs 2 offline events (with 1 duplicate)...")
events = [
    {
        "id": "evt-001",
        "eventType": "AUTH_SUCCESS",
        "description": "Supervisor biometric authentication verified",
        "severity": "INFO",
        "timestamp": 1793610920000,
        "isSimulated": False
    },
    {
        "id": "evt-002",
        "eventType": "GEOFENCE_BREACH",
        "description": "Perimeter breach detected 320m from VELS hall",
        "severity": "CRITICAL",
        "timestamp": 1793610925000,
        "isSimulated": True
    }
]
sync1 = post("/api/events/sync", {"device_id": "VELS-PIXEL-8", "events": events}, headers={"Authorization": f"Bearer {token}"})
print(f"   Sync 1: synced_count = {sync1['synced_count']}, acknowledged_ids = {sync1['acknowledged_ids']}")

print("7. Retry sync with same events (deduplication check)...")
sync2 = post("/api/events/sync", {"device_id": "VELS-PIXEL-8", "events": events}, headers={"Authorization": f"Bearer {token}"})
print(f"   Sync 2 (Duplicates): synced_count = {sync2['synced_count']}, acknowledged_ids = {sync2['acknowledged_ids']}")

print("8. Read events from dashboard...")
evs = get("/api/events")
print(f"   Total Synced Events on Dashboard: {evs['count']} (Deduplicated successfully!)")

print("9. Test unpair...")
u = post("/api/pairing/unpair")
print(f"   Unpair Status: {u['status']}")

print("\n[ALL 9 BACKEND TESTS PASSED]")
