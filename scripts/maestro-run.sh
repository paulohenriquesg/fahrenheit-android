#!/usr/bin/env bash
# Runs a Maestro flow, working around Maestro's IME not being enabled.
#
# Maestro types by switching to its own input method,
# dev.mobile.maestro/.input.MaestroInputMethodService, which lives in the driver
# APK. On this setup the IME is never enabled, so the switch fails silently:
# `inputText` reports COMPLETED and not one character reaches the app, while
# `pressKey` keeps working (it goes via UiAutomator, not the IME). The result
# looks like an app bug rather than a harness problem.
#
# Enabling it once does not stick: Maestro uninstalls the driver when a run
# ends, which drops the component from enabled_input_methods, and the next
# install does not restore it. So the enable has to happen *inside* each run,
# after the driver is installed and before the first inputText.
#
# This starts a watcher that waits for the driver package to appear, enables the
# IME, and exits. Text entry happens several steps into a flow, so the watcher
# always wins the race comfortably.
#
# Usage: scripts/maestro-run.sh .maestro/login-smoke.yaml [extra maestro args...]
set -euo pipefail

ADB="${ADB:-adb}"
command -v "$ADB" >/dev/null 2>&1 || ADB="$HOME/Library/Android/sdk/platform-tools/adb"
MAESTRO="${MAESTRO:-maestro}"
command -v "$MAESTRO" >/dev/null 2>&1 || MAESTRO="$HOME/.maestro/bin/maestro"
IME="dev.mobile.maestro/.input.MaestroInputMethodService"

[ $# -ge 1 ] || { echo "usage: $0 <flow.yaml> [maestro args...]" >&2; exit 1; }

for v in ABS_HOST ABS_USERNAME ABS_PASSWORD; do
  [ -n "${!v:-}" ] || { echo "$v is not set - run 'direnv allow' first" >&2; exit 1; }
done

# Keep the screen up: an asleep device returns an EMPTY hierarchy, so every
# assertVisible fails as though the UI regressed.
"$ADB" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
"$ADB" shell svc power stayon true >/dev/null 2>&1 || true

enable_ime_when_driver_lands() {
  for _ in $(seq 1 120); do
    if "$ADB" shell pm list packages 2>/dev/null | tr -d '\r' | grep -q '^package:dev.mobile.maestro$'; then
      "$ADB" shell ime enable "$IME" >/dev/null 2>&1 && echo "[maestro-run] enabled Maestro IME" >&2
      return 0
    fi
    sleep 0.5
  done
  echo "[maestro-run] WARNING: driver never appeared; inputText will type nothing" >&2
}
enable_ime_when_driver_lands &
WATCHER=$!
trap 'kill "$WATCHER" 2>/dev/null || true' EXIT

FLOW="$1"; shift
"$MAESTRO" test "$FLOW" \
  -e ABS_HOST="$ABS_HOST" \
  -e ABS_USERNAME="$ABS_USERNAME" \
  -e ABS_PASSWORD="$ABS_PASSWORD" \
  "$@"
