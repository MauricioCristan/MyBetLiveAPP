@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\Iniciar-MyBetLive.ps1" %*
if errorlevel 1 pause
