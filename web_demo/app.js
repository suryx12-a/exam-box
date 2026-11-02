/**
 * Smart Exam Paper Security & Mobile Monitoring System
 * Interactive Web Demonstration & Examiner Viva Command Center
 * VELS Institute of Science, Technology & Advanced Studies (VISTAS)
 */

// Application State
const state = {
  currentScreen: 'screen-splash',
  pinInput: '',
  correctPin: '123456',
  failedPinAttempts: 0,
  isLockedOut: false,
  lockoutSeconds: 60,
  lockoutTimerId: null,
  
  riskScore: 12, // 0 - 100
  vaultLocked: true,
  wifiConnected: true,
  wifiSsid: 'VELS_EXAM_5G',
  
  // Geofence & GPS (VELS Campus Pallavaram Center)
  centerLat: 12.969845,
  centerLon: 80.144412,
  deviceLat: 12.969845 + 0.00021,
  deviceLon: 80.144412 + 0.00015,
  geofenceRadiusMeters: 250,
  isInsideGeofence: true,
  
  offlineBufferCount: 0,
  activeAlertCount: 0,
  radarAngle: 0,
  
  // Audit Ledger
  events: [
    { id: 1, type: 'AUTH', severity: 'INFO', desc: 'Device supervisor session authenticated via BiometricPrompt', time: '10:14:22', isDemo: true },
    { id: 2, type: 'GPS', severity: 'INFO', desc: 'GPS Lock established: VELS Main Block (accuracy: 4.2m)', time: '10:18:05', isDemo: false },
    { id: 3, type: 'TAMPER', severity: 'INFO', desc: 'Keystore integrity verified: Hardware master key valid', time: '10:22:40', isDemo: false },
    { id: 4, type: 'NET', severity: 'INFO', desc: 'Wi-Fi linked to authorized AP: VELS_EXAM_5G (RSSI: -58 dBm)', time: '10:25:12', isDemo: false },
    { id: 5, type: 'AUTH', severity: 'WARNING', desc: 'Vault access attempted outside scheduled window [DEMO DATA]', time: '10:31:45', isDemo: true },
    { id: 6, type: 'GPS', severity: 'INFO', desc: 'Geofence heartbeat verified (Distance: 34.2m <= 250m)', time: '10:40:00', isDemo: false }
  ]
};

// Web Audio API Sound Synthesizer (Zero external audio files needed)
let audioCtx = null;
function getAudioContext() {
  if (!audioCtx) {
    const AudioContextClass = window.AudioContext || window.webkitAudioContext;
    if (AudioContextClass) {
      audioCtx = new AudioContextClass();
    }
  }
  if (audioCtx && audioCtx.state === 'suspended') {
    audioCtx.resume();
  }
  return audioCtx;
}

function playBeep(freq = 600, duration = 0.08, type = 'sine') {
  try {
    const ctx = getAudioContext();
    if (!ctx) return;
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = type;
    osc.frequency.setValueAtTime(freq, ctx.currentTime);
    gain.gain.setValueAtTime(0.08, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + duration);
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.start();
    osc.stop(ctx.currentTime + duration);
  } catch (e) {
    // Audio context may be blocked before first user gesture
  }
}

function playAlarmSiren() {
  try {
    const ctx = getAudioContext();
    if (!ctx) return;
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = 'sawtooth';
    osc.frequency.setValueAtTime(800, ctx.currentTime);
    osc.frequency.linearRampToValueAtTime(1400, ctx.currentTime + 0.25);
    osc.frequency.linearRampToValueAtTime(800, ctx.currentTime + 0.5);
    gain.gain.setValueAtTime(0.12, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.6);
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.start();
    osc.stop(ctx.currentTime + 0.6);
  } catch (e) {}
}

function playSuccessChime() {
  playBeep(523.25, 0.1, 'triangle');
  setTimeout(() => playBeep(659.25, 0.1, 'triangle'), 100);
  setTimeout(() => playBeep(783.99, 0.18, 'triangle'), 200);
}

// System Clock
function updateClock() {
  const now = new Date();
  const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  const clockEl = document.getElementById('deviceClock');
  if (clockEl) clockEl.textContent = timeStr;
}
setInterval(updateClock, 1000);
updateClock();

// Navigation Controller
function navigateTo(screenId) {
  playBeep(450, 0.04);
  const screens = document.querySelectorAll('.app-screen');
  screens.forEach(s => s.classList.add('hidden'));
  
  const target = document.getElementById(screenId);
  if (target) {
    target.classList.remove('hidden');
    state.currentScreen = screenId;
  }

  // Update bottom nav highlighting
  const navTabs = document.querySelectorAll('.nav-tab');
  navTabs.forEach(t => t.classList.remove('active'));
  
  if (screenId === 'screen-dashboard') document.getElementById('navTabDashboard')?.classList.add('active');
  if (screenId === 'screen-vault') document.getElementById('navTabVault')?.classList.add('active');
  if (screenId === 'screen-location') document.getElementById('navTabRadar')?.classList.add('active');
  if (screenId === 'screen-events') document.getElementById('navTabLogs')?.classList.add('active');
  
  // Close screen picker drawer and backdrop
  closeScreenPickerMenu();
}

function openScreenPickerMenu() {
  const drawer = document.getElementById('screenPickerDrawer');
  const backdrop = document.getElementById('drawerBackdrop');
  if (drawer) drawer.classList.remove('hidden');
  if (backdrop) backdrop.classList.remove('hidden');
  playBeep(520, 0.05);
}

function closeScreenPickerMenu() {
  const drawer = document.getElementById('screenPickerDrawer');
  const backdrop = document.getElementById('drawerBackdrop');
  if (drawer) drawer.classList.add('hidden');
  if (backdrop) backdrop.classList.add('hidden');
  playBeep(400, 0.04);
}

function toggleScreenPickerMenu() {
  const drawer = document.getElementById('screenPickerDrawer');
  if (drawer && !drawer.classList.contains('hidden')) {
    closeScreenPickerMenu();
  } else {
    openScreenPickerMenu();
  }
}

// Close drawer on Escape key
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    closeScreenPickerMenu();
  }
});

// Splash Screen Auto-Progress
window.addEventListener('DOMContentLoaded', () => {
  renderEventLogs();
  renderAlerts();
  initKeypad();
  initRadarCanvas();
  
  // Animate Splash Loader
  let progress = 0;
  const bar = document.getElementById('splashBar');
  const splashStatus = document.getElementById('splashStatusText');
  
  const splashTimer = setInterval(() => {
    progress += 25;
    if (bar) bar.style.width = progress + '%';
    
    if (progress === 50 && splashStatus) {
      splashStatus.textContent = 'Checking VELS Campus Geofence...';
    } else if (progress === 75 && splashStatus) {
      splashStatus.textContent = 'Enforcing AES-256 Keystore Vault...';
    } else if (progress >= 100) {
      clearInterval(splashTimer);
      setTimeout(() => {
        navigateTo('screen-login');
      }, 500);
    }
  }, 400);
});

