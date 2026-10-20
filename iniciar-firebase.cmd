@echo off
cd /d "%~dp0"
if not exist "node_modules\.bin\firebase.cmd" call npm ci
if errorlevel 1 goto error
call npm run firebase:local
if errorlevel 1 goto error
exit /b 0
:error
echo Revisa que Node.js 20 o posterior este instalado.
pause
exit /b 1
