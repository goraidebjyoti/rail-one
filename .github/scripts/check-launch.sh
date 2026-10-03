#!/usr/bin/env bash
set -euo pipefail

# The emulator action runs script lines separately. Keep shell control flow here.
capture_diagnostics() {
  adb logcat -d > logcat.txt || true
  adb exec-out screencap -p > launch.png || true
}
trap capture_diagnostics EXIT

adb wait-for-device
adb install -r apk/app-debug.apk
adb logcat -c
adb shell am start -W -n com.example.railone/.MainActivity | tee launch-output.txt
if ! grep -q '^Status: ok' launch-output.txt; then
  echo 'Rail One did not launch successfully.' >&2
  exit 1
fi

sleep 8
adb logcat -d > logcat.txt
if ! adb shell pidof com.example.railone > /dev/null; then
  echo 'Rail One stopped after launch.' >&2
  cat logcat.txt
  exit 1
fi
if grep -q 'Process: com.example.railone' logcat.txt; then
  echo 'Rail One crash detected in logcat.' >&2
  cat logcat.txt
  exit 1
fi
echo 'Rail One is running after launch.'