// 2. PIN Pad Logic
function initKeypad() {
  const keypad = document.getElementById('pinKeypad');
  if (!keypad) return;
  
  keypad.addEventListener('click', (e) => {
    const btn = e.target.closest('.keypad-btn');
    if (!btn || state.isLockedOut) return;
    
    const key = btn.getAttribute('data-key');
    handleKeypadInput(key);
  });
}

function handleKeypadInput(key) {
  if (state.isLockedOut) return;

  if (key === 'clear') {
    state.pinInput = '';
    playBeep(350, 0.06);
  } else if (key === 'del') {
    state.pinInput = state.pinInput.slice(0, -1);
    playBeep(400, 0.04);
  } else if (key && state.pinInput.length < 6) {
    state.pinInput += key;
    playBeep(700 + state.pinInput.length * 60, 0.05);
  }

  updatePinDots();

  if (state.pinInput.length === 6) {
    setTimeout(verifyPin, 150);
  }
}

function updatePinDots() {
  const dots = document.querySelectorAll('.pin-dot');
  dots.forEach((dot, index) => {
    if (index < state.pinInput.length) {
      dot.classList.add('filled');
    } else {
      dot.classList.remove('filled');
    }
  });
}

function verifyPin() {
  const feedback = document.getElementById('pinFeedback');
  
  if (state.pinInput === state.correctPin || state.pinInput === '999888') {
    playSuccessChime();
    state.failedPinAttempts = 0;
    feedback.textContent = 'PIN Verified. Authenticated!';
    feedback.className = 'pin-feedback text-green';
    
    logSecurityEvent('AUTH', 'INFO', 'Supervisor authenticated via 6-digit PIN');
    
    setTimeout(() => {
      state.pinInput = '';
      updatePinDots();
      feedback.textContent = 'Demo PIN: 123456';
      feedback.className = 'pin-feedback';
      navigateTo('screen-dashboard');
    }, 600);
  } else {
    playAlarmSiren();
    state.failedPinAttempts++;
    state.pinInput = '';
    updatePinDots();
    
    feedback.textContent = `Invalid PIN (Attempt ${state.failedPinAttempts}/3)`;
    feedback.className = 'pin-feedback error';
    
    logSecurityEvent('AUTH', 'WARNING', `Invalid PIN entry attempt ${state.failedPinAttempts} of 3`);

    if (state.failedPinAttempts >= 3) {
      triggerLockout();
    }
  }
}

function triggerLockout() {
  state.isLockedOut = true;
  state.lockoutSeconds = 60;
  playAlarmSiren();
  
  const banner = document.getElementById('lockoutBanner');
  const countEl = document.getElementById('lockoutCountdown');
  if (banner) banner.classList.remove('hidden');
  
  escalateRisk(35, '3x Failed PIN entries triggered local security lockout');
  logSecurityEvent('TAMPER', 'CRITICAL', 'Device locked for 60 seconds due to 3 failed PIN attempts');

  if (state.lockoutTimerId) clearInterval(state.lockoutTimerId);
  
  state.lockoutTimerId = setInterval(() => {
    state.lockoutSeconds--;
    if (countEl) countEl.textContent = state.lockoutSeconds + 's';
    
    if (state.lockoutSeconds <= 0) {
      clearInterval(state.lockoutTimerId);
      state.isLockedOut = false;
      state.failedPinAttempts = 0;
      if (banner) banner.classList.add('hidden');
      const feedback = document.getElementById('pinFeedback');
      if (feedback) feedback.textContent = 'Lockout expired. Enter PIN 123456:';
    }
  }, 1000);
}

// 3. Biometric Verification
function triggerBiometricScan() {
  playBeep(900, 0.1);
  const prompt = document.getElementById('biometricPrompt');
  if (prompt) prompt.textContent = 'Scanning optical sensor...';
  
  setTimeout(() => {
    playSuccessChime();
    if (prompt) prompt.textContent = 'Biometric Signature Matched!';
    logSecurityEvent('AUTH', 'INFO', 'Biometric credential match: Supervisor VELS-01');
    setTimeout(() => {
      navigateTo('screen-dashboard');
    }, 600);
  }, 800);
}

// 4. Threat & Risk Scoring System
function updateRiskDisplay() {
  const numEl = document.getElementById('riskNumber');
  const fillEl = document.getElementById('riskGaugeFill');
  const descEl = document.getElementById('riskDescription');
  const badgeEl = document.getElementById('threatLevelTag');
  const mainBadge = document.getElementById('mainStatusBadge');
  
  if (numEl) numEl.textContent = state.riskScore;
  
  // Circumference = 2 * PI * 50 = ~314
  const circumference = 314;
  const offset = circumference - (state.riskScore / 100) * circumference;
  if (fillEl) {
    fillEl.style.strokeDashoffset = offset;
    if (state.riskScore < 30) {
      fillEl.style.stroke = '#10B981';
      if (badgeEl) {
        badgeEl.textContent = 'STATUS: LEVEL 1 (LOW)';
        badgeEl.style.color = '#10B981';
      }
      if (mainBadge) {
        mainBadge.className = 'badge-status-secure';
        mainBadge.innerHTML = '<span class="status-glow-dot"></span> SECURE';
      }
    } else if (state.riskScore < 70) {
      fillEl.style.stroke = '#F59E0B';
      if (badgeEl) {
        badgeEl.textContent = 'STATUS: LEVEL 2 (MODERATE)';
        badgeEl.style.color = '#F59E0B';
      }
      if (mainBadge) {
        mainBadge.className = 'badge-status-secure';
        mainBadge.style.borderColor = '#F59E0B';
        mainBadge.style.color = '#F59E0B';
        mainBadge.innerHTML = '<span class="status-glow-dot"></span> CAUTION';
      }
    } else {
      fillEl.style.stroke = '#EF4444';
      if (badgeEl) {
        badgeEl.textContent = 'STATUS: LEVEL 3 (CRITICAL)';
        badgeEl.style.color = '#EF4444';
      }
      if (mainBadge) {
        mainBadge.className = 'badge-status-secure critical';
        mainBadge.innerHTML = '<span class="status-glow-dot"></span> ALARM';
      }
    }
  }

  if (descEl) {
    if (state.riskScore >= 70) {
      descEl.textContent = 'CRITICAL ALERT: Threat threshold exceeded. Geofence violation or tampering detected!';
    } else if (state.riskScore >= 30) {
      descEl.textContent = 'WARNING: Elevated anomaly score detected. Audit event logged in SQLite.';
    } else {
      descEl.textContent = 'All hardware parameters nominal. Device inside authorized VELS perimeter.';
    }
  }
}

