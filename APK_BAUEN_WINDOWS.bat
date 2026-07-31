@echo off
cd /d "%~dp0"
echo.
echo Windi-Chronik APK wird gebaut...
echo.
call gradlew.bat assembleDebug
if errorlevel 1 (
  echo.
  echo Build fehlgeschlagen. Projekt bitte in Android Studio oeffnen.
  pause
  exit /b 1
)
echo.
echo Fertig:
echo app\build\outputs\apk\debug\app-debug.apk
pause
