#!/usr/bin/env sh
# Compiles the app into build/inventory.jar. Needs Java 17 or newer.
set -e
cd "$(dirname "$0")"
rm -rf build/classes && mkdir -p build/classes
javac -d build/classes $(find src/main/java -name "*.java")
jar --create --file build/inventory.jar --main-class com.benedict.inventory.Main -C build/classes .
echo "Built build/inventory.jar"
