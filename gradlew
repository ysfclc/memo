#!/usr/bin/env sh
# Pinned Gradle bootstrap launcher. Prefers an already-installed Gradle 8.14.6,
# otherwise downloads the official distribution and verifies its SHA-256.
set -eu
VERSION='8.14.6'
SHA256='7988ed071b2a07900e2ec715fca15c6ab72bce6db433af301fa7fa7e9407bd24'
URL='https://services.gradle.org/distributions/gradle-8.14.6-bin.zip'

if command -v gradle >/dev/null 2>&1 && gradle --version 2>/dev/null | grep -q "^Gradle ${VERSION}$"; then
  exec gradle "$@"
fi

if ! command -v java >/dev/null 2>&1; then
  echo 'Java 17 or newer is required. Install JDK 17 and retry.' >&2
  exit 1
fi
if ! command -v curl >/dev/null 2>&1 || ! command -v unzip >/dev/null 2>&1; then
  echo 'Gradle 8.14.6 is not installed, and curl + unzip are required to bootstrap it.' >&2
  exit 1
fi

GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
CACHE="$GRADLE_USER_HOME/synthos-distributions"
INSTALL="$CACHE/gradle-$VERSION"
ARCHIVE="$CACHE/gradle-$VERSION-bin.zip"
mkdir -p "$CACHE"
if [ ! -x "$INSTALL/bin/gradle" ]; then
  TMP="$CACHE/.gradle-$VERSION-$$"
  rm -rf "$TMP"
  mkdir -p "$TMP"
  trap 'rm -rf "$TMP"' EXIT HUP INT TERM
  if [ ! -f "$ARCHIVE" ] || [ "$( (sha256sum "$ARCHIVE" 2>/dev/null || shasum -a 256 "$ARCHIVE" 2>/dev/null || true) | awk '{print $1}' )" != "$SHA256" ]; then
    rm -f "$ARCHIVE"
    echo "Downloading verified Gradle $VERSION distribution…" >&2
    curl --fail --location --retry 3 --retry-delay 2 "$URL" --output "$TMP/gradle.zip"
    if command -v sha256sum >/dev/null 2>&1; then
      ACTUAL="$(sha256sum "$TMP/gradle.zip" | awk '{print $1}')"
    else
      ACTUAL="$(shasum -a 256 "$TMP/gradle.zip" | awk '{print $1}')"
    fi
    if [ "$ACTUAL" != "$SHA256" ]; then
      echo 'Gradle distribution checksum mismatch. Refusing to execute it.' >&2
      exit 1
    fi
    mv "$TMP/gradle.zip" "$ARCHIVE"
  fi
  unzip -q "$ARCHIVE" -d "$TMP"
  rm -rf "$INSTALL"
  mv "$TMP/gradle-$VERSION" "$INSTALL"
  trap - EXIT HUP INT TERM
  rm -rf "$TMP"
fi
exec "$INSTALL/bin/gradle" "$@"
