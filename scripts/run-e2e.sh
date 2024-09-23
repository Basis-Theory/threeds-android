#!/bin/bash
set -x

npx envinfo
./gradlew build -x test
# sync clock
adb shell su root date -u @$(date +%s)
# install apk
adb install example/build/outputs/apk/debug/example-debug.apk

./gradlew maestroTest

# Save logs to upload as artifact
adb logcat -d > emulator_logcat.txt
