#!/usr/bin/env bash
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

BUILD_ROOT="${TMPDIR:-/tmp}/memo-vercel-android"
ANDROID_HOME="$BUILD_ROOT/android-sdk"
export ANDROID_HOME ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
export MEMO_VERSION_CODE="${MEMO_VERSION_CODE:-$(date +%s)}"

mkdir -p "$BUILD_ROOT" "$ANDROID_HOME/cmdline-tools" "$GRADLE_USER_HOME"

need() {
  command -v "$1" >/dev/null 2>&1 || { echo "::error::Required build utility missing: $1"; exit 1; }
}
need curl
need unzip
need tar
need python3
need sha256sum
need sha1sum
need readlink

echo "== Memo Vercel Android build =="
echo "Commit: ${VERCEL_GIT_COMMIT_SHA:-unknown}"
echo "Branch: ${VERCEL_GIT_COMMIT_REF:-unknown}"
echo "Version code: $MEMO_VERSION_CODE"

# Pin Eclipse Temurin 17 directly to its official GitHub release asset. The
# Adoptium metadata API returned HTTP 403 from Vercel's build network, so do not
# depend on that API at build time. Verify the published release SHA-256.
if command -v java >/dev/null 2>&1 && java -version 2>&1 | grep -qE 'version "17([.]|")'; then
  JAVA_BIN="$(readlink -f "$(command -v java)")"
  JAVA_HOME="$(dirname "$(dirname "$JAVA_BIN")")"
else
  echo "Installing checksum-verified Eclipse Temurin JDK 17.0.20.1+1..."
  JDK_URL="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.20.1%2B1/OpenJDK17U-jdk_x64_linux_hotspot_17.0.20.1_1.tar.gz"
  JDK_SHA="3808d1d15e3ec6bd5b84057fb5d84c33d8a1536a258146bcea2e603fc726e08e"
  curl --fail --location --retry 3 --retry-delay 2 "$JDK_URL" -o "$BUILD_ROOT/temurin17.tar.gz"
  echo "$JDK_SHA  $BUILD_ROOT/temurin17.tar.gz" | sha256sum -c -
  mkdir -p "$BUILD_ROOT/jdk-extract"
  tar -xzf "$BUILD_ROOT/temurin17.tar.gz" -C "$BUILD_ROOT/jdk-extract"
  JDK_DIR="$(find "$BUILD_ROOT/jdk-extract" -mindepth 1 -maxdepth 1 -type d -print -quit)"
  test -x "$JDK_DIR/bin/java"
  JAVA_HOME="$BUILD_ROOT/jdk"
  rm -rf "$JAVA_HOME"
  mv "$JDK_DIR" "$JAVA_HOME"
fi
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
java -version

# Resolve Google's current Linux command-line tools archive from its official
# SDK manifest and verify the published SHA-1/SHA-256 before extracting it.
if [[ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]]; then
  echo "Installing checksum-verified Android command-line tools..."
  python3 - <<'PY' > "$BUILD_ROOT/sdk-tools-package.txt"
import urllib.request
import xml.etree.ElementTree as ET
url = "https://dl.google.com/android/repository/repository2-1.xml"
request = urllib.request.Request(
    url,
    headers={
        "User-Agent": "Mozilla/5.0 (compatible; MemoAndroidBuild/1.0)",
        "Accept": "application/xml,text/xml,*/*",
    },
)
with urllib.request.urlopen(request, timeout=45) as response:
    root = ET.fromstring(response.read())
local = lambda element: element.tag.rsplit("}", 1)[-1]
for package in root.iter():
    if local(package) != "remotePackage" or package.attrib.get("path") != "cmdline-tools;latest":
        continue
    for archive in package.iter():
        if local(archive) != "archive":
            continue
        values = {local(child): (child.text or "").strip() for child in archive.iter()}
        if values.get("host-os") == "linux" and values.get("url") and values.get("checksum"):
            print(values["url"])
            print(values["checksum"])
            raise SystemExit(0)
