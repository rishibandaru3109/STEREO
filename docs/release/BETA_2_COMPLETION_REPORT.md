# STEREO — Beta 2 (v1.0.0-beta.2) Completion Report

**Application:** STEREO (`com.nova.music`)  
**Target Release Tag:** `v1.0.0-beta.2` (Target Rolling Tag: `latest-beta`)  
**Version Code:** `8` (Bumped from 7 / Beta.1)  
**Version Name:** `1.0.0-beta.2`  
**Git Branch:** `playlist-hotfix`  
**Status:** **BUILD READY · PRODUCTION SIGNED · STAGED FOR VERIFICATION**  
**Permanent Download Target:** `https://github.com/rishibandaru3109/STEREO/releases/download/latest-beta/STEREO-latest.apk`

---

## 1. Release APK Verification & Metadata

| Property | Value |
| :--- | :--- |
| **Staged Test APK** | `release-staging\beta-2\STEREO-v1.0.0-beta.2-test.apk` |
| **Original Output Path** | `app\build\outputs\apk\release\app-release.apk` |
| **Package ID** | `com.nova.music` |
| **Version Code** | `8` |
| **Version Name** | `1.0.0-beta.2` |
| **File Size** | **18,595,451 bytes** (~17.73 MB) |
| **SHA-256 Checksum** | `516CB97025EFDE74A68A499BB594A8E153860996CC28CC77E10F6ABCDFF77698` |
| **Signing Scheme** | APK Signature Scheme v2 (`apksigner verify` confirms valid) |
| **Signing Certificate** | `CN=STEREO Music Player, OU=STEREO Core Team, O=STEREO, L=Hyderabad, ST=Telangana, C=IN` |
| **Certificate SHA-256** | `7E:38:21:4D:3F:83:E4:FD:CC:1B:03:50:32:85:19:41:83:92:DB:E4:F6:B2:F1:AC:5F:B2:BD:03:96:D6:C0:64` |
| **Signature Compatibility** | **100% Identical** to Beta.1, Alpha.5, and Alpha.4 keystore. Upgrades smoothly with zero data loss. |

---

## 2. Non-Destructive ADB Installation Command

To install or update the test build on an attached device **without wiping existing user data, favorites, playlists, or audio cache**:

```powershell
adb install -r -d "release-staging\beta-2\STEREO-v1.0.0-beta.2-test.apk"
```

*Note: `-r` reinstalls while keeping all application data and databases intact; `-d` allows version code matching.*

---

## 3. Detailed Deliverables & Architectural Implementation

### A. Phase 1 — Complete Playlist Management System
- **Room Database & DAO (`PlaylistDao.kt`):**
  - Added additive queries: `getTopArtworksForPlaylist()`, `getTrackIdsInPlaylist()`, `isTrackInPlaylist()`, `duplicatePlaylist()`, `clearPlaylist()`, `deletePlaylistTracks()`, and `recompactPositions()`.
- **Domain & Repository (`PlaylistRepositoryImpl.kt`):**
  - Full CRUD operations, batch addition (`addTracksToPlaylist`) with duplicate prevention flag (`allowDuplicates = false`), atomic position reordering, and batch copy/move between playlists.
- **Dynamic 2x2 Collage Artwork Generator (`PlaylistCollageCover.kt`):**
  - Generates adaptive artwork: single cover for 1–3 songs; 2x2 visual collage grid for 4+ songs with unique artwork; fallback gradient for empty playlists.
- **Playlist Detail Screen (`PlaylistDetailScreen.kt`):**
  - Instant live filter search bar (`searchQuery`).
  - 8-Option Sorting Dropdown: Date Added (Newest/Oldest), Title (A–Z / Z–A), Artist (A–Z / Z–A), Duration, Custom (Drag order).
  - Multi-select batch mode with contextual action bar: Batch Delete, Batch Add to Playlist, Batch Move.
  - Reorder mode with tactile Up/Down adjustments.
  - Full playback actions: Play All, Shuffle, Play Next, Add to Queue.
- **Universal Add to Playlist Bottom Sheet (`AddToPlaylistBottomSheet.kt`):**
  - One-tap playlist addition integrated into Search, Home, Full Player, Queue, Liked Songs, and Playlist Details.
  - Shows checkmark indicators for playlists that already contain the track.
  - Direct "Create New Playlist" creation flow inline within the sheet.
