#!/usr/bin/env bash
# One-time device setup for Maestro flows. Safe to re-run.
#
# Without this, flows fail in ways that look like app bugs:
#
#   * inputText reports COMPLETED and types nothing. Maestro types by switching
#     to its own IME, which ships in the driver APK but is NOT enabled on a
#     fresh device, and `ime set` on a disabled IME fails silently. pressKey
#     keeps working throughout, so navigation looks fine while every character
#     is dropped.
#
#   * A dozing screen makes the view hierarchy come back EMPTY, so every
#     assertVisible fails as though the UI regressed.
set -euo pipefail

ADB="${ADB:-adb}"
command -v "$ADB" >/dev/null || ADB="$HOME/Library/Android/sdk/platform-tools/adb"
command -v "$ADB" >/dev/null || { echo "adb not found; set ADB=/path/to/adb" >&2; exit 1; }

MAESTRO_IME="dev.mobile.maestro/.input.MaestroInputMethodService"

"$ADB" wait-for-device

# Keep the screen up: an asleep device reports an empty hierarchy, not an error.
"$ADB" shell input keyevent KEYCODE_WAKEUP || true
"$ADB" shell svc power stayon true || true
"$ADB" shell settings put system screen_off_timeout 2147483647 || true

# The driver only exists while a flow runs, so the IME may not be installed yet.
# Enabling it persists in secure settings and survives driver reinstalls.
if "$ADB" shell ime list -a -s 2>/dev/null | tr -d '\r' | grep -q "^${MAESTRO_IME}$"; then
  "$ADB" shell ime enable "$MAESTRO_IME" >/dev/null
  echo "Enabled Maestro IME."
else
  echo "Maestro IME not present yet - run any flow once, then re-run this script." >&2
fi

# Deliberately NOT `ime set`: leave the normal keyboard active and let Maestro
# switch in and out per inputText. Forcing it as default breaks focus handling.
echo "Enabled IMEs:"
"$ADB" shell ime list -s 2>/dev/null | tr -d '\r' | sed 's/^/  /'
echo "Wakefulness: $("$ADB" shell dumpsys power 2>/dev/null | grep -o 'mWakefulness=[A-Za-z]*' | head -1)"
