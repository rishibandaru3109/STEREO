# STEREO Release 2 — Official GitHub Publication Report
**v1.0.0-beta.1 · SYNAPSE Recommendations + Automatic Audio Caching**
**Publication Timestamp**: 2026-11-23T09:58:42Z (November 23, 2026, 15:28 IST)
**Publication Authority**: Explicit user authorization following successful physical device installation and playback verification.

---

## 1. Publication Executive Summary

STEREO Release 2 has been officially and successfully published to GitHub as a public pre-release. The release delivers two headline architectural capabilities: **Automatic Audio Caching** (Media3 `SimpleCache`) and the autonomous **SYNAPSE Personalized Recommendation Engine** (dual-horizon preference modeling), along with full 40-language music discovery, UI refinements, and seamless in-place upgrade continuity from Release 1.

| Field | Publication Record |
| :--- | :--- |
| **GitHub Release URL** | [https://github.com/rishibandaru3109/STEREO/releases/tag/v1.0.0-beta.1](https://github.com/rishibandaru3109/STEREO/releases/tag/v1.0.0-beta.1) |
| **Published APK URL** | [https://github.com/rishibandaru3109/STEREO/releases/download/v1.0.0-beta.1/STEREO-v1.0.0-beta.1-release.apk](https://github.com/rishibandaru3109/STEREO/releases/download/v1.0.0-beta.1/STEREO-v1.0.0-beta.1-release.apk) |
| **Checksums URL** | [https://github.com/rishibandaru3109/STEREO/releases/download/v1.0.0-beta.1/SHA256SUMS.txt](https://github.com/rishibandaru3109/STEREO/releases/download/v1.0.0-beta.1/SHA256SUMS.txt) |
| **Release Tag** | `v1.0.0-beta.1` |
| **Release Commit (Tag HEAD)**| `e3b8a96a578c2a84c3a5dfd9da593f77d1086507` |
| **Target Architecture** | Android 8.0+ (API 26+) · Kotlin 2.0 · Jetpack Compose · Media3 1.3.1 |
| **Pre-Release Status** | `True` (Beta Release) |
| **Repository** | `https://github.com/rishibandaru3109/STEREO` |

---

## 2. Published Artifact Integrity & Metadata

The published APK was independently downloaded from the live GitHub release endpoint and validated byte-for-byte against local build outputs and checksum records.

### APK Specifications
- **File Name:** `STEREO-v1.0.0-beta.1-release.apk`
- **Application ID:** `com.nova.music`
- **Version Name:** `1.0.0-beta.1`
- **Version Code:** `7` (strictly monotonic upgrade over Release 1 `vCode = 6`)
- **Min SDK:** 26 (Android 8.0 Oreo)
- **Target SDK:** 34 (Android 14)
- **File Size:** 18,415,227 bytes (~18.4 MB)
- **SHA-256 Checksum:** `8DE248781E2E4BF810BBCD58B87291EDDFD56E4E1D6C3DED251EF4B5C852EB3E`

### Cryptographic Signature Verification
```powershell
apksigner verify --verbose --print-certs STEREO-v1.0.0-beta.1-release.apk
# Verifies: true
# Signature Scheme: v2 (APK Signature Scheme v2)
# Signer #1 DN: CN=STEREO Music Player, OU=STEREO Core Team, O=STEREO, L=Hyderabad, ST=Telangana, C=IN
# Signer #1 Key: RSA 4096-bit
# Signer #1 SHA-256 Digest: 7e38214d3f83e4fdcc1b0350328519418392dbe4f6b2f1ac5fb2bd0396d6c064
```
**Continuity Verdict:** Identical signing certificate as Release 1 Alpha.4 and Alpha.5. Android Package Manager permits seamless in-place installation without data loss or uninstall.

---

## 3. Scope of Implemented Features

### Headline Feature 1: Automatic Audio Caching
- **Media3 SimpleCache:** Audio streamed during playback is progressively written to `no_backup/automatic_audio_v2`.
- **Zero Redundant Data Transfers:** Repeat playback reuses local byte spans directly, eliminating bandwidth consumption.
- **Offline Playback for Cached Tracks:** Any track whose byte ranges are fully cached can be played offline without an internet connection.
- **Cache Management:** Configurable quotas (100 MB, 250 MB, 500 MB default, 1 GB, Unlimited) with automated least-recently-used (LRU) eviction and active playback lock protection.

### Headline Feature 2: SYNAPSE Personalized Recommendations
- **Behavioral Intelligence:** Tracks listening duration and completion ratios to model affinity; skips are confidence-weighted ($N \ge 2$ required for artist penalties) to prevent accidental artist suppression.
- **Dual-Horizon Taste Centroids:** Decouples temporary listening vibes from long-term taste preferences.
- **Personalized Shelves:** Quick Picks, Daily Mixes (4-quadrant dynamic album art covers), Weekly Discover, and "Because You Listened To [Artist]" rails.
- **Maximal Marginal Relevance (MMR):** Exploration slider allowing fine-tuning between Adventurous (0.2), Balanced (0.65), and Familiar (0.9).
- **Embedded Modularity:** Standalone Kotlin library embedded directly at `Synapse/` ensuring reproducible, portable builds.

### UI Polish & Ergonomics
- **Scroll-Aware Surprise Me FAB:** Dynamic floating action button auto-collapses to a compact 48.dp circular icon button during feed scrolling, preventing content occlusion, and expands back on idle.
- **Two-Line Title Alignment:** Shelf cards feature clean 2-line title wrapping with fixed minimum line counts for uniform carousel alignment.
- **Navigation Clearance:** 160.dp / 230.dp bottom padding ensuring all tracks, menus, and bottom shelves remain reachable above navigation bars.
- **Settings Typography:** Refined badge pills and calibration anchors with zero text collision.

---

## 4. Verification & Testing Summary

### Automated Test Suite
- **STEREO Application:** 136 passing unit tests, 0 failures, 100% success rate (`app/build/reports/tests/testDebugUnitTest/index.html`).
- **SYNAPSE Library:** 29 passing unit tests, 0 failures, 100% success rate.
- **Combined Total:** **165 passing tests, 0 failures**.

### Static Analysis & Release Lint
- Strict release lint enforced (`checkReleaseBuilds = true`, `abortOnError = true`).
- `lintRelease` and `lintVitalRelease` completed with **0 errors**.
- Suppressed only Google Media3 internal `@UnstableApi` annotations.

### Physical Device Validation
- Tested on physical Realme RMX5030 (`RMX5030IN`, Android 16).
- Validated real audio caching, playback continuity, dynamic recommendation rails, UI layout clearance, and user-confirmed operation.

---

## 5. Security & Privacy Audit

- **Zero Trackers:** No analytics SDKs, telemetry frameworks, or advertising IDs.
- **Local SQLite Room Storage:** Listening history, liked songs, and taste profiles remain strictly on-device.
- **Zero Exposed Secrets:** Keystores, passwords, and tokens are completely excluded from Git tracked files and remote commits.
- **Public Infrastructure Budget:** ₹0 infrastructure cost; serverless and independent.

---

## 6. Known Limitations & Release 3 Backlog

### Release 2 Limitations
- **Cache vs. Downloads:** Automatic caching saves audio during normal streaming; it is **not** an explicit user-controlled file downloader.
- **Offline Replay Scope:** Playback without an active internet connection is available only for audio tracks that have been fully cached locally during previous streaming.
- **Stream URL Refresh:** Pausing streams across several hours may require refreshing temporary stream URLs from content delivery servers.

### Outstanding Release 3 Backlog
- Explicit user-managed song downloads.
- Local MP3/FLAC music playback.
- Dedicated download manager.
- Unified local library screen.
- Synced, interactive lyrics display.
- Sleep timer with gradual volume fade-out.
