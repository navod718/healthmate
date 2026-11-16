#!/usr/bin/env bash
set -euo pipefail
mkdir -p release-evidence
adb install releases/HealthMate-0.1.0-release.apk
adb logcat -c
for attempt in 1 2; do
  adb shell am force-stop com.navod718.healthmate
  adb shell am start -W -n com.navod718.healthmate/.MainActivity | tee "release-evidence/launch-${attempt}.txt"
  sleep 4
  adb shell pidof com.navod718.healthmate | tee "release-evidence/process-${attempt}.txt"
  adb shell uiautomator dump /sdcard/release-window.xml
  adb pull /sdcard/release-window.xml "release-evidence/window-${attempt}.xml"
  python3 - "$attempt" <<'PY'
import sys, xml.etree.ElementTree as ET
from pathlib import Path
attempt = sys.argv[1]
launch = Path(f'release-evidence/launch-{attempt}.txt').read_text()
assert 'Status: ok' in launch, launch
root = ET.parse(f'release-evidence/window-{attempt}.xml').getroot()
texts = ' '.join(n.get('text', '') for n in root.iter('node'))
assert 'HealthMate' in texts and 'Steps walked' in texts and 'Add record' in texts, texts
print('Release dashboard visible on launch', attempt)
PY
  adb exec-out screencap -p > "release-evidence/launch-${attempt}.png"
done
adb logcat -d -b crash > release-evidence/crash-log.txt
if grep -q 'FATAL EXCEPTION' release-evidence/crash-log.txt; then
  cat release-evidence/crash-log.txt
  exit 1
fi
