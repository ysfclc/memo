#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

: "${MEMO_SOURCE_APK_URL:?MEMO_SOURCE_APK_URL must point to the previously verified APK}"
: "${BLOB_READ_WRITE_TOKEN:?BLOB_READ_WRITE_TOKEN is required for Blob publishing}"

if ! command -v curl >/dev/null 2>&1 || ! command -v node >/dev/null 2>&1 || ! command -v npm >/dev/null 2>&1 || ! command -v unzip >/dev/null 2>&1; then
  echo "curl, node, npm, and unzip are required." >&2
  exit 1
fi

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
mkdir -p artifacts

COMMIT="${VERCEL_GIT_COMMIT_SHA:-delivery}"
SHORT_COMMIT="${COMMIT:0:12}"
APK_NAME="Memo-debug-63b4fdf3d849.apk"
APK_PATH="$TMP_DIR/$APK_NAME"

echo "Downloading the already-built and tested Memo APK from the prior deployment..."
curl --fail --location --retry 5 --retry-all-errors --retry-delay 2 \
  --connect-timeout 30 --max-time 900 \
  --output "$APK_PATH" "$MEMO_SOURCE_APK_URL"

EXPECTED_SHA256="90dcc3f6f87770504a31bf55ee97c37468c74296e69850325e4a6824c0c53443"
ACTUAL_SHA256="$(sha256sum "$APK_PATH" | awk '{print $1}')"
if [ "$ACTUAL_SHA256" != "$EXPECTED_SHA256" ]; then
  echo "Source APK checksum mismatch: expected $EXPECTED_SHA256, got $ACTUAL_SHA256" >&2
  exit 1
fi
unzip -t "$APK_PATH" >/dev/null
cp "$APK_PATH" "artifacts/$APK_NAME"
printf '%s  %s\\n' "$ACTUAL_SHA256" "artifacts/$APK_NAME" > "artifacts/$APK_NAME.sha256"

echo "Publishing verified APK to Vercel Blob with multipart upload..."
npm install --prefix "$TMP_DIR/node" --no-save --no-audit --no-fund @vercel/blob
cat > "$TMP_DIR/node/upload.mjs" <<'NODE'
import { readFile } from 'node:fs/promises';
import { put } from '@vercel/blob';

const file = await readFile(process.argv[2]);
const blob = await put('memo/Memo-debug-63b4fdf3d849.apk', file, {
  access: 'public',
  addRandomSuffix: false,
  allowOverwrite: true,
  multipart: true,
  contentType: 'application/vnd.android.package-archive',
});
console.log('BLOB_DOWNLOAD_URL=' + blob.downloadUrl);
console.log('BLOB_ETAG=' + blob.etag);
NODE

BLOB_RESULT="$(node "$TMP_DIR/node/upload.mjs" "artifacts/$APK_NAME")"
printf '%s\\n' "$BLOB_RESULT"
BLOB_DOWNLOAD_URL="$(printf '%s\\n' "$BLOB_RESULT" | sed -n 's/^BLOB_DOWNLOAD_URL=//p')"
if [ -z "$BLOB_DOWNLOAD_URL" ]; then
  echo "Blob upload did not return a download URL." >&2
  exit 1
fi
printf '%s\\n' "$BLOB_DOWNLOAD_URL" > artifacts/APK_DOWNLOAD_URL.txt

EXPECTED_SIZE="$(wc -c < "artifacts/$APK_NAME" | tr -d ' ')"
REMOTE_HEADERS="$(curl --fail --silent --show-error --location --head "$BLOB_DOWNLOAD_URL")"
REMOTE_SIZE="$(printf '%s\\n' "$REMOTE_HEADERS" | awk 'BEGIN{IGNORECASE=1} /^content-length:/ {gsub(/\\r/,"",$2); size=$2} END{print size}')"
if [ -n "$REMOTE_SIZE" ] && [ "$REMOTE_SIZE" != "$EXPECTED_SIZE" ]; then
  echo "Hosted APK size mismatch: local=$EXPECTED_SIZE remote=$REMOTE_SIZE" >&2
  exit 1
fi

cat > artifacts/BUILD_INFO.txt <<EOF
Product: Memo Android
Variant: debug
Build commit: 63b4fdf3d849
Delivery commit: $COMMIT
Source: previously verified Vercel build
SHA-256: $ACTUAL_SHA256
Size: $EXPECTED_SIZE bytes
Download URL: $BLOB_DOWNLOAD_URL
EOF

echo "Verified download URL: $BLOB_DOWNLOAD_URL"
echo "Verified size: $EXPECTED_SIZE bytes"
echo "Build artifacts:"
ls -lh artifacts
