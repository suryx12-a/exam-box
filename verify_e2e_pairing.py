import urllib.request
import urllib.error
import json
import time
import sys

BASE_URL = "http://127.0.0.1:8080"
LAN_URL = "http://192.168.1.39:8080"

def log(msg, status="INFO"):
    print(f"[{status}] {msg}")

def request_json(url, method="GET", data=None, headers=None):
    if headers is None:
        headers = {}
    body = None
    if data is not None:
        body = json.dumps(data).encode("utf-8")
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=5) as resp:
        return resp.getcode(), json.loads(resp.read().decode("utf-8"))

def run_e2e_tests():
    log("=== SMART EXAM SECURITY E2E PAIRING & SYNC VERIFICATION ===")

    # 1. Reachability Check (both LAN IP and Localhost)
    try:
        code, ping = request_json(f"{LAN_URL}/api/ping")
        assert code == 200 and ping.get("pong") is True
        log(f"Step 1: LAN Reachability check passed on {LAN_URL} ({ping.get('system')})", "PASS")
    except Exception as e:
        log(f"Step 1 Failed on LAN IP: {type(e).__name__}: {e}", "FAIL")
        sys.exit(1)

    # 2. APK Availability Check
    try:
        req = urllib.request.Request(f"{LAN_URL}/SmartExamSecurity-debug.apk", method="HEAD")
        with urllib.request.urlopen(req, timeout=5) as resp:
            content_length = int(resp.headers.get("Content-Length", 0))
            assert resp.getcode() == 200
            assert content_length > 10000000  # > 10MB
            log(f"Step 2: APK endpoint accessible: size {content_length / 1024 / 1024:.2f} MB", "PASS")
    except Exception as e:
        log(f"Step 2 Failed checking APK: {e}", "FAIL")
        sys.exit(1)

    # 3. Generate Pairing Code on Laptop Dashboard
    code, gen = request_json(f"{LAN_URL}/api/pairing/generate", method="POST")
    assert code == 200 and gen.get("success") is True
    pairing_code = gen.get("pairing_code")
    assert len(pairing_code) == 6
    log(f"Step 3: Pairing Code generated: {pairing_code} (Expires in {gen.get('expires_in')}s)", "PASS")

    # 4. Check Status: CODE_GENERATED
    code, st = request_json(f"{LAN_URL}/api/status")
    assert st.get("status") == "CODE_GENERATED"
    log("Step 4: Dashboard status verified as CODE_GENERATED", "PASS")

    # 5. Phone Submits Pairing Code
    phone_payload = {
        "device_id": "VELS-SEC-PHONE-PALLAVARAM-01",
        "model": "Samsung Galaxy S22 Ultra (Demo Device)",
        "pairing_code": pairing_code
    }
    code, req_resp = request_json(f"{LAN_URL}/api/pairing/request", method="POST", data=phone_payload)
    assert code == 200 and req_resp.get("status") == "PENDING_CONFIRMATION"
    log("Step 5: Phone submitted pairing code. State: PENDING_CONFIRMATION", "PASS")

    # 6. Dashboard sees Pending Request
    code, st = request_json(f"{LAN_URL}/api/status")
    assert st.get("status") == "PENDING_CONFIRMATION"
    pending = st.get("pending_request", {})
    assert pending.get("device_id") == "VELS-SEC-PHONE-PALLAVARAM-01"
    log(f"Step 6: Dashboard received incoming request from {pending.get('model')}", "PASS")

    # 7. Laptop Admin Approves Request
    confirm_payload = {"action": "approve"}
    code, conf_resp = request_json(f"{LAN_URL}/api/pairing/confirm", method="POST", data=confirm_payload)
    assert code == 200 and conf_resp.get("status") == "PAIRED"
    log("Step 7: Admin approved pairing request.", "PASS")

    # 8. Phone Polls for Token
    code, token_resp = request_json(f"{LAN_URL}/api/pairing/status")
    assert token_resp.get("status") == "PAIRED"
    auth_token = token_resp.get("auth_token")
    assert auth_token is not None and len(auth_token) > 16
    log(f"Step 8: Phone received Bearer token: {auth_token[:8]}...{auth_token[-4:]}", "PASS")

    # 9. Send Heartbeat with Live Phone Telemetry
    auth_headers = {"Authorization": f"Bearer {auth_token}"}
    heartbeat_data = {
        "battery_level": 92,
        "is_charging": False,
        "wifi_ssid": "VELS-Exam-Secure-5G",
        "latitude": 12.969845,
        "longitude": 80.144412,
        "is_inside_geofence": True,
        "threat_risk_score": 14,
        "vault_status": "LOCKED (AES-256-GCM)",
        "unsynced_event_count": 0
    }
    code, hb_resp = request_json(f"{LAN_URL}/api/heartbeat", method="POST", data=heartbeat_data, headers=auth_headers)
    assert code == 200 and hb_resp.get("success") is True
    log("Step 9: Authenticated heartbeat accepted by backend.", "PASS")

    # 10. Verify Dashboard shows Paired Telemetry
    code, st = request_json(f"{LAN_URL}/api/status")
    assert st.get("status") == "PAIRED"
    telemetry = st.get("paired_device", {}).get("telemetry", {})
    assert telemetry.get("battery_level") == 92
    assert telemetry.get("wifi_ssid") == "VELS-Exam-Secure-5G"
    assert telemetry.get("is_inside_geofence") is True
    log("Step 10: Live telemetry accurately reflected in dashboard state.", "PASS")

    # 11. Batch Sync Queued Offline Events from Phone SQLite
    sync_events = [
        {
            "id": "e2e-event-uuid-001",
            "eventType": "AUTH_SUCCESS",
            "deviceId": "VELS-SEC-PHONE-PALLAVARAM-01",
            "timestamp": int(time.time() * 1000) - 20000,
            "formattedTime": "10:15:00",
            "description": "Supervisor session authenticated via BiometricPrompt",
            "severity": "INFO",
            "latitude": 12.969845,
            "longitude": 80.144412,
            "isSimulated": False
        },
        {
            "id": "e2e-event-uuid-002",
            "eventType": "GEOFENCE_BREACH",
            "deviceId": "VELS-SEC-PHONE-PALLAVARAM-01",
            "timestamp": int(time.time() * 1000) - 10000,
            "formattedTime": "10:15:10",
            "description": "Device moved 450m outside VELS Campus boundary",
            "severity": "CRITICAL",
            "latitude": 12.978512,
            "longitude": 80.155621,
            "isSimulated": True
        }
    ]
    sync_payload = {
        "device_id": "VELS-SEC-PHONE-PALLAVARAM-01",
        "events": sync_events
    }
    code, sync_resp = request_json(f"{LAN_URL}/api/events/sync", method="POST", data=sync_payload, headers=auth_headers)
    assert code == 200 and sync_resp.get("synced_count") == 2
    assert "e2e-event-uuid-001" in sync_resp.get("acknowledged_ids", [])
    log("Step 11: Batch offline events synced (2 events: 1 real, 1 simulated).", "PASS")

    # 12. Test Deduplication: Re-send same events
    code, dup_resp = request_json(f"{LAN_URL}/api/events/sync", method="POST", data=sync_payload, headers=auth_headers)
    assert code == 200 and dup_resp.get("synced_count") == 0  # 0 new events because duplicates
    assert len(dup_resp.get("acknowledged_ids", [])) == 2   # Acknowledged so phone marks synced
    log("Step 12: Deduplication verified: 0 duplicate events created on re-send.", "PASS")

    # 13. Verify Synced Events via GET /api/events
    code, events_resp = request_json(f"{LAN_URL}/api/events")
    event_ids = [e["id"] for e in events_resp.get("events", [])]
    assert "e2e-event-uuid-001" in event_ids
    assert "e2e-event-uuid-002" in event_ids
    log(f"Step 13: Dashboard /api/events contains synced events ({len(event_ids)} total).", "PASS")

    # 14. Unpair Flow
    code, unpair_resp = request_json(f"{LAN_URL}/api/pairing/unpair", method="POST", data={}, headers=auth_headers)
    assert code == 200 and unpair_resp.get("status") == "UNPAIRED"
    
    # 15. Verify Token Invalidation
    try:
        request_json(f"{LAN_URL}/api/heartbeat", method="POST", data=heartbeat_data, headers=auth_headers)
        log("Step 15 Failed: Token was not invalidated!", "FAIL")
        sys.exit(1)
    except urllib.error.HTTPError as e:
        assert e.code == 401
        log("Step 15: Token successfully revoked upon unpair (HTTP 401 Unauthorized).", "PASS")

    log("=========================================================================")
    log("ALL 15 END-TO-END VERIFICATION CHECKS PASSED PERFECTLY!", "SUCCESS")
    log("=========================================================================")

if __name__ == "__main__":
    run_e2e_tests()
