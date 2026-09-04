@echo off
setlocal
cd /d "%~dp0"
java -jar vitrine7-printer-agent.jar printer-agent.properties
pause
