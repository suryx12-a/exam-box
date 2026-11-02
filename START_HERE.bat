@echo off
title Smart Exam Paper Security - Master Control Menu
color 0b
chcp 65001 >nul
cd /d "%~dp0"

:MAIN_MENU
cls
echo ===============================================================================
echo        SMART EXAM PAPER SECURITY AND MOBILE MONITORING SYSTEM
echo       VELS Institute of Science, Technology ^& Advanced Studies (VISTAS)
echo ===============================================================================
echo.
echo Welcome! This package contains the complete Android application, interactive
echo Chrome demonstration system, USB hardware bridge, and college viva presentation.
echo.
echo -------------------------------------------------------------------------------
echo SELECT AN OPTION TO GET STARTED:
echo -------------------------------------------------------------------------------
echo  [1] Launch Web Demonstration in Google Chrome (Live Emulator ^& Viva Deck)
echo  [2] Open USB Mobile Phone Controller (Install APK, Remote Lock/Unlock)
echo  [3] Directly Install APK to Connected Smartphone (via USB ADB)
echo  [4] Run Unit ^& Integration Tests (.\gradlew testDebugUnitTest)
echo  [5] Build Android Debug APK from Source (.\gradlew assembleDebug)
echo  [6] Clean Cache ^& Temporary Files (Prepare Clean ZIP to Send)
echo  [7] Open Project Documentation ^& Viva Guide (README.md)
echo  [0] Exit
echo -------------------------------------------------------------------------------
set /p choice="Enter option number [0-7]: "

if "%choice%"=="1" goto LAUNCH_WEB
if "%choice%"=="2" goto LAUNCH_USB
if "%choice%"=="3" goto QUICK_INSTALL
if "%choice%"=="4" goto RUN_TESTS
if "%choice%"=="5" goto BUILD_APK
if "%choice%"=="6" goto CLEAN_PROJECT
if "%choice%"=="7" goto OPEN_DOCS
if "%choice%"=="0" exit
goto MAIN_MENU

:LAUNCH_WEB
echo.
echo [*] Starting Smart Exam Security server on http://localhost:8080...
start /b python -u server.py >nul 2>&1
timeout /t 2 >nul
start http://localhost:8080
echo [✓] Web dashboard opened in your default browser (Google Chrome)!
echo.
pause
goto MAIN_MENU

:LAUNCH_USB
call "%~dp0connect_phone.bat"
goto MAIN_MENU

:QUICK_INSTALL
echo.
set ADB="%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if not exist %ADB% set ADB="C:\Users\suryx\AppData\Local\Android\Sdk\platform-tools\adb.exe"
if not exist %ADB% set ADB=adb
echo [*] Checking connected USB devices...
%ADB% devices -l
echo.
echo [*] Pushing SmartExamSecurity-debug.apk to phone...
%ADB% install -r "%~dp0SmartExamSecurity-debug.apk"
if %errorlevel% equ 0 (
    echo [✓] SUCCESS: App installed! Launching on phone now...
    %ADB% shell am start -n com.smartexambox.security/.MainActivity
) else (
    echo [X] Installation failed. Ensure phone has USB Debugging enabled.
)
pause
goto MAIN_MENU

:RUN_TESTS
echo.
echo [*] Executing all security and unit tests with Gradle...
call .\gradlew.bat testDebugUnitTest
pause
goto MAIN_MENU

:BUILD_APK
echo.
echo [*] Compiling Android APK from source...
call .\gradlew.bat assembleDebug
pause
goto MAIN_MENU

:CLEAN_PROJECT
call "%~dp0clean_project.bat"
goto MAIN_MENU

:OPEN_DOCS
start "" "%~dp0README.md"
goto MAIN_MENU
