#!/usr/bin/env python3
"""Builds the update manifest the app polls for new versions.

The manifest keeps the history of released versions, so a TV that has not been
updated for months can list the notes of everything it missed. The previous
manifest is read back from the published release and this version prepended, so
the history survives without being stored in the repository.

Usage: build-update-manifest.py <version-code> <version-name> <apk> <notes> <previous|-> <out>
"""
import hashlib
import json
import sys

HISTORY_LIMIT = 50


def notes_from(path):
    """The "- " bullets of the release notes, in order, as plain strings."""
    lines = []
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if line.startswith("- "):
                lines.append(line[2:].strip())
    return lines


def previous_versions(path):
    if path == "-":
        return []
    try:
        with open(path, encoding="utf-8") as handle:
            return json.load(handle).get("versions") or []
    except (OSError, ValueError):
        # No previous manifest, or an error page in its place: start fresh
        # rather than fail the release.
        return []


def main():
    version_code, version_name, apk_path, notes_path, previous_path, out_path = sys.argv[1:7]
    version_code = int(version_code)

    with open(apk_path, "rb") as handle:
        apk_bytes = handle.read()

    entry = {
        "versionCode": version_code,
        "versionName": version_name,
        "changelog": notes_from(notes_path),
    }
    versions = [entry] + [v for v in previous_versions(previous_path)
                          if v.get("versionCode") != version_code]
    versions.sort(key=lambda v: v.get("versionCode") or 0, reverse=True)

    manifest = {
        "apk": {
            # The "latest" release is rewritten on every release, so this URL
            # always points at the newest APK.
            "url": f"https://github.com/{sys.argv[7]}/releases/download/latest/app-release.apk",
            "sha256": hashlib.sha256(apk_bytes).hexdigest(),
            "sizeBytes": len(apk_bytes),
        },
        "versions": versions[:HISTORY_LIMIT],
    }

    with open(out_path, "w", encoding="utf-8") as handle:
        json.dump(manifest, handle, indent=2)
        handle.write("\n")

    print(f"{version_name} ({version_code}), {len(manifest['versions'])} versions listed")


if __name__ == "__main__":
    main()