raise SystemExit("No Linux cmdline-tools archive found in Google's repository manifest")
PY
  SDK_TOOLS_URL="$(sed -n '1p' "$BUILD_ROOT/sdk-tools-package.txt")"
  SDK_TOOLS_SHA="$(sed -n '2p' "$BUILD_ROOT/sdk-tools-package.txt")"
  test -n "$SDK_TOOLS_URL" && [[ "$SDK_TOOLS_SHA" =~ ^([0-9a-fA-F]{40}|[0-9a-fA-F]{64})$ ]]
  [[ "$SDK_TOOLS_URL" == https://* ]] || SDK_TOOLS_URL="https://dl.google.com/android/repository/$SDK_TOOLS_URL"
  curl --fail --location --retry 3 --retry-delay 2 "$SDK_TOOLS_URL" -o "$BUILD_ROOT/android-cmdline-tools.zip"
  if [[ "${#SDK_TOOLS_SHA}" -eq 40 ]]; then
    echo "$SDK_TOOLS_SHA  $BUILD_ROOT/android-cmdline-tools.zip" | sha1sum -c -
  else
    echo "$SDK_TOOLS_SHA  $BUILD_ROOT/android-cmdline-tools.zip" | sha256sum -c -
  fi
  mkdir -p "$BUILD_ROOT/sdk-tools-extract"
  unzip -q "$BUILD_ROOT/android-cmdline-tools.zip" -d "$BUILD_ROOT/sdk-tools-extract"
  test -d "$BUILD_ROOT/sdk-tools-extract/cmdline-tools"
  rm -rf "$ANDROID_HOME/cmdline-tools/latest"
  mv "$BUILD_ROOT/sdk-tools-extract/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi

SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
test -x "$SDKMANAGER"
set +o pipefail
yes | "$SDKMANAGER" --sdk_root="$ANDROID_HOME" --licenses >/dev/null
set -o pipefail
"$SDKMANAGER" --sdk_root="$ANDROID_HOME" --install \
  "platform-tools" "platforms;android-35" "build-tools;35.0.0"

printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties

echo "== Source and remote-runner checks =="
python3 tools/verify_source.py
(
  cd remote-runner
  PYTHONPATH=. python3 -m unittest discover -s tests -v
)

# Prefer the existing pinned upload certificate when the project owner has
# provisioned all four credentials as protected Vercel environment variables.
# A partial configuration is an error; never silently fall back from a broken key.
SIGNING_VALUES="${MEMO_UPLOAD_KEYSTORE_BASE64:-}|${MEMO_UPLOAD_STORE_PASSWORD:-}|${MEMO_UPLOAD_KEY_ALIAS:-}|${MEMO_UPLOAD_KEY_PASSWORD:-}"
SIGNING_COUNT=0
IFS='|' read -r -a SIGNING_PARTS <<< "$SIGNING_VALUES"
for value in "${SIGNING_PARTS[@]}"; do [[ -n "$value" ]] && SIGNING_COUNT=$((SIGNING_COUNT + 1)); done
if [[ "$SIGNING_COUNT" -ne 0 && "$SIGNING_COUNT" -ne 4 ]]; then
  echo "::error::Memo signing is partially configured. Supply all four protected signing variables or none."
  exit 1
fi
SIGNING_DESCRIPTION="ephemeral Android debug key (not update-compatible with the pinned Memo certificate)"
if [[ "$SIGNING_COUNT" -eq 4 ]]; then
  export MEMO_UPLOAD_KEYSTORE_FILE="$BUILD_ROOT/memo-upload.jks"
  printf '%s' "$MEMO_UPLOAD_KEYSTORE_BASE64" | base64 --decode > "$MEMO_UPLOAD_KEYSTORE_FILE"
  chmod 600 "$MEMO_UPLOAD_KEYSTORE_FILE"
  test -s "$MEMO_UPLOAD_KEYSTORE_FILE"
  SIGNING_DESCRIPTION="pinned Memo upload certificate"
fi

echo "== Gradle tests, lint and APK assembly =="
bash ./gradlew --no-daemon --stacktrace \
  :core:test \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleDebugAndroidTest \
  :app:assembleDebug

APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
test -s "$APK"
BUILD_TOOLS="$ANDROID_HOME/build-tools/35.0.0"
for tool in aapt apksigner zipalign; do test -x "$BUILD_TOOLS/$tool"; done
PACKAGE_LINE="$("$BUILD_TOOLS/aapt" dump badging "$APK" | grep -m1 '^package:')"
[[ "$PACKAGE_LINE" == *"name='app.memo.android.debug'"* ]] || {
  echo "::error::Unexpected Memo APK application ID: $PACKAGE_LINE"
  exit 1
}
"$BUILD_TOOLS/zipalign" -c -p 4 "$APK"
SIGNATURE="$("$BUILD_TOOLS/apksigner" verify --verbose --print-certs "$APK" 2>&1)"
printf '%s\n' "$SIGNATURE"
if [[ "$SIGNING_COUNT" -eq 4 ]]; then
  CERT="$(printf '%s\n' "$SIGNATURE" | sed -nE 's/.*certificate SHA-256 digest: ([0-9a-fA-F]+).*/\1/p' | head -n1 | tr '[:upper:]' '[:lower:]')"
  EXPECTED_MEMO_CERT_SHA256='a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a'
  [[ "$CERT" == "$EXPECTED_MEMO_CERT_SHA256" ]] || {
    echo "::error::Persistent Memo signing certificate mismatch. Refusing to publish this APK."
    exit 1
  }
fi
APK_BYTES="$(wc -c < "$APK" | tr -d ' ')"
if (( APK_BYTES > 95000000 )); then
  echo "::error::APK is ${APK_BYTES} bytes; exceeds the 95 MB Vercel static artifact safety limit."
  exit 1
fi

DIST="$ROOT/dist"
rm -rf "$DIST"
mkdir -p "$DIST"
cp "$APK" "$DIST/Memo-Android-debug.apk"
APK_SHA="$(sha256sum "$DIST/Memo-Android-debug.apk" | awk '{print $1}')"
COMMIT="${VERCEL_GIT_COMMIT_SHA:-$(git rev-parse HEAD)}"
BRANCH="${VERCEL_GIT_COMMIT_REF:-$(git branch --show-current)}"
{
  echo "Commit: $COMMIT"
  echo "Branch: $BRANCH"
  echo "Variant: debug"
  echo "Package: app.memo.android.debug"
  echo "Signing: $SIGNING_DESCRIPTION"
  echo "Gradle: 8.14.6"
  echo "Compile SDK: 35"
  echo "APK bytes: $APK_BYTES"
  echo "APK SHA-256: $APK_SHA"
  echo "Generated UTC: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
} > "$DIST/BUILD-INFO.txt"
(
  cd "$DIST"
  sha256sum Memo-Android-debug.apk BUILD-INFO.txt > SHA256SUMS.txt
)

# A public Blob URL avoids Vercel Deployment Protection on the static preview.
# Never print or persist the read/write token; only print the returned public URL.
DOWNLOAD_URL="./Memo-Android-debug.apk"
if [[ -n "${BLOB_READ_WRITE_TOKEN:-}" ]]; then
  echo "Uploading verified APK to the configured Vercel Blob store..."
  BLOB_RESPONSE="$(
    curl --fail-with-body --silent --show-error --location \
      --request PUT \
      --header "Authorization: Bearer $BLOB_READ_WRITE_TOKEN" \
      --header "x-api-version: 7" \
      --header "x-add-random-suffix: false" \
      --header "x-allow-overwrite: true" \
      --header "Content-Type: application/vnd.android.package-archive" \
      --data-binary "@$DIST/Memo-Android-debug.apk" \
      "https://blob.vercel-storage.com/memo/Memo-Android-debug-$COMMIT.apk"
  )"
  BLOB_URL="$(python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get("downloadUrl") or d.get("url") or "")' <<< "$BLOB_RESPONSE")"
  [[ "$BLOB_URL" == https://* ]] || { echo "::error::Blob upload did not return a public HTTPS URL."; exit 1; }
  printf '%s\n' "$BLOB_URL" > "$DIST/APK-DOWNLOAD-URL.txt"
  DOWNLOAD_URL="$BLOB_URL"
  echo "Public APK URL: $BLOB_URL"
else
  echo "::warning::BLOB_READ_WRITE_TOKEN is unavailable; the APK will only be in the Vercel deployment output and may require SSO."
fi

cat > "$DIST/index.html" <<HTML
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Memo Android build</title>
<style>
:root{color-scheme:dark;font:16px/1.5 system-ui,sans-serif;background:#0b1018;color:#e8eef7}
body{max-width:720px;margin:8vh auto;padding:24px}
main{border:1px solid #263449;border-radius:20px;padding:28px;background:#111a27}
h1{margin-top:0}a{color:#9bc8ff}code{overflow-wrap:anywhere}
small{color:#aab8ca}.download{display:inline-block;margin:18px 0;padding:12px 18px;border-radius:10px;background:#b8d7ff;color:#102033;text-decoration:none;font-weight:700}
</style>
</head>
<body><main>
<h1>Memo Android build</h1>
<p>This is a test build from the stabilization branch. It is not a production release.</p>
<p><a class="download" href="$DOWNLOAD_URL" download>Download Memo APK</a></p>
<p>Package: <code>app.memo.android.debug</code></p>
<p>Commit: <code>$COMMIT</code></p>
<p>SHA-256: <code>$APK_SHA</code></p>
<p><a href="./BUILD-INFO.txt">Build information</a> · <a href="./SHA256SUMS.txt">Checksums</a></p>
<small>Signing: $SIGNING_DESCRIPTION. If this uses the ephemeral debug key, Android will not allow it to update an installation signed with Memo's persistent upload certificate.</small>
</main></body></html>
HTML

echo "APK build complete: $APK_BYTES bytes; SHA-256 $APK_SHA"
