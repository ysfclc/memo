#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

echo "== Memo Android build =="
echo "Commit: ${VERCEL_GIT_COMMIT_SHA:-unknown}"
echo "Branch: ${VERCEL_GIT_COMMIT_REF:-unknown}"
echo "Build host: $(uname -a)"

export DEBIAN_FRONTEND=noninteractive
if ! command -v java >/dev/null 2>&1 || ! java -version 2>&1 | grep -q '17\\.'; then
  if command -v apt-get >/dev/null 2>&1 && [ "$(id -u)" -eq 0 ]; then
    apt-get update
    apt-get install -y --no-install-recommends openjdk-17-jdk-headless curl unzip ca-certificates
    rm -rf /var/lib/apt/lists/*
  else
    echo "Java 17 is required and could not be installed in this build image." >&2
    exit 1
  fi
fi

if ! command -v curl >/dev/null 2>&1 || ! command -v unzip >/dev/null 2>&1; then
  if command -v apt-get >/dev/null 2>&1 && [ "$(id -u)" -eq 0 ]; then
    apt-get update
    apt-get install -y --no-install-recommends curl unzip ca-certificates
    rm -rf /var/lib/apt/lists/*
  else
    echo "curl and unzip are required." >&2
    exit 1
  fi
fi

export JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"
export PATH="$JAVA_HOME/bin:$PATH"
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

yes | sdkmanager --sdk_root="$ANDROID_SDK_ROOT" --licenses >/dev/null
sdkmanager --sdk_root="$ANDROID_SDK_ROOT" \
  "platform-tools" \
  "platforms;android-35" \
  "build-tools;35.0.0"

chmod +x ./gradlew
./gradlew --no-daemon --stacktrace :core:test :app:assembleDebug

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
Java: $(java -version 2>&1 | head -n 1)
EOF

echo "Build artifact:"
ls -lh artifacts
