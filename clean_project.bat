@echo off
title Smart Exam Paper Security - Clean Project for Sharing
color 0e
cd /d "%~dp0"

echo ===============================================================================
echo     CLEAN PROJECT FOR SHARING / ZIP PACKAGING
echo ===============================================================================
echo.
echo This will remove temporary caches (.gradle, build, .kotlin, __pycache__)
echo to make your ZIP file small and easy to send to your friend.
echo.
echo Your source code, tests, documentation, and SmartExamSecurity-debug.apk
echo will NOT be deleted.
echo.
set /p confirm="Do you want to clean temporary build files? [Y/N]: "
if /i not "%confirm%"=="Y" goto END

echo.
echo [*] Cleaning Gradle temporary build files...
if exist ".gradle" (
    echo Deleting .gradle cache...
    rd /s /q ".gradle" >nul 2>&1
)

if exist ".kotlin" (
    echo Deleting .kotlin cache...
    rd /s /q ".kotlin" >nul 2>&1
)

if exist "build" (
    echo Deleting root build folder...
    rd /s /q "build" >nul 2>&1
)

if exist "app\build" (
    echo Deleting app build folder...
    rd /s /q "app\build" >nul 2>&1
)

if exist "__pycache__" (
    echo Deleting __pycache__...
    rd /s /q "__pycache__" >nul 2>&1
)

echo.
echo [✓] CLEANUP COMPLETE!
echo.
echo Your project directory is now clean, lightweight, and ready to be zipped:
echo Right-click the "Exam box" folder ➔ "Compress to ZIP file" ➔ Send to your friend!
echo.

:END
pause
