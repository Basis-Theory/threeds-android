#!/bin/bash
set -e -x
TMP_FILE=_fail_proccess
# Start video recording
# $ANDROID_HOME/platform-tools/adb shell screenrecord /sdcard/video_record.mp4 & echo $! > video_record.pid
# sleep 3
APP_ID=com.basistheory.threeds.example

npx envinfo
adb devices
./gradlew build -x test
# sync clock
adb shell su root date -u @$(date +%s)
# install apk
adb install example/build/outputs/apk/debug/example-debug.apk


echo "===== Run E2E Attempt:  1 ===="

if ./gradlew maestroTest; then
    echo "===== Run E2E Step Succeeded ===="
else
    echo "===== Run E2E Step Failed ===="
    touch "$TMP_FILE"
    # Save logs before exiting
    adb logcat -d > emulator_logcat.txt
    exit 1  # Exit after saving logs
fi

# Save logs to upload as artifact
adb logcat -d > emulator_logcat.txt
# Stop video recording
# kill -SIGINT "$(cat video_record.pid)"
# sleep 3
# rm -rf video_record.pid
# Grab screenshots
# $ANDROID_HOME/platform-tools/adb shell screencap -p /sdcard/last_img.png
# $ANDROID_HOME/platform-tools/adb pull /sdcard/last_img.png
# $ANDROID_HOME/platform-tools/adb pull /sdcard/video_record.mp4
# $ANDROID_HOME/platform-tools/adb shell rm /sdcard/video_record.mp4
# Debug
# echo "::group::Maestro hierarchy"
#     ^^^^^^^^ see: https://docs.github.com/en/actions/using-workflows/workflow-commands-for-github-actions#grouping-log-lines
# $HOME/.maestro/bin/maestro hierarchy
# echo "::endgroup::"