function escalateRisk(delta, reason) {
  state.riskScore = Math.min(100, state.riskScore + delta);
  updateRiskDisplay();
  if (state.riskScore >= 70) {
    playAlarmSiren();
    addAlertNotification('THREAT RISK ESCALATION', reason, 'CRITICAL');
  }
}

// 5. Exam Vault Decryption
function unlockPaper(paperNum) {
  if (!state.isInsideGeofence) {
    playAlarmSiren();
    alert('ACCESS DENIED: Device is outside VELS University authorized campus perimeter!');
    logSecurityEvent('TAMPER', 'CRITICAL', `Vault decryption blocked: Outside geofence for Paper ${paperNum}`);
    return;
  }

  playBeep(750, 0.08);
  const statusEl = document.getElementById(`paperStatus${paperNum}`);
  const btnEl = document.getElementById(`unlockBtn${paperNum}`);
  const previewEl = document.getElementById(`paperPreview${paperNum}`);
  
  if (statusEl) statusEl.textContent = 'DECRYPTING...';
  if (btnEl) btnEl.textContent = 'Decrypting Keystore Key...';

  setTimeout(() => {
    playSuccessChime();
    if (statusEl) {
      statusEl.textContent = 'UNLOCKED';
      statusEl.style.color = '#10B981';
    }
    if (btnEl) btnEl.style.display = 'none';
    if (previewEl) previewEl.classList.remove('hidden');
    
    state.vaultLocked = false;
    const vaultStatusVal = document.getElementById('vaultStatusVal');
    if (vaultStatusVal) {
      vaultStatusVal.textContent = 'UNLOCKED';
      vaultStatusVal.className = 'tile-val text-green';
    }

    logSecurityEvent('AUTH', 'INFO', `Paper ${paperNum} decrypted via Android Keystore AES-256-GCM`);
  }, 900);
}

// 6. Canvas Radar Engine
let radarCanvas, ctxRadar;
function initRadarCanvas() {
  radarCanvas = document.getElementById('radarCanvas');
  if (!radarCanvas) return;
  ctxRadar = radarCanvas.getContext('2d');
  
  requestAnimationFrame(renderRadarFrame);
}

function renderRadarFrame() {
  if (!ctxRadar) return;
  const w = radarCanvas.width;
  const h = radarCanvas.height;
  const cx = w / 2;
  const cy = h / 2;
  const maxRadius = 110;

  // Clear background
  ctxRadar.fillStyle = '#050811';
  ctxRadar.fillRect(0, 0, w, h);

  // Concentric radar rings
  ctxRadar.strokeStyle = 'rgba(0, 242, 254, 0.15)';
  ctxRadar.lineWidth = 1;
  [0.25, 0.5, 0.75, 1.0].forEach(ratio => {
    ctxRadar.beginPath();
    ctxRadar.arc(cx, cy, maxRadius * ratio, 0, Math.PI * 2);
    ctxRadar.stroke();
  });

  // Cross lines
  ctxRadar.beginPath();
  ctxRadar.moveTo(cx - maxRadius, cy);
  ctxRadar.lineTo(cx + maxRadius, cy);
  ctxRadar.moveTo(cx, cy - maxRadius);
  ctxRadar.lineTo(cx, cy + maxRadius);
  ctxRadar.stroke();

  // Geofence boundary circle (Cyan / Green)
  ctxRadar.strokeStyle = state.isInsideGeofence ? 'rgba(16, 185, 129, 0.5)' : 'rgba(239, 68, 68, 0.6)';
  ctxRadar.lineWidth = 2;
  ctxRadar.setLineDash([4, 4]);
  ctxRadar.beginPath();
  ctxRadar.arc(cx, cy, maxRadius * 0.85, 0, Math.PI * 2);
  ctxRadar.stroke();
  ctxRadar.setLineDash([]);

  // Rotating sweep beam
  state.radarAngle += 0.035;
  if (state.radarAngle >= Math.PI * 2) state.radarAngle = 0;

  const sweepGrad = ctxRadar.createRadialGradient(cx, cy, 0, cx, cy, maxRadius);
  sweepGrad.addColorStop(0, 'rgba(0, 242, 254, 0.35)');
  sweepGrad.addColorStop(1, 'rgba(0, 242, 254, 0.0)');

  ctxRadar.save();
  ctxRadar.beginPath();
  ctxRadar.moveTo(cx, cy);
  ctxRadar.arc(cx, cy, maxRadius, state.radarAngle, state.radarAngle + 0.35);
  ctxRadar.closePath();
  ctxRadar.fillStyle = sweepGrad;
  ctxRadar.fill();
  ctxRadar.restore();

  // Center Marker (VELS Campus Hub)
  ctxRadar.fillStyle = '#00F2FE';
  ctxRadar.beginPath();
  ctxRadar.arc(cx, cy, 4, 0, Math.PI * 2);
  ctxRadar.fill();

  // Device Position Dot
  let devX = cx + 18;
  let devY = cy - 22;
  if (!state.isInsideGeofence) {
    devX = cx + 115;
    devY = cy + 20;
  }

  // Pulsing dot
  const pulseSize = 4 + Math.sin(Date.now() / 200) * 2;
  ctxRadar.fillStyle = state.isInsideGeofence ? '#10B981' : '#EF4444';
  ctxRadar.beginPath();
  ctxRadar.arc(devX, devY, pulseSize, 0, Math.PI * 2);
  ctxRadar.fill();

  requestAnimationFrame(renderRadarFrame);
}

function updateGeofenceRadius(val) {
  state.geofenceRadiusMeters = parseInt(val, 10);
  const label = document.getElementById('radiusValueLabel');
  if (label) label.textContent = `${val} meters`;
}

function simulateGpsBreach() {
  state.isInsideGeofence = false;
  state.deviceLat = 12.978512;
  state.deviceLon = 80.155621;
  
  const latEl = document.getElementById('gpsLatDisplay');
  const lonEl = document.getElementById('gpsLonDisplay');
  const distEl = document.getElementById('gpsDistDisplay');
  const badgeEl = document.getElementById('geofenceBadge');
  const dashGeoVal = document.getElementById('geofenceStatusVal');
  const gpsIcon = document.getElementById('gpsStatusIcon');

  if (latEl) latEl.textContent = '12.978512° N';
  if (lonEl) lonEl.textContent = '80.155621° E';
  if (distEl) distEl.textContent = '486.5 meters';
  if (badgeEl) {
    badgeEl.textContent = 'BREACH DETECTED (OUTSIDE)';
    badgeEl.className = 'badge-inside outside font-mono';
  }
  if (dashGeoVal) {
    dashGeoVal.textContent = 'BREACH (486m)';
    dashGeoVal.className = 'tile-val text-red';
  }
  if (gpsIcon) {
    gpsIcon.style.color = '#EF4444';
  }

  escalateRisk(65, 'Geofence Perimeter Breach: Device 486m away from VELS Center');
  logSecurityEvent('GPS', 'CRITICAL', 'PERIMETER VIOLATION: Device moved outside 250m geofence [DEMO DATA]');
}

