# Changelog

All notable changes to **STEREO** are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0-beta.2] - 2026-11-24

### Added
- **Complete Playlist Management System:**
  - Create, rename, edit descriptions, set custom artwork URLs, pin, duplicate, clear, and delete playlists with zero media loss.
  - Dynamic 2x2 collage artwork generator (`PlaylistCollageCover`) displaying a 4-quadrant grid from top track artworks, with graceful fallbacks for 1-3 tracks or empty playlists.
  - Multi-select batch mode supporting multi-track removal, move between playlists, and copy to target playlists.
  - Interactive reordering mode (Move Up / Move Down) preserving custom track sequencing in Room database.
  - 8 sorting orders: Custom Order (saved), Recently Added, Oldest Added, Title (A-Z), Title (Z-A), Artist (A-Z), Artist (Z-A), and Duration.
  - Live in-playlist search filter for instant real-time song filtering.
  - Queue operations: Play All, Shuffle Play, Play Next, and Add to Queue without creating redundant Media3 player instances.
- **Universal Add to Playlist Bottom Sheet:**
  - Accessible universally across the app: Search results, Home shelves (Quick Picks, Daily Mix, Weekly Discover, Because You Listened), Queue track rows, Full Player Sheet, Liked Songs, and Playlist tracks.
  - In-sheet instant playlist creation, multi-playlist target selection, live search, and indicators showing playlists already containing the selected song.
- **Multi-Format Playlist Import Engine:**
  - Zero-credential Spotify playlist import via public embed metadata scraper.
  - YouTube and YouTube Music public playlist import.
  - Local file import for M3U, M3U8, CSV, and STEREO Native JSON backup formats.
  - Intelligent fuzzy matching engine (`TrackMatchingEngine`) featuring noise stripping (video/remaster/quality tags), artist normalization, duration delta tolerance, and version penalties preventing cover/karaoke hijacking.
- **Multi-Format Playlist Exporter:**
  - One-tap export to STEREO Native JSON (`STEREO_PLAYLIST_BACKUP`), standard CSV, and M3U8 with Android Sharesheet integration.
- **Persistent Offline Downloads System:**
  - Save full-fidelity tracks to private app storage (`files/offline_downloads/`).
  - Dedicated Downloads screen featuring Downloaded, Active Queue (with live byte and progress percentage), and Failed tabs.
  - Verified offline airplane-mode playback with zero network connectivity.
  - Startup queue recovery (`init { getActiveDownloads() }`) automatically resuming interrupted downloads.
  - Wi-Fi only download preference toggle in Settings.
- **SYNAPSE Recommended Songs:**
  - Contextual "Recommended Songs" section at the bottom of playlists with quick one-tap "+" addition.
  - Intent logging for playlist additions and explicit downloads to the SYNAPSE recommendation engine without misclassifying removals as negative feedback.
- **Permanent Direct APK Download:**
  - Rolling GitHub Actions workflow synchronizing `latest-beta` tag and fixed download URL `https://github.com/rishibandaru3109/STEREO/releases/download/latest-beta/STEREO-latest.apk`.

### Fixed
- **HTTP 403 Stream Resolution:** InnerTubeX VisionOS client profile resolving unthrottled direct Google Video streams without stream expiry or 403 Forbidden errors.
- **Downloads Tab Wrap Defect:** Replaced fixed `TabRow` with `ScrollableTabRow`, guarded indicator bounds, and added `maxLines = 1, softWrap = false` to eliminate `Downloade`/`d (0)` wrapping and tab switching crashes.
- **Library Playlists Header Wrap Defect:** Repaired `Playlists` header wrapping on narrow screens with compact button padding and single-line constraints.
- **PRNG Reseeding Lock:** Fixed `SurpriseMeEngineImpl` pseudo-random generator re-seeding bug during rapid successive sampling.
- **Noise Regex Normalization:** Refined bracketed noise stripping in `TrackMatchingEngine` for compound tags (e.g. `[4K Remastered]`).

