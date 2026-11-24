# STEREO Permanent Direct APK Download Architecture

## Overview
To provide a stable, permanent direct-download URL for Android testers, distribution portals (e.g. IzzyOnDroid, Discord communities, website download buttons), and direct side-loaders, STEREO utilizes an automated rolling release pipeline.

### Permanent Download URLs
- **Direct APK Download:**
  `https://github.com/rishibandaru3109/STEREO/releases/download/latest-beta/STEREO-latest.apk`
- **SHA-256 Checksum:**
  `https://github.com/rishibandaru3109/STEREO/releases/download/latest-beta/STEREO-latest.apk.sha256`
- **Rolling Release Page:**
  `https://github.com/rishibandaru3109/STEREO/releases/tag/latest-beta`

---

## Architecture & Automation Mechanism

### 1. Rolling Release Tag (`latest-beta`)
- Rather than forcing testers and external indices to scrape version tags (e.g. `v1.0.0-beta.1`, `v1.0.0-beta.2`), a dedicated rolling git tag and release `latest-beta` is maintained.
- The asset name is fixed as `STEREO-latest.apk`.

### 2. Synchronization Workflow (`rolling-release.yml`)
- Triggered automatically whenever an official GitHub Release is published (`on: release: types: [published]`).
- Also supports manual execution via `workflow_dispatch` with an optional target tag.
- **Recursion Prevention:** Explicitly checks that the triggering release is not `latest-beta`.
- **Integrity Verification:** Downloads the official release APK, computes its SHA-256 digest, and overwrites the rolling assets in `latest-beta` with `--clobber`.
- **Historical Immutability:** Historical releases (e.g., `v1.0.0-alpha.3`, `v1.0.0-alpha.4`, `v1.0.0-alpha.5`, `v1.0.0-beta.1`) remain completely untouched and immutable.

### 3. Production Signing & Verification
- The rolling APK is byte-for-byte identical to the official release-signed APK published under the corresponding version tag.
- Never re-signed or rebuilt in the rolling step; it directly mirrors the verified production build.