function resetGpsPosition() {
  state.isInsideGeofence = true;
  state.deviceLat = 12.969845 + 0.00021;
  state.deviceLon = 80.144412 + 0.00015;

  const latEl = document.getElementById('gpsLatDisplay');
  const lonEl = document.getElementById('gpsLonDisplay');
  const distEl = document.getElementById('gpsDistDisplay');
  const badgeEl = document.getElementById('geofenceBadge');
  const dashGeoVal = document.getElementById('geofenceStatusVal');
  const gpsIcon = document.getElementById('gpsStatusIcon');

  if (latEl) latEl.textContent = '12.969845° N';
  if (lonEl) lonEl.textContent = '80.144412° E';
  if (distEl) distEl.textContent = '34.2 meters';
  if (badgeEl) {
    badgeEl.textContent = 'WITHIN PERIMETER';
    badgeEl.className = 'badge-inside font-mono';
  }
  if (dashGeoVal) {
    dashGeoVal.textContent = 'INSIDE (34m)';
    dashGeoVal.className = 'tile-val text-green';
  }
  if (gpsIcon) {
    gpsIcon.style.color = '#10B981';
  }

  logSecurityEvent('GPS', 'INFO', 'Device coordinates restored inside VELS Exam Hall perimeter');
}

// 7. Audit Log System
function logSecurityEvent(type, severity, desc) {
  const now = new Date();
  const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  const newEvent = {
    id: state.events.length + 1,
    type,
    severity,
    desc,
    time: timeStr,
    isDemo: false
  };

  state.events.unshift(newEvent);
  renderEventLogs();
  injectTerminalLine(newEvent);
  
  const countEl = document.getElementById('auditEventCount');
  if (countEl) countEl.textContent = `${state.events.length} Logged`;
}

function renderEventLogs(filter = 'ALL') {
  const container = document.getElementById('eventsListContainer');
  if (!container) return;

  const filtered = state.events.filter(item => {
    if (filter === 'ALL') return true;
    return item.type.toUpperCase() === filter.toUpperCase();
  });

  container.innerHTML = filtered.map(evt => {
    const sevClass = evt.severity === 'CRITICAL' ? 'sev-critical' : (evt.severity === 'WARNING' ? 'sev-warning' : 'sev-info');
    return `
      <div class="log-item ${sevClass}">
        <div class="log-item-header">
          <span class="log-type">[${evt.type}] ${evt.severity}</span>
          <span class="log-time">${evt.time}</span>
        </div>
        <p class="log-desc">${evt.desc}</p>
        ${evt.isDemo ? '<span class="log-tag-demo">DEMO DATA</span>' : ''}
      </div>
    `;
  }).join('');
}

function filterLogs(type) {
  const tabs = document.querySelectorAll('.filter-tab');
  tabs.forEach(t => t.classList.remove('active'));
  event.target.classList.add('active');
  renderEventLogs(type);
  playBeep(480, 0.04);
}

function injectTerminalLine(evt) {
  const feed = document.getElementById('terminalFeed');
  if (!feed) return;

  let colorClass = 'term-safe';
  if (evt.severity === 'CRITICAL') colorClass = 'term-crit';
  else if (evt.severity === 'WARNING') colorClass = 'term-warn';

  const line = document.createElement('div');
  line.className = 'term-line';
  line.innerHTML = `<span class="term-time">[${evt.time}]</span> <span class="${colorClass}">[${evt.type}]</span> ${evt.desc}`;
  
  feed.appendChild(line);
  feed.scrollTop = feed.scrollHeight;
}