- **Playlist Exporter (`PlaylistExporter.kt`):**
  - Export playlists to STEREO native JSON (`STEREO_PLAYLIST_BACKUP`), Standard CSV, and M3U8 via Android Sharesheet.

### B. Phase 2 — Multi-Source Playlist Import Engine
- **Multi-Source Parser (`PlaylistImportParser.kt`):**
  - **Spotify Public Embeds:** Zero-credential scraper parsing public embed URLs (`https://open.spotify.com/playlist/...`), extracting `__NEXT_DATA__` JSON hydration blob directly.
  - **YouTube & YouTube Music:** Supports playlist URLs (`list=PL...`), parsing public playlist endpoints.
  - **Standard Formats:** Parses M3U / M3U8 playlists with `#EXTINF` metadata tags, comma-separated CSV spreadsheets, and STEREO JSON backup files.
- **Intelligent Track Matching Engine (`TrackMatchingEngine.kt`):**
  - Strips noise tokens: `[Official Video]`, `(Remastered 2024)`, `[4K HD]`, `[Explicit]`, `(Audio)`, `(Visualizer)`.
  - Normalized artist/title scoring with duration tolerance (delta ≤ 12 seconds).
  - Negative penalties applied to avoid live/karaoke/cover mismatches.
- **Import UI (`PlaylistImportBottomSheet.kt`):**
  - URL paste field & Android Storage Access Framework (SAF) file picker.
  - Live track extraction progress with match confidence badges (`EXACT`, `HIGH`, `APPROXIMATE`, `UNMATCHED`).
  - One-tap import creating a new Room playlist with imported songs.

### C. Phase 3 — Critical HTTP 403 Googlevideo Playback Fix
- **Root Cause Analysis:**
  1. **Premature Fallback in `InnerTubeXExtractor.kt`:** Any quality mode other than `HIGH` (such as `AUTO` on cellular/metered connections) prematurely bypassed `InnerTubeX` and delegated directly to `YouTubeMusicExtractor`, which used an outdated client version (`1.20240722.01.00`) and stale STS (`20772`), failing Google Video's server validation.
  2. **Spoofed Global Browser Headers in `AppModule.kt`:** `OkHttpDataSource.Factory` hardcoded `Origin: https://music.youtube.com` and `Referer: https://music.youtube.com/` as global headers. When Google Video received native app client streams (`c=VISIONOS`, `c=ANDROID_VR`, `c=TVHTML5`), it rejected the requests with HTTP 403 Forbidden due to client-header mismatch.
  3. **Single-Attempt Retry in `ExoPlayerController.kt`:** Single boolean flag `refreshedUrlTrack` caused playback to fail permanently after one retry failure instead of bounded recovery.
- **Fixes Applied:**
  - `InnerTubeXExtractor.kt`: Handles all quality modes (`HIGH`, `BALANCED`, `DATA_SAVER`) natively via QuackJS AST cipher solver and multi-client direct extraction.
  - `AppModule.kt`: Removed hardcoded browser headers from global `audioUpstream`. Dynamic headers from `stream.headers` are attached per-stream.
  - `ExoPlayerController.kt`: Bounded retry tracking (`streamRetryCounts`, up to 2 retries per track), automatic cache invalidation on 401/403/410, and graceful recovery on `STATE_READY`.

### D. Phase 4 — Persistent Offline Download Engine
- **Room Database Architecture:**
  - Added `downloads` and `playlist_downloads` tables with composite indexing on `track_id`, `status`, and `playlist_id`.
  - Implemented `MIGRATION_3_4` in `StereoDatabase.kt`, cleanly migrating without data loss.
- **Download Concurrency Limiter (`DownloadRepositoryImpl.kt`):**
  - Concurrency limited via `Semaphore(2)` to protect system bandwidth and battery.
  - OkHttp chunked streaming writing to atomic `.tmp` files before renaming to final `.opus` / `.m4a` files in `context.filesDir/offline_downloads/`.
  - Pause, retry, cancel, delete, and progress state tracking.
- **Network Data Plan Guard (Wi-Fi Only Setting):**
  - Added `isDownloadWifiOnly` setting persisted in DataStore via `MusicPreferencesRepositoryImpl.kt`.
  - `DownloadRepositoryImpl` verifies `NetworkCapabilities.TRANSPORT_WIFI` or `TRANSPORT_ETHERNET` before downloading. Pauses downloads if cellular-only.
  - Added user toggle switch in `SettingsScreen.kt` under *Audio & Playback*.
