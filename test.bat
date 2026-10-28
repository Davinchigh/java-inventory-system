@echo off
rem Compiles the app and the tests, then runs the tests.
cd /d "%~dp0"
if exist build\test-classes rmdir /s /q build\test-classes
mkdir build\test-classes
dir /s /b src\main\java\*.java src\test\java\*.java > build\test-sources.txt
javac -d build\test-classes @build\test-sources.txt || exit /b 1
java -cp build\test-classes com.benedict.inventory.InventoryTests
