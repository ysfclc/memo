#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"
mkdir -p artifacts
LOG="$ROOT/artifacts/Memo-CircleCI-Build.log"
exec > >(tee -a "$LOG") 2>&1

fail() { echo "ERROR: $*" >&2; exit 1; }

echo "Memo CircleCI build"
echo "Branch: ${CIRCLE_BRANCH:-unknown}"
echo "Commit: ${CIRCLE_SHA1:-unknown}"

export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
test -n "$ANDROID_HOME" && test -d "$ANDROID_HOME" || fail "Android SDK not found"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/build-tools/35.0.0:$PATH"

# Recreate the source fingerprint for this exact checkout; do not mutate or push source.
find . -type f \
  ! -path './.git/*' ! -path './artifacts/*' ! -path '*/.gradle/*' ! -path '*/build/*' \
  ! -path '*/__pycache__/*' ! -path '*/node_modules/*' ! -path '*/.idea/*' \
  ! -path '*/.kotlin/*' ! -path '*/.cxx/*' ! -path '*/.externalNativeBuild/*' \
  ! -path '*/captures/*' ! -path '*/.venv/*' ! -path '*/venv/*' \
  ! -name '*.zip' ! -name 'SOURCE_SHA256SUMS.txt' ! -name 'local.properties' \
  ! -name '.env' ! -name '.env.*' ! -name '*.jks' ! -name '*.keystore' \
  ! -name '*.p12' ! -name '*.pfx' ! -name '*.pem' ! -name '*.key' \
  ! -name '*signing*.properties' ! -name '*credentials*.json' \
  ! -name '*secret*.json' ! -name '*token*.json' ! -name 'service-account*.json' \
  -print0 | sort -z | xargs -0 -r sha256sum > SOURCE_SHA256SUMS.txt
sha256sum -c SOURCE_SHA256SUMS.txt

python3 tools/verify_source.py
(cd remote-runner && python3 -m unittest discover -s tests -v)

# Persistent signing is mandatory: never fall back to a runner-generated debug key.
values=("${MEMO_UPLOAD_KEYSTORE_BASE64:-}" "${MEMO_UPLOAD_STORE_PASSWORD:-}" "${MEMO_UPLOAD_KEY_ALIAS:-}" "${MEMO_UPLOAD_KEY_PASSWORD:-}")
for value in "${values[@]}"; do
  [[ -n "$value" ]] || fail "CircleCI project environment is missing one or more Memo signing variables (MEMO_UPLOAD_KEYSTORE_BASE64, MEMO_UPLOAD_STORE_PASSWORD, MEMO_UPLOAD_KEY_ALIAS, MEMO_UPLOAD_KEY_PASSWORD)."
done
KEYSTORE="$HOME/memo-upload-signing.jks"
printf '%s' "$MEMO_UPLOAD_KEYSTORE_BASE64" | base64 --decode > "$KEYSTORE"
test -s "$KEYSTORE" || fail "Decoded Memo keystore is empty"
keytool -list -keystore "$KEYSTORE" -storepass "$MEMO_UPLOAD_STORE_PASSWORD" -alias "$MEMO_UPLOAD_KEY_ALIAS" >/dev/null
export MEMO_UPLOAD_KEYSTORE_FILE="$KEYSTORE"
VERSION_TIMES=($(git log -2 --format=%ct HEAD))
[[ "${#VERSION_TIMES[@]}" -eq 2 ]] || fail "Memo versioning requires the current commit and its parent."
for version_time in "${VERSION_TIMES[@]}"; do
  [[ "$version_time" =~ ^[1-9][0-9]{0,9}$ ]] || fail "Invalid Memo source commit timestamp: $version_time"
done
CURRENT_VERSION_CODE=$(( VERSION_TIMES[0] / 4 ))
PARENT_VERSION_CODE=$(( VERSION_TIMES[1] / 4 ))
(( CURRENT_VERSION_CODE > PARENT_VERSION_CODE && CURRENT_VERSION_CODE <= 2100000000 )) || fail "Memo source versionCode must increase and remain within Android versionCode limits."
export MEMO_VERSION_CODE="$CURRENT_VERSION_CODE"
export MEMO_UPLOAD_STORE_PASSWORD MEMO_UPLOAD_KEY_ALIAS MEMO_UPLOAD_KEY_PASSWORD

./gradlew --no-daemon --stacktrace \
  :core:test \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest

APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
INSTRUMENTATION_APK="$ROOT/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
AAPT="$ANDROID_HOME/build-tools/35.0.0/aapt"
APKSIGNER="$ANDROID_HOME/build-tools/35.0.0/apksigner"
test -s "$APK" && test -s "$INSTRUMENTATION_APK" || fail "APK or instrumentation APK missing"
test -x "$AAPT" && test -x "$APKSIGNER" || fail "Android APK tools missing"

PACKAGE_LINE="$("$AAPT" dump badging "$APK" | grep -m1 '^package:')"
[[ "$PACKAGE_LINE" == *"name='app.memo.android.debug'"* ]] || fail "Unexpected application ID: $PACKAGE_LINE"
[[ "$PACKAGE_LINE" == *"versionCode='$MEMO_VERSION_CODE'"* ]] || fail "APK versionCode does not match build number"
[[ "$PACKAGE_LINE" == *"versionName='4.0.0-memo-debug'"* ]] || fail "APK versionName does not match source"