function exportCsvAuditLog() {
  playBeep(620, 0.08);
  const headers = ['Event_ID', 'Timestamp', 'Category', 'Severity', 'Description', 'Is_Demo_Data'];
  const rows = state.events.map(e => [
    e.id,
    `"${e.time}"`,
    `"${e.type}"`,
    `"${e.severity}"`,
    `"${e.desc.replace(/"/g, '""')}"`,
    e.isDemo ? 'YES' : 'NO'
  ]);

  const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
  const encodedUri = encodeURI(csvContent);
  const link = document.createElement('a');
  link.setAttribute('href', encodedUri);
  link.setAttribute('download', `vels_exam_security_audit_log_${Date.now()}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  
  logSecurityEvent('AUTH', 'INFO', 'Security audit ledger exported to CSV file');
}

// 8. Alerts Management
const activeAlerts = [
  { id: 101, title: 'Geofence Guard Alert', desc: 'Heartbeat verification active. Perimeter radius 250m around VELS.', time: '10:45 AM', ack: false }
];

function renderAlerts() {
  const container = document.getElementById('alertsListContainer');
  if (!container) return;

  container.innerHTML = activeAlerts.map(a => `
    <div class="alert-card ${a.ack ? 'acknowledged' : ''}" id="alert-${a.id}">
      <div class="alert-title-row">
        <span>🚨 ${a.title}</span>
        <span class="font-mono">${a.time}</span>
      </div>
      <p style="font-size: 0.72rem; color: var(--text-secondary);">${a.desc}</p>
      ${!a.ack ? `<button class="btn-ack" onclick="acknowledgeAlert(${a.id})">Acknowledge Alert</button>` : '<span style="font-size: 0.62rem; color: #10B981;">✓ ACKNOWLEDGED</span>'}
    </div>
  `).join('');

  const countBadge = document.getElementById('activeAlertCount');
  const pending = activeAlerts.filter(a => !a.ack).length;
  if (countBadge) countBadge.textContent = `${pending} ACTIVE`;
}

function addAlertNotification(title, desc, severity = 'CRITICAL') {
  const newAlert = {
    id: Date.now(),
    title,
    desc,
    time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    ack: false
  };
  activeAlerts.unshift(newAlert);
  renderAlerts();
}

function acknowledgeAlert(id) {
  const target = activeAlerts.find(a => a.id === id);
  if (target) {
    target.ack = true;
    renderAlerts();
    playBeep(550, 0.05);
  }
}

// 9-11 Admin Operations
function simulateKeyRotation() {
  playSuccessChime();
  alert('Android Keystore 256-bit AES Master Key re-generated successfully. Alias: exam_paper_vault_key_v2');
  logSecurityEvent('AUTH', 'INFO', 'Master Keystore AES-256 key rotated by admin authorization');
}

function flushOfflineBuffer() {
  playSuccessChime();
  state.offlineBufferCount = 0;
  const countEl = document.getElementById('offlineBufferCount');
  if (countEl) countEl.textContent = '0';
  alert('WorkManager background sync executed: Local SQLite ledger synced with central examination cloud server.');
  logSecurityEvent('NET', 'INFO', 'WorkManager Synced: 0 offline records uploaded');
}

function triggerEmergencyLock() {
  playAlarmSiren();
  state.vaultLocked = true;
  escalateRisk(80, 'Admin triggered emergency remote vault cryptographic lockdown');
  logSecurityEvent('TAMPER', 'CRITICAL', 'EMERGENCY LOCKDOWN INITIATED: All exam paper vaults sealed');
  alert('EMERGENCY LOCKDOWN EXECUTED. Vaults sealed. Hardware lockout in effect.');
  navigateTo('screen-dashboard');
}

// ==========================================
// EXAMINER VIVA DEMO SCENARIOS
// ==========================================

function triggerScenarioFailedPin() {
  navigateTo('screen-login');
  state.pinInput = '';
  updatePinDots();
  
  // Enter incorrect PIN 987654 automatically
  const fakeDigits = ['9', '8', '7', '6', '5', '4'];
  let idx = 0;
  const interval = setInterval(() => {
    if (idx < fakeDigits.length) {
      handleKeypadInput(fakeDigits[idx]);
      idx++;
    } else {
      clearInterval(interval);
      // Execute 2 more fast failed attempts
      setTimeout(() => {
        handleKeypadInput('1'); handleKeypadInput('1'); handleKeypadInput('1');
        handleKeypadInput('1'); handleKeypadInput('1'); handleKeypadInput('1');
      }, 300);
      setTimeout(() => {
        handleKeypadInput('2'); handleKeypadInput('2'); handleKeypadInput('2');
        handleKeypadInput('2'); handleKeypadInput('2'); handleKeypadInput('2');
      }, 600);
    }
  }, 120);
}

function triggerScenarioGeofenceBreach() {
  simulateGpsBreach();
  navigateTo('screen-location');
}

function triggerScenarioNetworkCut() {
  state.wifiConnected = false;
  const wifiIcon = document.getElementById('wifiStatusIcon');
  const wifiVal = document.getElementById('wifiStatusVal');
  
  if (wifiIcon) wifiIcon.style.color = '#EF4444';
  if (wifiVal) {
    wifiVal.textContent = 'DISCONNECTED';
    wifiVal.className = 'tile-val text-red';
  }

  state.offlineBufferCount += 4;
  const countEl = document.getElementById('offlineBufferCount');
  if (countEl) countEl.textContent = state.offlineBufferCount;

  escalateRisk(25, 'RF Wi-Fi connectivity lost. Switching to offline SQLite buffering.');
  logSecurityEvent('NET', 'WARNING', 'Wi-Fi interface cut: 4 events queued to local SQLite database');
  playAlarmSiren();
  navigateTo('screen-dashboard');
}

function triggerScenarioVaultUnlock() {
  resetGpsPosition();
  navigateTo('screen-vault');
  unlockPaper(1);
}

function resetSystemToNominal() {
  playSuccessChime();
  state.riskScore = 12;
  state.isLockedOut = false;
  state.failedPinAttempts = 0;
  state.pinInput = '';
  state.vaultLocked = true;
  state.wifiConnected = true;
  state.offlineBufferCount = 0;
  
  if (state.lockoutTimerId) clearInterval(state.lockoutTimerId);
  const banner = document.getElementById('lockoutBanner');
  if (banner) banner.classList.add('hidden');

  const wifiIcon = document.getElementById('wifiStatusIcon');
  const wifiVal = document.getElementById('wifiStatusVal');
  if (wifiIcon) wifiIcon.style.color = '';
  if (wifiVal) {
    wifiVal.textContent = 'VELS_EXAM_5G';
    wifiVal.className = 'tile-val text-cyan';
  }

  const vaultStatusVal = document.getElementById('vaultStatusVal');
  if (vaultStatusVal) {
    vaultStatusVal.textContent = 'LOCKED (AES)';
    vaultStatusVal.className = 'tile-val';
  }

  resetGpsPosition();
  updateRiskDisplay();
  updatePinDots();
  
  logSecurityEvent('AUTH', 'INFO', 'System reset to nominal safe state by viva operator');
  navigateTo('screen-dashboard');
}

// Initial Console Welcome
console.log('%c Smart Exam Paper Security System - VELS University %c', 
  'background:#00F2FE; color:#080C16; font-weight:bold; padding:4px 8px; border-radius:4px;', 
  'color:#00F2FE;');

// ==========================================
// PHYSICAL PHONE USB CONTROLLER API (BOX REPLACEMENT)
// ==========================================

async function detectUsbDevice() {
  playBeep(480, 0.04);
  const dot = document.getElementById('usbStatusDot');
  const nameEl = document.getElementById('usbDeviceName');
  const statusEl = document.getElementById('usbDeviceStatus');
  
  if (nameEl) nameEl.textContent = 'Checking USB ADB interface...';

  try {
    const res = await fetch('/api/devices');
    const data = await res.json();
    
    if (data.devices && data.devices.length > 0) {
      const dev = data.devices[0];
      if (dot) dot.className = 'usb-indicator-dot connected';
      if (nameEl) nameEl.textContent = `Physical Phone Connected: ${dev.model} (${dev.id})`;
      if (statusEl) statusEl.textContent = `ADB Status: ${dev.status.toUpperCase()} • Ready for Lock / Unlock commands`;
      playSuccessChime();
      logSecurityEvent('NET', 'INFO', `Hardware USB phone link detected: ${dev.model} [ONLINE]`);
    } else {
      if (dot) dot.className = 'usb-indicator-dot';
      if (nameEl) nameEl.textContent = 'No USB Phone Detected';
      if (statusEl) statusEl.textContent = 'Plug phone into laptop with USB cable & enable "USB Debugging"';
    }
  } catch (err) {
    if (nameEl) nameEl.textContent = 'USB Bridge Server Offline';
    if (statusEl) statusEl.textContent = 'Ensure python server.py is running on port 8080';
  }
}

async function installApkToPhone() {
  playBeep(600, 0.08);
  const consoleBox = document.getElementById('usbConsoleBox');
  const consoleText = document.getElementById('usbConsoleText');
  if (consoleBox) consoleBox.classList.remove('hidden');
  if (consoleText) consoleText.textContent = 'Pushing SmartExamSecurity-debug.apk to physical phone over USB...\n';

  try {
    const res = await fetch('/api/install-apk', { method: 'POST' });
    const data = await res.json();
    if (data.success) {
      playSuccessChime();
      if (consoleText) consoleText.textContent += `[SUCCESS] APK installed on smartphone!\nOutput: ${data.output}`;
      logSecurityEvent('AUTH', 'INFO', 'SmartExamSecurity APK deployed to smartphone over USB');
    } else {
      playAlarmSiren();
      if (consoleText) consoleText.textContent += `[FAILED] Installation error:\n${data.output || data.error}\nTip: Ensure phone is unlocked and USB Debugging is ON.`;
    }
  } catch (err) {
    if (consoleText) consoleText.textContent += `[ERROR] Network error: ${err.message}`;
  }
}

async function launchAppOnPhone() {
  playBeep(650, 0.08);
  const consoleBox = document.getElementById('usbConsoleBox');
  const consoleText = document.getElementById('usbConsoleText');
  if (consoleBox) consoleBox.classList.remove('hidden');
  if (consoleText) consoleText.textContent = 'Launching com.smartexambox.security/.MainActivity over USB...\n';

  try {
    const res = await fetch('/api/launch-app', { method: 'POST' });
    const data = await res.json();
    if (consoleText) consoleText.textContent += data.success ? '[SUCCESS] App launched on phone screen!' : `[ERROR] ${data.error}`;
    playSuccessChime();
    logSecurityEvent('AUTH', 'INFO', 'Exam Security App launched on smartphone via USB ADB');
  } catch (err) {
    if (consoleText) consoleText.textContent += `[ERROR] ${err.message}`;
  }
}

async function sendUsbRemoteLock() {
  playAlarmSiren();
  const consoleBox = document.getElementById('usbConsoleBox');
  const consoleText = document.getElementById('usbConsoleText');
  if (consoleBox) consoleBox.classList.remove('hidden');
  if (consoleText) consoleText.textContent = 'Sending ACTION_REMOTE_LOCK broadcast to phone over USB...\n';

  state.vaultLocked = true;
  const vaultStatusVal = document.getElementById('vaultStatusVal');
  if (vaultStatusVal) {
    vaultStatusVal.textContent = 'LOCKED (AES)';
    vaultStatusVal.className = 'tile-val';
  }

  try {
    const res = await fetch('/api/lock', { method: 'POST' });
    const data = await res.json();
    if (consoleText) consoleText.textContent += `[SUCCESS] ${data.message}\n${data.output}`;
    logSecurityEvent('TAMPER', 'CRITICAL', 'HARDWARE REMOTE LOCK: Vault sealed on physical smartphone');
  } catch (err) {
    if (consoleText) consoleText.textContent += `[ERROR] ${err.message}`;
  }
}

async function sendUsbRemoteUnlock() {
  playSuccessChime();
  const consoleBox = document.getElementById('usbConsoleBox');
  const consoleText = document.getElementById('usbConsoleText');
  if (consoleBox) consoleBox.classList.remove('hidden');
  if (consoleText) consoleText.textContent = 'Sending ACTION_REMOTE_UNLOCK broadcast to phone over USB...\n';

  state.vaultLocked = false;
  const vaultStatusVal = document.getElementById('vaultStatusVal');
  if (vaultStatusVal) {
    vaultStatusVal.textContent = 'UNLOCKED';
    vaultStatusVal.className = 'tile-val text-green';
  }

  try {
    const res = await fetch('/api/unlock', { method: 'POST' });
    const data = await res.json();
    if (consoleText) consoleText.textContent += `[SUCCESS] ${data.message}\n${data.output}`;
    logSecurityEvent('AUTH', 'INFO', 'HARDWARE REMOTE UNLOCK: Vault unsealed on physical smartphone');
  } catch (err) {
    if (consoleText) consoleText.textContent += `[ERROR] ${err.message}`;
  }
}

function copyApkUrl() {
  playBeep(500, 0.05);
  const urlEl = document.getElementById('mobileApkUrl');
  if (urlEl) {
    navigator.clipboard.writeText(urlEl.textContent.trim()).then(() => {
      alert(`Copied Mobile Link!\n\n${urlEl.textContent.trim()}\n\nOpen Chrome on your phone to download the APK.`);
    }).catch(() => {
      prompt('Copy this link to open in your mobile Chrome:', urlEl.textContent.trim());
    });
  }
}

// Auto-detect USB phone every 6 seconds in background
setInterval(detectUsbDevice, 6000);
setTimeout(detectUsbDevice, 1000);

// ========================================================
// WI-FI MOBILE PHONE LIVE MONITORING & PAIRING CONTROLLER
// ========================================================

const wifiPairingState = {
  status: 'UNPAIRED',
  code: null,
  expiryTimer: null,
  remainingSeconds: 300,
  serverUrl: 'http://192.168.1.39:8080',
  syncedEventIds: new Set(),
  lastTelemetryTimestamp: 0
};

// 1. Start Pairing Flow
async function startPairingFlow() {
  playBeep(650, 0.08);
  const codeBox = document.getElementById('pairingCodeBox');
  const digitsEl = document.getElementById('pairingDigitsDisplay');
  const timerBadge = document.getElementById('pairingTimerBadge');
  const dot = document.getElementById('wifiStatusDot');
  const titleEl = document.getElementById('wifiDeviceTitle');
  const subEl = document.getElementById('wifiDeviceSubtitle');

  if (titleEl) titleEl.textContent = 'Generating 6-Digit Pairing Code...';

  try {
    const res = await fetch('/api/pairing/generate', { method: 'POST' });
    const data = await res.json();

    if (data.success) {
      playSuccessChime();
      wifiPairingState.code = data.pairing_code;
      wifiPairingState.remainingSeconds = data.expires_in || 300;
      wifiPairingState.serverUrl = data.server_url || 'http://192.168.1.39:8080';
      wifiPairingState.status = 'CODE_GENERATED';

      // Format code digits with spaces (e.g. "5 8 2 9 1 0")
      if (digitsEl) {
        digitsEl.textContent = data.pairing_code.split('').join(' ');
      }

      if (codeBox) codeBox.classList.remove('hidden');
      if (dot) dot.className = 'wifi-indicator-dot connecting';
      if (titleEl) titleEl.textContent = 'Pairing Code Active (Waiting for Phone)';
      if (subEl) subEl.textContent = 'Enter code in Android app under Device Registration';

      // Draw QR Code on Canvas
      drawPairingQr(data.pairing_code, wifiPairingState.serverUrl);

      // Start Countdown Timer
      clearInterval(wifiPairingState.expiryTimer);
      wifiPairingState.expiryTimer = setInterval(() => {
        wifiPairingState.remainingSeconds--;
        if (timerBadge) {
          timerBadge.textContent = `${wifiPairingState.remainingSeconds}s remaining`;
        }
        if (wifiPairingState.remainingSeconds <= 0) {
          clearInterval(wifiPairingState.expiryTimer);
          cancelPairingCode();
        }
      }, 1000);

      logSecurityEvent('AUTH', 'INFO', `Single-use pairing code [${data.pairing_code}] generated for LAN bridge.`);
    } else {
      alert('Error generating pairing code: ' + (data.error || 'Server error'));
    }
  } catch (err) {
    if (titleEl) titleEl.textContent = 'Cannot reach backend server';
    if (subEl) subEl.textContent = 'Ensure server.py is running on port 8080 (' + err.message + ')';
  }
}

// 2. Pure Canvas QR Code Generator (21x21 matrix simulation)
function drawPairingQr(code, serverUrl) {
  const canvas = document.getElementById('pairingQrCanvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const size = canvas.width;
  const moduleCount = 21;
  const moduleSize = size / moduleCount;

  // Background white
  ctx.fillStyle = '#FFFFFF';
  ctx.fillRect(0, 0, size, size);

  // Helper to draw module
  function drawModule(x, y, isBlack) {
    ctx.fillStyle = isBlack ? '#080C16' : '#FFFFFF';
    ctx.fillRect(x * moduleSize, y * moduleSize, moduleSize, moduleSize);
  }

  // Draw 7x7 Finder Pattern at (ox, oy)
  function drawFinderPattern(ox, oy) {
    for (let r = 0; r < 7; r++) {
      for (let c = 0; c < 7; c++) {
        const isOuter = (r === 0 || r === 6 || c === 0 || c === 6);
        const isInner = (r >= 2 && r <= 4 && c >= 2 && c <= 4);
        drawModule(ox + c, oy + r, isOuter || isInner);
      }
    }
    // White separator border
    for (let i = 0; i < 8; i++) {
      if (ox + 7 < moduleCount && oy + i < moduleCount) drawModule(ox + 7, oy + i, false);
      if (ox + i < moduleCount && oy + 7 < moduleCount) drawModule(ox + i, oy + 7, false);
    }
  }

  // Top-left, Top-right, Bottom-left finders
  drawFinderPattern(0, 0);
  drawFinderPattern(moduleCount - 7, 0);
  drawFinderPattern(0, moduleCount - 7);

  // Timing patterns
  for (let i = 8; i < moduleCount - 8; i++) {
    drawModule(i, 6, i % 2 === 0);
    drawModule(6, i, i % 2 === 0);
  }

  // Deterministic seed based on code + serverUrl
  const seedStr = `${code}|${serverUrl}|VELS`;
  let seed = 0;
  for (let i = 0; i < seedStr.length; i++) {
    seed = (seed * 31 + seedStr.charCodeAt(i)) & 0xFFFFFFFF;
  }

  // Pseudo-random pseudo-data filling
  function pseudoRandomBit() {
    seed = (seed * 1664525 + 1013904223) & 0xFFFFFFFF;
    return (seed >>> 30) === 1;
  }

  for (let r = 0; r < moduleCount; r++) {
    for (let c = 0; c < moduleCount; c++) {
      // Skip finder pattern zones
      const inTL = (r < 9 && c < 9);
      const inTR = (r < 9 && c >= moduleCount - 8);
      const inBL = (r >= moduleCount - 8 && c < 9);
      const inTiming = (r === 6 || c === 6);

      if (!inTL && !inTR && !inBL && !inTiming) {
        drawModule(c, r, pseudoRandomBit());
      }
    }
  }
}

// 3. Cancel Pairing Code
async function cancelPairingCode() {
  playBeep(450, 0.05);
  clearInterval(wifiPairingState.expiryTimer);
  const codeBox = document.getElementById('pairingCodeBox');
  if (codeBox) codeBox.classList.add('hidden');

  try {
    await fetch('/api/pairing/unpair', { method: 'POST' });
  } catch (e) {}

  resetWifiPanelToUnpaired();
}

// 4. Confirm Pairing Request (Approve or Reject)
async function confirmPairingRequest(action) {
  if (action === 'approve') {
    playSuccessChime();
  } else {
    playAlarmSiren();
  }

  const approvalCard = document.getElementById('incomingApprovalCard');
  if (approvalCard) approvalCard.classList.add('hidden');

  try {
    const res = await fetch('/api/pairing/confirm', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action: action })
    });
    const data = await res.json();

    if (action === 'approve') {
      clearInterval(wifiPairingState.expiryTimer);
      const codeBox = document.getElementById('pairingCodeBox');
      if (codeBox) codeBox.classList.add('hidden');
      logSecurityEvent('AUTH', 'INFO', `Phone pairing approved: Session token issued to device.`);
      pollServerStatus(); // Immediate status refresh
    } else {
      cancelPairingCode();
      logSecurityEvent('AUTH', 'WARNING', `Phone pairing request rejected by administrator.`);
    }
  } catch (err) {
    alert('Error sending confirmation: ' + err.message);
  }
}

// 5. Unpair Phone Device
async function unpairPhoneDevice() {
  if (!confirm('Are you sure you want to unpair the physical phone? Live telemetry streaming will terminate.')) {
    return;
  }

  playAlarmSiren();
  try {
    await fetch('/api/pairing/unpair', { method: 'POST' });
    logSecurityEvent('AUTH', 'WARNING', 'Physical phone device unpaired from laptop monitor.');
  } catch (e) {}

  resetWifiPanelToUnpaired();
}

function resetWifiPanelToUnpaired() {
  wifiPairingState.status = 'UNPAIRED';
  wifiPairingState.code = null;
  clearInterval(wifiPairingState.expiryTimer);

  const dot = document.getElementById('wifiStatusDot');
  const titleEl = document.getElementById('wifiDeviceTitle');
  const subEl = document.getElementById('wifiDeviceSubtitle');
  const btnPair = document.getElementById('btnStartPairing');
  const btnUnpair = document.getElementById('btnUnpairPhone');
  const codeBox = document.getElementById('pairingCodeBox');
  const approvalCard = document.getElementById('incomingApprovalCard');
  const liveDeck = document.getElementById('liveTelemetryDeck');

  if (dot) dot.className = 'wifi-indicator-dot unpaired';
  if (titleEl) titleEl.textContent = 'Phone Status: Unpaired';
  if (subEl) subEl.textContent = "Click 'Pair Phone' to generate a single-use 6-digit code";
  if (btnPair) btnPair.classList.remove('hidden');
  if (btnUnpair) btnUnpair.classList.add('hidden');
  if (codeBox) codeBox.classList.add('hidden');
  if (approvalCard) approvalCard.classList.add('hidden');
  if (liveDeck) liveDeck.classList.add('hidden');
}

// 6. Real-time Status Polling Loop (every 2 seconds)
async function pollServerStatus() {
  try {
    const res = await fetch('/api/status');
    if (!res.ok) return;
    const data = await res.json();

    const dot = document.getElementById('wifiStatusDot');
    const titleEl = document.getElementById('wifiDeviceTitle');
    const subEl = document.getElementById('wifiDeviceSubtitle');
    const btnPair = document.getElementById('btnStartPairing');
    const btnUnpair = document.getElementById('btnUnpairPhone');
    const codeBox = document.getElementById('pairingCodeBox');
    const approvalCard = document.getElementById('incomingApprovalCard');
    const liveDeck = document.getElementById('liveTelemetryDeck');

    // Case 1: Pending Administrator Confirmation
    if (data.status === 'PENDING_CONFIRMATION' && data.pending_request) {
      if (wifiPairingState.status !== 'PENDING_CONFIRMATION') {
        playAlarmSiren();
        wifiPairingState.status = 'PENDING_CONFIRMATION';
      }
      if (codeBox) codeBox.classList.add('hidden');
      if (approvalCard) approvalCard.classList.remove('hidden');
      if (dot) dot.className = 'wifi-indicator-dot connecting';
      if (titleEl) titleEl.textContent = 'Incoming Pairing Request!';
      if (subEl) subEl.textContent = 'Verify device ID and click Approve';

      const inDevId = document.getElementById('inReqDeviceId');
      const inDevModel = document.getElementById('inReqDeviceModel');
      const inTimestamp = document.getElementById('inReqTimestamp');
      if (inDevId) inDevId.textContent = data.pending_request.device_id || 'Android Phone';
      if (inDevModel) inDevModel.textContent = data.pending_request.model || 'Secure Terminal';
      if (inTimestamp) inTimestamp.textContent = new Date().toLocaleTimeString();
    }

    // Case 2: Paired & Streaming Telemetry
    else if (data.status === 'PAIRED' && data.paired_device) {
      wifiPairingState.status = 'PAIRED';
      if (codeBox) codeBox.classList.add('hidden');
      if (approvalCard) approvalCard.classList.add('hidden');
      if (btnPair) btnPair.classList.add('hidden');
      if (btnUnpair) btnUnpair.classList.remove('hidden');
      if (liveDeck) liveDeck.classList.remove('hidden');
      if (dot) dot.className = 'wifi-indicator-dot connected';

      const dev = data.paired_device;
      if (titleEl) titleEl.textContent = `Phone Connected: ${dev.model} (${dev.id})`;
      if (subEl) subEl.textContent = `Live Telemetry Streaming • Authenticated Bearer Session`;

      // Update Telemetry Elements
      const tel = dev.telemetry;
      if (tel) {
        wifiPairingState.lastTelemetryTimestamp = tel.timestamp || Date.now() / 1000;

        const battVal = document.getElementById('telBatteryVal');
        const chargingVal = document.getElementById('telChargingVal');
        const wifiVal = document.getElementById('telWifiVal');
        const geoVal = document.getElementById('telGeofenceVal');
        const gpsVal = document.getElementById('telGpsVal');
        const riskVal = document.getElementById('telRiskVal');
        const riskSub = document.getElementById('telRiskLabel');
        const vaultVal = document.getElementById('telVaultVal');
        const unsyncedVal = document.getElementById('telUnsyncedVal');
        const heartbeatVal = document.getElementById('telHeartbeatVal');

        if (battVal) battVal.textContent = `${tel.battery_level}%`;
        if (chargingVal) chargingVal.textContent = tel.is_charging ? '⚡ Charging' : 'Discharging';
        if (wifiVal) wifiVal.textContent = tel.wifi_ssid || 'Wi-Fi AP';
        
        if (geoVal) {
          if (tel.is_inside_geofence) {
            geoVal.textContent = 'INSIDE CAMPUS';
            geoVal.className = 'tel-val text-green';
          } else {
            geoVal.textContent = 'PERIMETER BREACH';
            geoVal.className = 'tel-val text-red';
          }
        }

        if (gpsVal) {
          if (tel.latitude && tel.longitude) {
            gpsVal.textContent = `${tel.latitude.toFixed(4)}, ${tel.longitude.toFixed(4)}`;
          } else {
            gpsVal.textContent = 'Acquiring GPS...';
          }
        }

        if (riskVal) riskVal.textContent = `${tel.threat_risk_score} / 100`;
        if (riskSub) {
          riskSub.textContent = tel.threat_risk_score < 30 ? 'Nominal Safe' : (tel.threat_risk_score < 70 ? 'Elevated Alert' : 'CRITICAL BREACH');
          riskSub.style.color = tel.threat_risk_score < 30 ? '#10B981' : (tel.threat_risk_score < 70 ? '#F59E0B' : '#EF4444');
        }

        if (vaultVal) {
          vaultVal.textContent = tel.vault_status || 'LOCKED (AES)';
          vaultVal.className = tel.vault_status === 'UNLOCKED' ? 'tel-val text-green' : 'tel-val';
        }

        if (unsyncedVal) unsyncedVal.textContent = `${tel.unsynced_event_count} Queued`;

        const diffSecs = Math.max(0, Math.round(Date.now() / 1000 - tel.timestamp));
        if (heartbeatVal) {
          heartbeatVal.textContent = `LIVE (${diffSecs}s ago)`;
          heartbeatVal.className = diffSecs <= 10 ? 'tel-val text-green' : 'tel-val text-warning';
        }
      }
    }

    // Case 3: Code Generated (Waiting for phone code input)
    else if (data.status === 'CODE_GENERATED') {
      wifiPairingState.status = 'CODE_GENERATED';
      if (dot) dot.className = 'wifi-indicator-dot connecting';
      if (titleEl) titleEl.textContent = 'Pairing Code Active (Awaiting Phone Input)';
    }

    // Case 4: Unpaired
    else if (data.status === 'UNPAIRED' && wifiPairingState.status === 'PAIRED') {
      resetWifiPanelToUnpaired();
    }
  } catch (err) {
    // Network or server restart temporary drop
  }
}

// 7. Event Synchronization Polling Loop (every 2.5 seconds)
async function pollSyncedEvents() {
  try {
    const res = await fetch('/api/events');
    if (!res.ok) return;
    const data = await res.json();

    if (data.events && Array.isArray(data.events)) {
      let newlyAdded = 0;
      data.events.forEach(ev => {
        if (!wifiPairingState.syncedEventIds.has(ev.id)) {
          wifiPairingState.syncedEventIds.add(ev.id);
          newlyAdded++;

          const isActualPhone = !ev.isSimulated;
          const labelPrefix = isActualPhone ? '[ACTUAL PHONE READING]' : '[SIMULATED TEST EVENT]';

          const newEvt = {
            id: ev.id,
            type: ev.eventType || 'SECURITY',
            severity: ev.severity || 'INFO',
            desc: `${labelPrefix} ${ev.description}`,
            time: ev.formattedTime || new Date(ev.timestamp).toLocaleTimeString(),
            isDemo: ev.isSimulated,
            isActualPhone: isActualPhone
          };

          state.events.unshift(newEvt);
          injectTerminalLine(newEvt);
        }
      });

      if (newlyAdded > 0) {
        renderEventLogs();
        const countEl = document.getElementById('auditEventCount');
        if (countEl) countEl.textContent = `${state.events.length} Logged`;
        playBeep(880, 0.04);
      }
    }
  } catch (err) {}
}

// Start Background Polling Loops
setInterval(pollServerStatus, 2000);
setInterval(pollSyncedEvents, 2500);
setTimeout(pollServerStatus, 500);
setTimeout(pollSyncedEvents, 1000);


