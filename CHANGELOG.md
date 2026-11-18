# Changelog

All notable changes to **STEREO** are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