SIGNATURE="$("$APKSIGNER" verify --verbose --print-certs "$APK" 2>&1)"
printf '%s\n' "$SIGNATURE"
CERT="$(printf '%s\n' "$SIGNATURE" | sed -nE 's/.*certificate SHA-256 digest: ([0-9a-fA-F]+).*/\1/p' | head -n1 | tr '[:upper:]' '[:lower:]')"
EXPECTED_MEMO_CERT_SHA256='a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a'
[[ -n "$CERT" && "$CERT" == "$EXPECTED_MEMO_CERT_SHA256" ]] || fail "Memo signing certificate mismatch. Refusing to publish an APK that may require uninstalling the installed app."
{
  echo "Package: app.memo.android.debug"
  echo "Version code: $MEMO_VERSION_CODE"
  echo "Version name: 4.0.0-memo-debug"
  echo "Certificate SHA-256: $CERT"
  echo "Signing mode: pinned persistent Memo upload keystore"
} | tee artifacts/Memo-APK-Identity.txt

# Build a lower-version baseline with the same key, then prove upgrade on a real emulator.
CURRENT_APK="$ROOT/artifacts/Memo-current.apk"
BASELINE_APK="$ROOT/artifacts/Memo-baseline.apk"
cp "$APK" "$CURRENT_APK"
BASELINE_CODE="$((MEMO_VERSION_CODE - 1))"
MEMO_VERSION_CODE="$BASELINE_CODE" ./gradlew --no-daemon :app:assembleDebug
cp "$APK" "$BASELINE_APK"
MEMO_VERSION_CODE="$((BASELINE_CODE + 1))" ./gradlew --no-daemon :app:assembleDebug
cp "$CURRENT_APK" "$APK"

AVDMANAGER="$(find "$ANDROID_HOME" -type f -name avdmanager -print -quit)"
EMULATOR="$ANDROID_HOME/emulator/emulator"
test -x "$EMULATOR" && test -n "$AVDMANAGER" || fail "Android emulator tools missing"
export ANDROID_USER_HOME="$HOME/.android"
export ANDROID_AVD_HOME="$ANDROID_USER_HOME/avd"
export ANDROID_SDK_HOME="$HOME"
mkdir -p "$ANDROID_AVD_HOME"
printf 'no\n' | "$AVDMANAGER" create avd --force --name memo-api35 --package 'system-images;android-35;google_apis;x86_64' --device pixel_2
"$EMULATOR" -list-avds
[[ -e /dev/kvm ]] || fail "CircleCI Android machine does not expose /dev/kvm"
sudo chmod 666 /dev/kvm
[[ -r /dev/kvm && -w /dev/kvm ]] || fail "Cannot access /dev/kvm"

"$EMULATOR" -avd memo-api35 -accel on -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot -no-metrics > artifacts/Memo-emulator.log 2>&1 &
EMULATOR_PID=$!
cleanup() {
  adb emu kill >/dev/null 2>&1 || true
  kill "$EMULATOR_PID" >/dev/null 2>&1 || true
}
diagnose() {
  adb devices -l || true
  tail -n 160 artifacts/Memo-emulator.log || true
}
trap 'status=$?; if (( status != 0 )); then diagnose; fi; cleanup; exit "$status"' EXIT

timeout 120s adb wait-for-device
BOOTED=''
for attempt in $(seq 1 60); do
  BOOTED="$(timeout 8s adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  [[ "$BOOTED" == '1' ]] && break
  sleep 3
done
[[ "$BOOTED" == '1' ]] || fail "Android emulator did not finish booting"
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

TEST_RUNNER='app.memo.android.debug.test/androidx.test.runner.AndroidJUnitRunner'
run_store_phase() {
  local phase="$1" output="artifacts/Memo-store-$1.txt"
  adb shell am instrument -w -e phase "$phase" "$TEST_RUNNER" 2>&1 | tee "$output"
  grep -Fq 'OK (1 test)' "$output" || fail "Encrypted-store instrumentation phase '$phase' failed"
}
adb install "$BASELINE_APK"
adb install "$INSTRUMENTATION_APK"
run_store_phase seed
adb shell "run-as app.memo.android.debug sh -c 'mkdir -p files/.memo-upgrade-test && printf \"%s\" \"memo-private-data-survives-update\" > files/.memo-upgrade-test/sentinel'"
adb install -r "$CURRENT_APK"
run_store_phase verify
PRESERVED="$(adb shell run-as app.memo.android.debug cat files/.memo-upgrade-test/sentinel | tr -d '\r')"
[[ "$PRESERVED" == 'memo-private-data-survives-update' ]] || fail "App-private data did not survive in-place upgrade"

cp "$CURRENT_APK" artifacts/Memo-Android-debug.apk
rm -f artifacts/Memo-current.apk artifacts/Memo-baseline.apk
(
  cd artifacts
  sha256sum Memo-Android-debug.apk Memo-APK-Identity.txt > SHA256SUMS.txt
)
echo "PASS: source checks, remote-runner tests, Gradle unit tests, lint, APK identity/signature, encrypted-store upgrade and private-data preservation."