### Compatibility
- Signed with the permanent production RSA 4096-bit certificate (`7E:38:21:4D:3F:83:E4:FD:CC:1B:03:50:32:85:19:41:83:92:DB:E4:F6:B2:F1:AC:5F:B2:BD:03:96:D6:C0:64`).
- **Seamless Upgrade:** All existing Alpha.4, Alpha.5, and Beta.1 users can install and update directly with 100% preservation of all listening history, favorites, settings, and cached audio.

---

## [1.0.0-beta.1] - 2026-11-23

### Added
- **Automatic Audio Caching:** Progressive Media3 `SimpleCache` pipeline automatically caching audio streams in app storage (`no_backup/automatic_audio_v2`), eliminating redundant network data on track replay.
- **Offline Validated Playback:** Seamless playback for fully cached songs without active network connectivity.
- **SYNAPSE Personalized Recommendation Engine:** Autonomous dual-horizon recommendation engine tracking listening duration, completion ratios, contextual skips, and explicit likes.
- **Dynamic Recommendation Shelves:** Quick Picks, Daily Mixes, Weekly Discover, and "Because You Listened To [Artist]" rails powered by Maximal Marginal Relevance (MMR) diversity reranking.
- **Discovery Calibration (MMR):** User-adjustable exploration slider in Settings featuring Adventurous (0.2), Balanced (0.65), and Familiar (0.9) anchors.
- **Scroll-Aware Surprise Me FAB:** Dynamic floating action button that auto-collapses to a compact 48.dp circular icon button during list scrolling and smoothly expands on idle.
- **Room Migration 2 to 3:** Automated SQLite schema upgrade creating `synapse_listening_events` and `synapse_taste_profile` tables with performance indices.

### Changed
- **Typography & Shelf Ergonomics:** Card titles wrap cleanly across 2 lines with 16.sp/18.sp line heights and fixed minimum line counts for uniform carousel height.
- **Bottom Content Clearance:** Extended bottom content padding (160.dp / 230.dp with mini-player) ensuring all bottom tracks and menus clear system navigation bars.
- **Confidence-Weighted Skip Penalties:** Accidental single skips ($N=1$) no longer banish entire artist catalogs; artist-level penalties now require repeated skips ($N \ge 2$) and recover on subsequent listens or likes.
- **Dynamic Version Reflection:** Settings About screen dynamically reflects `BuildConfig.VERSION_NAME` and `BuildConfig.VERSION_CODE`.

### Compatibility
- Signed with the identical permanent production RSA 4096-bit certificate as Alpha.4 and Alpha.5 (`7E:38:21:4D:3F:83:E4:FD:CC:1B:03:50:32:85:19:41:83:92:DB:E4:F6:B2:F1:AC:5F:B2:BD:03:96:D6:C0:64`).
- **Seamless Upgrade:** Existing Alpha.4 and Alpha.5 users can update directly without uninstalling or resetting user data.

---

## [1.0.0-alpha.5] - 2026-11-19

### Added
- **Test Architecture Separation:** Separated 101 deterministic offline unit tests from external network integration tests using JUnit categories (`LiveNetworkTest`).
- **Dedicated Live Integration Suite:** Preserved comprehensive live YouTube streaming and audio container validation tests in a separate `testLiveIntegration` task without gating standard CI.

### Changed
- **Extractor Resilience:** Enhanced `InnerTubeXExtractor` with `playlistItemData.videoId` extraction and improved `YouTubeMusicExtractor` with support for multi-column search layouts and fallback subtitle duration parsing.
- **Build Performance & Memory:** Fine-tuned JVM and Kotlin daemon parameters in `gradle.properties` (`-Xmx1536m -XX:MaxMetaspaceSize=384m`) preventing compiler daemon memory exhaustion.
- **Automated CI Validation:** Verified 100% green pass on GitHub Actions Linux CI for unit tests and release builds.

### Compatibility
- Signed with the identical permanent production RSA 4096-bit certificate as Alpha.4 (SHA-256: `7E:38:21:4D:3F:83:E4:FD:CC:1B:03:50:32:85:19:41:83:92:DB:E4:F6:B2:F1:AC:5F:B2:BD:03:96:D6:C0:64`).
- **Seamless Upgrade:** Existing Alpha.4 users can install and update directly without uninstalling or resetting user data.

---

## [1.0.0-alpha.4] - 2026-11-19

