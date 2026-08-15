#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EXTERNAL_DIR="$ROOT_DIR/ParallaxShell/shell/src/main/cpp/external"
mkdir -p "$EXTERNAL_DIR"

clone_dependency() {
  local name="$1" url="$2" ref="$3" marker="$4"
  local destination="$EXTERNAL_DIR/$name"
  if [[ -e "$destination/$marker" ]]; then
    printf '%s is already available\n' "$name"
    return
  fi
  local temporary="${destination}.tmp"
  rm -rf "$destination" "$temporary"
  printf 'Fetching %s (%s)\n' "$name" "$ref"
  git clone --depth 1 --branch "$ref" "$url" "$temporary"
  [[ -e "$temporary/$marker" ]] || {
    printf 'Missing expected file %s/%s\n' "$temporary" "$marker" >&2
    rm -rf "$temporary"
    exit 1
  }
  mv "$temporary" "$destination"
}

clone_dependency minizip-ng https://github.com/zlib-ng/minizip-ng.git 4.0.10 CMakeLists.txt
clone_dependency mbedtls https://github.com/Mbed-TLS/mbedtls.git mbedtls-3.6.4 CMakeLists.txt
clone_dependency bhook https://github.com/bytedance/bhook.git main bytehook/src/main/cpp/CMakeLists.txt
clone_dependency Dobby https://github.com/luoyesiqiu/Dobby.git master CMakeLists.txt

LOCK_FILE="$EXTERNAL_DIR/native-dependencies.lock"
: > "$LOCK_FILE"
for dependency in minizip-ng mbedtls bhook Dobby; do
  printf '%s %s\n' "$dependency" "$(git -C "$EXTERNAL_DIR/$dependency" rev-parse HEAD)" >> "$LOCK_FILE"
done
cat "$LOCK_FILE"

for required in \
  "$EXTERNAL_DIR/mbedtls/include/mbedtls/aes.h" \
  "$EXTERNAL_DIR/minizip-ng/CMakeLists.txt" \
  "$EXTERNAL_DIR/bhook/bytehook/src/main/cpp/CMakeLists.txt" \
  "$EXTERNAL_DIR/Dobby/CMakeLists.txt"; do
  [[ -f "$required" ]] || { printf 'Native dependency validation failed: %s\n' "$required" >&2; exit 1; }
done
