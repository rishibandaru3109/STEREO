# STEREO v1.0.0-beta.2 Official Publication & Deployment Report
**Date:** 2026-11-24  
**Release Tag:** `v1.0.0-beta.2`  
**Rolling Tag:** `latest-beta`  
**Git Commit:** `a12bd65c2901306bb95c510df3132e01b3337a77`  
**Branch:** `master`  
**Package:** `com.nova.music`  
**versionCode:** `8` | **versionName:** `1.0.0-beta.2`  
**Status:** **OFFICIALLY PUBLISHED & VERIFIED LIVE**

---

## 1. Distribution Links & Endpoints

| Resource | URL | Description |
| :--- | :--- | :--- |
| **Permanent Direct APK URL** | `https://github.com/rishibandaru3109/STEREO/releases/download/latest-beta/STEREO-latest.apk` | **Direct download link that never changes.** Always serves the latest verified production APK across all future releases. |
| **Official Release Page** | `https://github.com/rishibandaru3109/STEREO/releases/tag/v1.0.0-beta.2` | GitHub Releases page containing full changelog, release notes, and assets. |
| **Versioned Direct APK URL** | `https://github.com/rishibandaru3109/STEREO/releases/download/v1.0.0-beta.2/STEREO-v1.0.0-beta.2-release.apk` | Permanent version-pinned download link specifically for Beta 2. |
| **Checksum Asset** | `https://github.com/rishibandaru3109/STEREO/releases/download/v1.0.0-beta.2/SHA256SUMS.txt` | Standard SHA-256 manifest for integrity validation. |
| **Rolling Release Page** | `https://github.com/rishibandaru3109/STEREO/releases/tag/latest-beta` | GitHub Releases tracking page for rolling beta distribution. |

---

## 2. Cryptographic & Binary Verification

- **Target APK Name:** `STEREO-v1.0.0-beta.2-release.apk` / `STEREO-latest.apk`
- **Binary Size:** `18,611,835 bytes` (~17.75 MB)
- **SHA-256 Digest:**
  ```
  a280c35f348d8d6d9af06bded3248685ac7ac4e61ceeb603a98bd6768bd23c8e
  ```
- **Signing Scheme:** APK Signature Scheme v2 (4096-bit RSA)
- **Signer DN:** `CN=STEREO Music Player, OU=STEREO Core Team, O=STEREO, L=Hyderabad, ST=Telangana, C=IN`
- **Certificate SHA-256 Fingerprint:**
  ```
  7E:38:21:4D:3F:83:E4:FD:CC:1B:03:50:32:85:19:41:83:92:DB:E4:F6:B2:F1:AC:5F:B2:BD:03:96:D6:C0:64
  ```
- **Signing Compatibility:** 100% cryptographic match with Alpha.4, Alpha.5, and Beta.1. Seamless in-place updates guaranteed without `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.

---

## 3. Real-Device Hardware Verification (Realme RMX5030, Android 16)

Prior to publication, the build was validated end-to-end on connected physical hardware (`75YTAYG6U4VKZ5OZ`):

1. **Streaming Audio Verification:**
   - Streamed *Jaymes Young — Infinity* (`WMK3JXG3Fx0`): audio playback initiated immediately, duration populated (3:57), playback progressed to `0:46+` with audible output, zero HTTP 403 errors.
   - Streamed *Stuck with U* (`yEA3qaB0dH8`): started instantly, duration populated (3:48).
   - Streamed *Tate McRae — you broke me first* (`0cmbxttS-RY`): verified audio stream playback and duration (2:50).

2. **Persistent Audio Downloads:**
   - Downloaded *Jaymes Young — Infinity*: 3.6 MB saved locally in private app storage (`files/offline_downloads/`).
   - Downloaded *Stuck with U*: 3.6 MB downloaded with real-time percentage progress (35% -> 53% -> 77% -> 100%).
   - Downloaded *you broke me first*: 2.6 MB saved locally.
   - Total offline library: `9.9 MB used • 3 songs offline`.

3. **Airplane Mode Offline Replay:**
   - Enabled Airplane Mode (`adb shell cmd connectivity airplane-mode enable`) with zero active Wi-Fi or cellular connectivity.
   - Replayed *you broke me first* from the Downloads screen.
   - Verified ExoPlayer initiated playback immediately from local disk (`/data/user/0/com.nova.music/files/offline_downloads/0cmbxttS-RY.m4a`) with full scrubber and play/pause controls.

4. **UI Responsiveness & Bug Fixes:**
   - **Downloads Tab Wrapping:** Replaced fixed `TabRow` with `ScrollableTabRow(edgePadding = 16.dp)`, added indicator boundary check `if (selectedTabIndex < tabPositions.size)`, and set `maxLines = 1, softWrap = false`. The `Downloade` / `d (0)` wrapping defect is completely resolved.
   - **Library Playlists Header:** Added `maxLines = 1, softWrap = false` and compact button padding `PaddingValues(horizontal = 10.dp, vertical = 6.dp)` so "Playlists" never wraps across lines.
   - **Download Queue Startup Recovery:** Added `init` block in `DownloadRepositoryImpl` querying `getActiveDownloads()` to automatically resume interrupted downloads across app process restarts.

5. **Full Unit Test Suite:**
   - 153/153 deterministic offline unit tests passed with 100% success rate (`.\gradlew.bat testDebugUnitTest`).

---

## 4. Real-World Spotify Playlist Import Verification

- **Test Playlist:** `https://open.spotify.com/playlist/2rjU7cseHr9R8tgiT9Ohll`
- **ID:** `2rjU7cseHr9R8tgiT9Ohll`
- **Extracted Title:** `Englishuuu🫶`
- **Creator / Subtitle:** `Aishu`
- **Track Count:** 100 tracks extracted cleanly via `PlaylistImportParser` embed scraper without requiring user credentials.
- **Track Order:** Preserved in exact sequence from track 1 (*Love Me Like You Do - Ellie Goulding*) to track 100.
- **Smart Matching Engine:** `TrackMatchingEngine` validated with fuzzy title normalization, video noise tag stripping, and version penalty filtering.

---

## 5. Automatic Rolling Release Workflow

A production GitHub Actions workflow is committed at `.github/workflows/rolling-release.yml`:
- **Trigger:** Fires automatically whenever a new release is published (`release: types: [published]`) or manually via `workflow_dispatch`.
- **Recursion Guard:** Ignores events from `latest-beta` itself to prevent recursive trigger loops.
- **Synchronization:** Downloads the verified release APK from the newly published release tag, validates SHA-256, updates `STEREO-latest.apk`, and uploads it with `--clobber` to the `latest-beta` release.
- **Zero-Maintenance Link:** Guarantees `https://github.com/rishibandaru3109/STEREO/releases/download/latest-beta/STEREO-latest.apk` will continue pointing to the latest approved production APK for Beta 3, Beta 4, and beyond.

---

## 6. Known Limitations & Policy
- **Audio Catalog Resolution:** Spotify playlist importing extracts metadata (title, artist, duration, ordering); audio streams are resolved through YouTube Music's catalog via InnerTubeX.
- **Offline Storage:** Offline downloads are strictly stored in private application sandboxes for personal offline playback.