### Added
- **Permanent Production Release Signing:** Upgraded from the legacy debug signing certificate to a dedicated RSA 4096-bit production keystore valid through April 2054.
- **Hierarchical Credential Security:** Configured Gradle to securely resolve signing credentials from environment variables or ignored properties without exposing secrets.
- **Migration & Signing Documentation:** Added complete documentation on certificate fingerprints, backup protocols, and one-time Alpha.3 migration in `docs/SIGNING_AND_MIGRATION_GUIDE.md`.

### Changed
- Production release builds are now permanently signed under APK Signature Scheme v2 & v3 with SHA-256 fingerprint `7E:38:21:4D:3F:83:E4:FD:CC:1B:03:50:32:85:19:41:83:92:DB:E4:F6:B2:F1:AC:5F:B2:BD:03:96:D6:C0:64`.

### Note for Alpha.3 Testers
- Due to Android's security signature verification, upgrading from Alpha.3 requires a one-time clean uninstall of the old build before installing Alpha.4. All subsequent releases will update seamlessly.

---

## [1.0.0-alpha.3] - 2026-11-18

### Added
- **40-Language Music Discovery Expansion:** Added comprehensive support for 40 mainstream music languages grouped into 5 geographical regions (South Asia, Europe & Americas, East & Southeast Asia, Middle East & Africa, and Additional).
- **Interactive Searchable Language Picker:** Multi-select dialog featuring real-time query filtering across English and native script titles, dynamic region badges, and selection count indicator.
- **Multilingual & Multi-Script Classification:** Support for 18 Unicode script ranges (Devanagari, Gurmukhi, Gujarati, Bengali, Tamil, Telugu, Kannada, Malayalam, Hangul, Kana, CJK, Thai, Burmese, Hebrew, Arabic, Cyrillic, Greek, Ethiopic) and romanized keywords.
- **Network Throttling:** Smart request limiting capping targeted discovery queries to a maximum of 3 concurrent requests to prevent network starvation.
- **Safe Fallback Heuristic:** Prevents empty Home feed states when users select niche single-language cold starts without explicit script metadata.
- **Detailed Provenance Diagnostics:** Comprehensive logging tracking seed count, radio candidates, supplement pool, shortlist, accept count, and fallback status.

### Fixed
- Fixed empty recommendations bug on cold start when strict language filters rejected unclassified audio candidates.
- Fixed high-contrast visibility on "Save & Discover" action button in onboarding and settings dialogs.
- Fixed soft-keyboard obscuring dialog bottom sheet action buttons.
- Enhanced settings preference row to cleanly display multi-language selections with `+N more` format.

---

## [1.0.0-alpha.2] - 2026-11-18

### Added
- **Native Pull-to-Refresh:** Integrated Material 3 pull-to-refresh indicators on Home feed and Search view for seamless recommendation updates without interrupting audio playback.
- **Surprise Me Taste FAB:** Instant randomized playback engine seeded by user taste affinities, liked songs, and listening history.
- **Adaptive Monochrome Icon:** Added full vector monochrome launcher asset (`ic_launcher_monochrome.xml`) supporting Android 13+ Material You themed icons.

### Fixed
- Resolved static refresh indicator freezing on initial launch.
- Fixed stream URL expiration handling on long pause intervals.
- Corrected contrast tokens on playback sheet action buttons across frosted glass themes.

---

## [1.0.0-alpha.1] - 2026-11-17

### Added
- **Master UI/UX Redesign:** Complete native Jetpack Compose architecture with modern 3-tab navigation (Home, Search, Library).
- **7 Theme Engine:** Classic Dark, True Black (AMOLED), Frosted Glass, Vintage Deck, Cyberpunk Neon, Pastel Breeze, and Deep Midnight.
- **2 Card Layout Modes:** Minimalist compact cards and detailed rich media cards.
- **YouTube Music Pipeline:** Integrated InnerTubeX extractor for music search, catalog discovery, song radio, and audio streaming.
- **Media3 Background Audio:** Foreground service with lock-screen notification controls, media button receivers, and gapless transition hooks.
- **Local Persistence:** Room database tracking listening history, liked songs, custom playlists, and offline queue snapshots.
