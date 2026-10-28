@echo off
rem Builds (if needed) and opens the inventory app.
cd /d "%~dp0"
if not exist build\inventory.jar call build.bat
java -jar build\inventory.jar %*
