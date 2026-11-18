# Changelog

All notable changes to **STEREO** are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
