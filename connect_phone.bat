@echo off
title Smart Exam Paper Security - USB Mobile Controller Bridge
color 0b
chcp 65001 >nul

set ADB="%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if not exist %ADB% set ADB="C:\Users\suryx\AppData\Local\Android\Sdk\platform-tools\adb.exe"
if not exist %ADB% set ADB=adb
set APK="%~dp0SmartExamSecurity-debug.apk"
set PKG=com.smartexambox.security
set ACTIVITY=com.smartexambox.security/.MainActivity

:MENU
cls
echo ===============================================================================
echo      SMART EXAM PAPER SECURITY SYSTEM - USB MOBILE CONTROLLER BRIDGE
echo         VELS Institute of Science, Technology ^& Advanced Studies (VISTAS)
echo ===============================================================================
echo.
echo [1] Checking USB Connected Android Phone...
%ADB% devices -l
echo.
echo -------------------------------------------------------------------------------
echo SELECT AN ACTION TO EXECUTE ON THE CONNECTED SMARTPHONE:
echo -------------------------------------------------------------------------------
echo  [1] Install SmartExamSecurity APK to Phone over USB Cable
echo  [2] Launch Exam Security App on Phone
echo  [3] Set Up USB Network Reverse Tunnel (tcp:8080)
echo  [4] Send REMOTE LOCK to Phone Vault (Hardware Lock Simulation)
echo  [5] Send REMOTE UNLOCK to Phone Vault (Hardware Unseal Simulation)
echo  [6] Stream Live Real-Time Security Logs from Phone (Logcat)
echo  [7] Query Phone Battery, Wi-Fi and Sensor Status
echo  [8] Open Chrome Web Dashboard on Laptop
echo  [9] Refresh Device Connection Status
echo  [0] Exit
echo -------------------------------------------------------------------------------
set /p opt="Enter choice [0-9]: "

if "%opt%"=="1" goto INSTALL_APK
if "%opt%"=="2" goto LAUNCH_APP
if "%opt%"=="3" goto REVERSE_TUNNEL
if "%opt%"=="4" goto REMOTE_LOCK
if "%opt%"=="5" goto REMOTE_UNLOCK
if "%opt%"=="6" goto LOGCAT_STREAM
if "%opt%"=="7" goto PHONE_STATUS
if "%opt%"=="8" goto OPEN_CHROME
if "%opt%"=="9" goto MENU
if "%opt%"=="0" exit
goto MENU

:INSTALL_APK
echo.
echo [*] Installing APK to connected phone over USB cable...
echo [*] APK Path: %APK%
%ADB% install -r %APK%
if %errorlevel% equ 0 (
    echo.
    echo [✓] SUCCESS: Smart Exam Security App installed on your phone!
    echo [*] Starting app now...
    %ADB% shell am start -n %ACTIVITY%
) else (
    echo.
    echo [X] FAILED: Make sure your phone is unlocked and USB Debugging is turned ON.
)
pause
goto MENU

:LAUNCH_APP
echo.
echo [*] Launching Smart Exam Security app on phone...
%ADB% shell am start -n %ACTIVITY%
pause
goto MENU

:REVERSE_TUNNEL
echo.
echo [*] Setting up reverse TCP tunnel on port 8080...
%ADB% reverse tcp:8080 tcp:8080
if %errorlevel% equ 0 (
    echo [✓] SUCCESS: Reverse tunnel active! Your phone can access http://localhost:8080 through the USB cable!
) else (
    echo [X] FAILED to set up reverse tunnel. Check ADB connection.
)
pause
goto MENU

:REMOTE_LOCK
echo.
echo [*] Sending REMOTE LOCK signal to smartphone over USB...
%ADB% shell am broadcast -a com.smartexambox.security.ACTION_REMOTE_LOCK
echo [✓] Broadcast sent: Phone vault sealed and locked!
pause
goto MENU

:REMOTE_UNLOCK
echo.
echo [*] Sending REMOTE UNLOCK signal to smartphone over USB...
%ADB% shell am broadcast -a com.smartexambox.security.ACTION_REMOTE_UNLOCK
echo [✓] Broadcast sent: Phone vault unsealed and unlocked!
pause
goto MENU

:LOGCAT_STREAM
echo.
echo ===============================================================================
echo [*] Streaming live security events from phone (Press Ctrl+C to stop)...
echo ===============================================================================
%ADB% logcat -c
%ADB% logcat -v time | findstr /i "SmartExam Security Keystore Geofence Vault"
pause
goto MENU

:PHONE_STATUS
echo.
echo [*] Battery Status from Phone:
%ADB% shell dumpsys battery | findstr /i "level status scale health technology"
echo.
echo [*] Wi-Fi Status from Phone:
%ADB% shell dumpsys wifi | findstr /i "mWifiInfo SSID BSSID RSSI"
pause
goto MENU

:OPEN_CHROME
start http://localhost:8080
goto MENU
