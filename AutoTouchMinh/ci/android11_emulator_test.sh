#!/usr/bin/env bash
# Android 11 API 30 release-install regression test for Auto Touch Minh v1.2.1.
set -euxo pipefail

mkdir -p emulator-evidence
exec > >(tee emulator-evidence/test-console.log) 2>&1

BASELINE='apk-output/AutoTouchMinh-v1.2.0-baseline-release.apk'
FINAL='apk-output/AutoTouchMinh-v1.2.1-Xiaomi-Redmi10-release.apk'
INSTALL_TEST='apk-output/AutoTouchMinh-Installation-Test-v1.0.0.apk'
PACKAGE='com.minh.autotouch.personal'
ACTIVITY="$PACKAGE/com.minh.autotouch.MainActivity"
TEST_PACKAGE='com.minh.autotouch.installtest'
TEST_ACTIVITY="$TEST_PACKAGE/com.minh.autotouch.installtest.MainActivity"
RESULT='emulator-evidence/result.txt'
AVD_NAME='autotouch_api30'
IMAGE='system-images;android-30;google_apis;x86_64'

export ANDROID_AVD_HOME="$GITHUB_WORKSPACE/.android/avd"
mkdir -p "$ANDROID_AVD_HOME"

echo "ANDROID_AVD_HOME=$ANDROID_AVD_HOME" > emulator-evidence/avd-home.txt

cleanup() {
  adb emu kill || true
  if [ -n "${EMU_PID:-}" ]; then kill "$EMU_PID" || true; fi
}
trap cleanup EXIT

uname -a > emulator-evidence/runner-uname.txt
ls -l /dev/kvm > emulator-evidence/kvm-device.txt 2>&1 || true
"$ANDROID_HOME/emulator/emulator" -accel-check > emulator-evidence/accel-check.txt 2>&1 || true
yes | sdkmanager --licenses > emulator-evidence/sdk-licenses.txt 2>&1 || true
sdkmanager --install platform-tools emulator "$IMAGE" > emulator-evidence/sdk-install.txt 2>&1
echo no | avdmanager create avd --force --name "$AVD_NAME" --package "$IMAGE" --device pixel_2 > emulator-evidence/avd-create.txt 2>&1
"$ANDROID_HOME/emulator/emulator" -list-avds > emulator-evidence/avd-list.txt 2>&1
find "$ANDROID_AVD_HOME" -maxdepth 2 -type f -print > emulator-evidence/avd-files.txt 2>&1 || true
grep -qx "$AVD_NAME" emulator-evidence/avd-list.txt

ACCEL='-accel off'
if [ -c /dev/kvm ] && [ -r /dev/kvm ] && [ -w /dev/kvm ]; then ACCEL='-accel on'; fi
echo "$ACCEL" > emulator-evidence/acceleration-selected.txt

nohup "$ANDROID_HOME/emulator/emulator" -avd "$AVD_NAME" \
  $ACCEL -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim \
  -no-snapshot -wipe-data -camera-back none -camera-front none \
  > emulator-evidence/emulator.log 2>&1 &
EMU_PID=$!
echo "$EMU_PID" > emulator-evidence/emulator-pid.txt

adb start-server
BOOTED=0
for i in $(seq 1 180); do
  adb devices -l > emulator-evidence/adb-devices-latest.txt 2>&1 || true
  if adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' | grep -q '^1$'; then
    BOOTED=1
    break
  fi
  if ! kill -0 "$EMU_PID" 2>/dev/null; then
    echo 'Emulator process exited before boot' >&2
    break
  fi
  sleep 5
done
if [ "$BOOTED" -ne 1 ]; then
  tail -n 300 emulator-evidence/emulator.log > emulator-evidence/emulator-tail.txt || true
  exit 31
fi

echo 'ANDROID_11_API_30_BOOT_OK' > "$RESULT"
adb shell getprop ro.build.version.release > emulator-evidence/android-version.txt
adb shell getprop ro.build.version.sdk > emulator-evidence/android-api.txt
adb shell getprop ro.product.cpu.abi > emulator-evidence/android-abi.txt

adb install "$INSTALL_TEST" | tee emulator-evidence/install-installation-test.txt
grep -q 'Success' emulator-evidence/install-installation-test.txt
adb logcat -c
adb shell am start -W -n "$TEST_ACTIVITY" | tee emulator-evidence/launch-installation-test.txt
grep -q 'Status: ok' emulator-evidence/launch-installation-test.txt
sleep 10
adb shell pidof "$TEST_PACKAGE" | tee emulator-evidence/pid-installation-test.txt
test -s emulator-evidence/pid-installation-test.txt
adb exec-out screencap -p > emulator-evidence/screenshot-installation-test.png
adb logcat -d -v threadtime > emulator-evidence/logcat-installation-test.txt
! grep -q 'FATAL EXCEPTION' emulator-evidence/logcat-installation-test.txt
adb uninstall "$TEST_PACKAGE" | tee emulator-evidence/uninstall-installation-test.txt
grep -q 'Success' emulator-evidence/uninstall-installation-test.txt
echo 'INSTALLATION_TEST_OK' >> "$RESULT"

adb install "$BASELINE" | tee emulator-evidence/install-baseline.txt
grep -q 'Success' emulator-evidence/install-baseline.txt
adb shell dumpsys package "$PACKAGE" > emulator-evidence/package-baseline.txt
grep -q 'versionName=1.2.0' emulator-evidence/package-baseline.txt
echo 'BASELINE_INSTALL_OK' >> "$RESULT"

adb install -r "$FINAL" | tee emulator-evidence/update-to-final.txt
grep -q 'Success' emulator-evidence/update-to-final.txt
adb shell dumpsys package "$PACKAGE" > emulator-evidence/package-final.txt
grep -q 'versionName=1.2.1' emulator-evidence/package-final.txt
echo 'SAME_SIGNATURE_UPDATE_OK' >> "$RESULT"

adb logcat -c
adb shell am start -W -n "$ACTIVITY" | tee emulator-evidence/launch-final.txt
grep -q 'Status: ok' emulator-evidence/launch-final.txt
sleep 10
adb shell pidof "$PACKAGE" | tee emulator-evidence/pid-final.txt
test -s emulator-evidence/pid-final.txt
adb exec-out screencap -p > emulator-evidence/screenshot-final.png
adb logcat -d -v threadtime > emulator-evidence/logcat-final.txt
! grep -q 'FATAL EXCEPTION' emulator-evidence/logcat-final.txt
echo 'FINAL_LAUNCH_10S_OK' >> "$RESULT"

adb shell am force-stop "$PACKAGE"
adb shell am start -W -n "$ACTIVITY" | tee emulator-evidence/relaunch-final.txt
grep -q 'Status: ok' emulator-evidence/relaunch-final.txt
sleep 3
adb shell pidof "$PACKAGE" | tee emulator-evidence/pid-relaunch.txt
test -s emulator-evidence/pid-relaunch.txt
echo 'FINAL_RELAUNCH_OK' >> "$RESULT"

adb uninstall "$PACKAGE" | tee emulator-evidence/uninstall-final.txt
grep -q 'Success' emulator-evidence/uninstall-final.txt
adb install "$FINAL" | tee emulator-evidence/reinstall-final.txt
grep -q 'Success' emulator-evidence/reinstall-final.txt
adb shell pm path "$PACKAGE" | tee emulator-evidence/package-path-final.txt
grep -q 'package:' emulator-evidence/package-path-final.txt
echo 'FINAL_UNINSTALL_REINSTALL_OK' >> "$RESULT"