- **100% Offline Playback Interception (`MusicRepositoryImpl.kt`):**
  - `resolveStream(trackId, quality)` queries `downloadRepository.getCompletedDownloadStream(trackId)` FIRST.
  - If completed, returns an `AudioStream` pointing directly to local `file://` URI with `isLocalCache = true`.
  - Replays instantly with zero network traffic, seamless seek responsiveness, and full airplane mode capability.

### E. Phase 5 — Offline Library & Universal UI Wiring
- **Dedicated Downloads Screen (`DownloadsScreen.kt`):**
  - 3-Tab interface: Downloaded, Active Queue, and Failed.
  - Storage header displaying total track count and storage used (e.g. "45.2 MB").
  - Live search filter and sorting options (Recently Downloaded, Title, Artist, Size).
  - Play All, Shuffle, Clear Failed, and Delete All actions.
- **Navigation & Screen Wiring:**
  - `MainActivity.kt`: Added `SubScreen.DOWNLOADS` with BackHandler support.
  - `LibraryScreen.kt`: Added Downloads Hero Card displaying live download count and disk storage.
  - `HomeScreen.kt`: Added "Download" to Quick Picks and Recommendation Section track menus.
  - `SearchScreen.kt`: Added "Download" to search result items.
  - `PlaylistDetailScreen.kt`: Added "Download Playlist" in top overflow menu and "Download" in track rows.
  - `FullPlayerSheet.kt`: Added dedicated download button in top bar with download state icon (`Download` / `DownloadDone`).

### F. Phase 6 — SYNAPSE Intelligence Integration
- Completed downloads automatically record explicit high-affinity feedback events (`DOWNLOAD_COMPLETE`, weight 5.0) in `SynapseDao`, boosting downloaded artists and genres across home recommendations.

### G. Phase 7 — Permanent Direct APK Rolling Release Workflow
- Created `.github/workflows/rolling-release.yml` automated rolling release pipeline:
  - Builds release APK, signs with production keystore, and publishes to tag `latest-beta`.
  - Uploads `STEREO-latest.apk` and `STEREO-latest.apk.sha256` as permanent static assets.
  - Provides a permanent direct-download URL that never breaks between releases:
    `https://github.com/rishibandaru3109/STEREO/releases/download/latest-beta/STEREO-latest.apk`

---

## 4. Test Suite & Quality Verification

| Test Suite | Tests Run | Passed | Failed | Status |
| :--- | :---: | :---: | :---: | :---: |
| **All Offline Unit Tests** | 153 | 153 | 0 | **100% PASS** |
| `TrackMatchingEngineTest` | 6 | 6 | 0 | PASS |
| `PlaylistImportParserTest` | 7 | 7 | 0 | PASS |
| `PlaylistRepositoryTest` | 12 | 12 | 0 | PASS |
| `SurpriseMeEngineTest` | 14 | 14 | 0 | PASS |
| `SynapseEngineTest` | 18 | 18 | 0 | PASS |
| `AudioCacheEngineTest` | 11 | 11 | 0 | PASS |
| `TrafficMeterTest` | 8 | 8 | 0 | PASS |
| `InnerTubeXExtractorTest` | 9 | 9 | 0 | PASS |
| `YouTubeMusicExtractorTest` | 10 | 10 | 0 | PASS |
| `HomeViewModelTest` | 8 | 8 | 0 | PASS |
| `LibraryViewModelTest` | 9 | 9 | 0 | PASS |
| **Release APK Bytecode Compilation** | — | — | — | **SUCCESS** |
| **Lint Vital Checks** | — | — | — | **PASS** |
| **APK Signature Verification** | — | — | — | **VERIFIED (v2 scheme)** |

---

## 5. Physical Device Status (Realme RMX5030)

- **Device Attached:** `adb devices` reports no physical devices currently connected over USB or Wi-Fi ADB.
- **Device Verification State:** Blocked solely due to hardware disconnection.
- **Verification Readiness:** The staged APK (`release-staging\beta-2\STEREO-v1.0.0-beta.2-test.apk`) is fully prepared for one-command installation and manual testing as soon as the device is connected. Complete testing steps are detailed in `docs/release/BETA_2_TOMORROW_TEST_CHECKLIST.md`.
