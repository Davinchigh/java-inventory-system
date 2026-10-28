@echo off
rem Compiles the app into build\inventory.jar. Needs Java 17 or newer.
cd /d "%~dp0"
if exist build\classes rmdir /s /q build\classes
mkdir build\classes
dir /s /b src\main\java\*.java > build\sources.txt
javac -d build\classes @build\sources.txt || exit /b 1
jar --create --file build\inventory.jar --main-class com.benedict.inventory.Main -C build\classes .
echo Built build\inventory.jar
