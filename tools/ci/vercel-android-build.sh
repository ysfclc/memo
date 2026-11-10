#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

echo "== Memo Android build =="
echo "Commit: ${VERCEL_GIT_COMMIT_SHA:-unknown}"
echo "Branch: ${VERCEL_GIT_COMMIT_REF:-unknown}"
echo "Build host: $(uname -a)"

if ! command -v curl >/dev/null 2>&1 || ! command -v tar >/dev/null 2>&1 || ! command -v unzip >/dev/null 2>&1; then
  echo "This Vercel build image must provide curl, tar, and unzip." >&2
  exit 1
fi

if ! command -v java >/dev/null 2>&1 || ! java -version 2>&1 | grep -q '17\\.'; then
  TOOLCHAIN_DIR="$HOME/.memo-toolchains"
  JAVA_HOME="$TOOLCHAIN_DIR/jdk-17"
  mkdir -p "$TOOLCHAIN_DIR"
  TMP_JDK="$(mktemp -d)"
  trap 'rm -rf "$TMP_JDK"' EXIT
  echo "Downloading a user-local Temurin JDK 17 (no root/apt required)..."
  curl --fail --location --retry 3 \
    https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse \
    --output "$TMP_JDK/jdk17.tar.gz"
  mkdir -p "$JAVA_HOME"
  tar -xzf "$TMP_JDK/jdk17.tar.gz" -C "$TMP_JDK"
  JDK_EXTRACTED=""
  for candidate in "$TMP_JDK"/jdk-*; do
    if [ -d "$candidate" ]; then
      JDK_EXTRACTED="$candidate"
      break
    fi
  done
  if [ -z "$JDK_EXTRACTED" ]; then
    echo "Could not locate extracted JDK 17 directory." >&2
    exit 1
  fi
  rm -rf "$JAVA_HOME"
  mv "$JDK_EXTRACTED" "$JAVA_HOME"
  rm -rf "$TMP_JDK"
  trap - EXIT
  export JAVA_HOME
  export PATH="$JAVA_HOME/bin:$PATH"
else
  export JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

java -version

export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"

if [ ! -x "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
  mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
  TMP_DIR="$(mktemp -d)"
  trap 'rm -rf "$TMP_DIR"' EXIT
  curl --fail --location --retry 3 --output "$TMP_DIR/commandlinetools.zip" \
    https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -q "$TMP_DIR/commandlinetools.zip" -d "$TMP_DIR"
  rm -rf "$ANDROID_SDK_ROOT/cmdline-tools/latest"
  mv "$TMP_DIR/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"
fi

set +o pipefail
yes | sdkmanager --sdk_root="$ANDROID_SDK_ROOT" --licenses >/dev/null
LICENSE_STATUS="${PIPESTATUS[1]}"
set -o pipefail
if [ "$LICENSE_STATUS" -ne 0 ]; then
  echo "Android SDK license acceptance failed with status $LICENSE_STATUS." >&2
  exit "$LICENSE_STATUS"
fi
sdkmanager --sdk_root="$ANDROID_SDK_ROOT" \
  "platform-tools" \
  "platforms;android-35" \
  "build-tools;35.0.0"

chmod +x ./gradlew
GRADLE_LOG="$(mktemp)"
if ! ./gradlew --no-daemon --console=plain --stacktrace --info :core:test :app:assembleDebug >"$GRADLE_LOG" 2>&1; then
  echo "Gradle failed. Kotlin/compiler diagnostics (up to 160 matching lines):"
  grep -nE '(^e: |^w: |error:|Errors occurred while compiling|Compilation error|Could not resolve|Could not find|What went wrong|FAILURE:)' "$GRADLE_LOG" | tail -n 160 || true
  echo "Gradle failure context (last 100 lines):"
  tail -n 100 "$GRADLE_LOG"
  rm -f "$GRADLE_LOG"
  exit 1
fi
rm -f "$GRADLE_LOG"

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -s "$APK_PATH" ]; then
  echo "Expected APK not found: $APK_PATH" >&2
  exit 1
fi

mkdir -p artifacts
COMMIT="${VERCEL_GIT_COMMIT_SHA:-local}"
SHORT_COMMIT="${COMMIT:0:12}"
cp "$APK_PATH" "artifacts/Memo-debug-$SHORT_COMMIT.apk"
sha256sum "artifacts/Memo-debug-$SHORT_COMMIT.apk" > "artifacts/Memo-debug-$SHORT_COMMIT.apk.sha256"
cat > artifacts/BUILD_INFO.txt <<EOF
Product: Memo Android
Variant: debug
Commit: $COMMIT
Branch: ${VERCEL_GIT_COMMIT_REF:-unknown}
Gradle tasks: :core:test :app:assembleDebug
Java: $(java -version 2>&1 | sed -n '1p')
EOF

echo "Build artifact:"
ls -lh artifacts
