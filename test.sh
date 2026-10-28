#!/usr/bin/env sh
# Compiles the app and the tests, then runs the tests.
set -e
cd "$(dirname "$0")"
rm -rf build/test-classes && mkdir -p build/test-classes
javac -d build/test-classes $(find src/main/java src/test/java -name "*.java")
java -cp build/test-classes com.benedict.inventory.InventoryTests
