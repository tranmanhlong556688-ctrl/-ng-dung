#!/usr/bin/env bash
set -euxo pipefail

mkdir -p emulator-evidence
exec > >(tee emulator-evidence/test-console.log) 2>&1

APK='apk-output/AutoTouchMinh-v1.3.0-Redmi10-MIUI14-Lite.apk'
PACKAGE='com.minh.autotouch.redmilite'
ACTIVITY="$PACKAGE/com.minh.autotouch.redmilite.MainActivity"
AVD_NAME='redmi_lite_api33'
IMAGE='system-images;android-33;google_apis;x86_64'

export ANDROID_AVD_HOME="$GITHUB_WORKSPACE/.android/avd"
mkdir -p "$ANDROID_AVD_HOME"

cleanup() {
  adb emu kill || true
  if [ -n "${EMU_PID:-}" ]; then kill "$EMU_PID" || true; fi
}
trap cleanup EXIT

uname -a > emulator-evidence/runner-uname.txt
ls -l /dev/kvm > emulator-evidence/kvm-device.txt 2>&1 || true
yes | sdkmanager --licenses > emulator-evidence/sdk-licenses.txt 2>&1 || true
sdkmanager --install platform-tools emulator "$IMAGE" > emulator-evidence/sdk-install.txt 2>&1
echo no | avdmanager create avd --force --name "$AVD_NAME" --package "$IMAGE" --device pixel_2 > emulator-evidence/avd-create.txt 2>&1
"$ANDROID_HOME/emulator/emulator" -list-avds > emulator-evidence/avd-list.txt 2>&1
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
  if ! kill -0 "$EMU_PID" 2>/dev/null; then break; fi
  sleep 5
done
if [ "$BOOTED" -ne 1 ]; then
  tail -n 300 emulator-evidence/emulator.log > emulator-evidence/emulator-tail.txt || true
  exit 31
fi

adb shell getprop ro.build.version.release > emulator-evidence/android-version.txt
adb shell getprop ro.build.version.sdk > emulator-evidence/android-api.txt
adb shell getprop ro.product.cpu.abi > emulator-evidence/android-abi.txt

echo 'ANDROID_13_API_33_BOOT_OK' > emulator-evidence/result.txt
adb install "$APK" | tee emulator-evidence/install.txt
grep -q 'Success' emulator-evidence/install.txt
echo 'INSTALL_OK' >> emulator-evidence/result.txt

adb shell dumpsys package "$PACKAGE" > emulator-evidence/package.txt
grep -q 'versionName=1.3.0-redmi-lite' emulator-evidence/package.txt
grep -q 'targetSdk=32' emulator-evidence/package.txt
echo 'PACKAGE_METADATA_OK' >> emulator-evidence/result.txt

adb logcat -c
adb shell am start -W -n "$ACTIVITY" | tee emulator-evidence/launch.txt
grep -q 'Status: ok' emulator-evidence/launch.txt
sleep 10
adb shell pidof "$PACKAGE" | tee emulator-evidence/pid.txt
test -s emulator-evidence/pid.txt
adb exec-out screencap -p > emulator-evidence/screenshot.png
adb logcat -d -v threadtime > emulator-evidence/logcat.txt
! grep -q 'FATAL EXCEPTION' emulator-evidence/logcat.txt
echo 'LAUNCH_10S_NO_FATAL_OK' >> emulator-evidence/result.txt

adb shell am force-stop "$PACKAGE"
adb shell am start -W -n "$ACTIVITY" | tee emulator-evidence/relaunch.txt
grep -q 'Status: ok' emulator-evidence/relaunch.txt
sleep 3
adb shell pidof "$PACKAGE" | tee emulator-evidence/pid-relaunch.txt
test -s emulator-evidence/pid-relaunch.txt
echo 'RELAUNCH_OK' >> emulator-evidence/result.txt

adb uninstall "$PACKAGE" | tee emulator-evidence/uninstall.txt
grep -q 'Success' emulator-evidence/uninstall.txt
adb install "$APK" | tee emulator-evidence/reinstall.txt
grep -q 'Success' emulator-evidence/reinstall.txt
adb shell pm path "$PACKAGE" | tee emulator-evidence/package-path.txt
grep -q 'package:' emulator-evidence/package-path.txt
echo 'UNINSTALL_REINSTALL_OK' >> emulator-evidence/result.txt
