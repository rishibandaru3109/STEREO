<p align="center">
  <img src="docs/branding/stereo_app_icon.png" width="128" height="128" alt="STEREO App Icon" />
</p>

<h1 align="center">STEREO</h1>

<p align="center">
  <strong>Original Android music player with a streamlined interface and personalized recommendations.</strong>
</p>

<p align="center">
  <a href="https://github.com/rishibandaru3109/STEREO/releases"><img src="https://img.shields.io/badge/Release-v1.0.0--beta.1-blue.svg?style=flat-square" alt="Latest Release" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-green.svg?style=flat-square" alt="License: GPL-3.0" /></a>
  <a href="https://developer.android.com/about/versions/oreo"><img src="https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-orange.svg?style=flat-square" alt="Android 8.0+" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.0-purple.svg?style=flat-square" alt="Kotlin" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-teal.svg?style=flat-square" alt="Compose M3" /></a>
  <a href="CONTRIBUTING.md"><img src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square" alt="PRs Welcome" /></a>
</p>

---

## 🌟 Overview

**STEREO** is an original Android music streaming and discovery application built from the ground up with Jetpack Compose, Kotlin Coroutines, and AndroidX Media3. STEREO combines high-fidelity audio playback with an advanced, personalized recommendation engine covering **40 mainstream languages** across the globe, automatic on-device audio caching for zero-data replays, and the autonomous **SYNAPSE** recommendation engine.

All personal preferences and listening history remain strictly on-device: zero trackers, zero telemetry, zero mandatory user accounts, and zero cloud lock-in.

---

## 🚀 What's New in Release 2

### 1. Automatic Audio Caching
- **Automatic Stream Replay:** Audio received during normal playback is automatically cached on-device in `no_backup/automatic_audio_v2` via Media3's `SimpleCache`.
- **Zero Redundant Data Transfers:** Replaying a cached song reuses local byte ranges immediately with zero network requests.
- **Offline Playback for Cached Tracks:** Any track whose audio spans are fully cached can be replayed without an active internet connection.
- **Configurable Cache Storage:** Choose from 100 MB, 250 MB, 500 MB (default), 1 GB, or Unlimited directly from Settings. Least-recently-used (LRU) eviction automatically reclaims space when limits are reached while pinning active playback audio.

### 2. SYNAPSE Personalized Recommendations
- **Adaptive Behavioral Intelligence:** Learns dynamically from your real listening activity. Songs played to completion increase artist affinity; accidental skips are confidence-weighted to prevent premature artist banishing.
- **Dual-Horizon Taste Modeling:** Separates short-term session vibes from long-term taste centroids so a single casual workout doesn't overwrite your primary music preferences.
- **Personalized Dynamic Rails:**
  - **Quick Picks:** Fast entry shelf curated from your highest affinity tracks.
  - **Daily Mixes:** Curated personalized mixes featuring 4-quadrant dynamic album art covers.
  - **Weekly Discover:** Fresh music tailored to your vibe that you haven't heard yet.
  - **Because You Listened To [Artist]:** Lush carousels blending unplayed catalog tracks with similar artists via on-device Content Similarity modeling.
- **Maximal Marginal Relevance (MMR):** Calibrate discovery in Settings between Adventurous (0.2), Balanced (0.65), and Familiar (0.9).

---

## 📸 Screenshots

<p align="center">
  <img src="release-staging/release-2/screenshots/m2_01_home_top.png" width="30%" alt="Home & Recommendations" />
  &nbsp;
  <img src="release-staging/release-2/screenshots/m2_04_settings_mmr_badge.png" width="30%" alt="Settings & MMR Calibration" />
  &nbsp;
  <img src="release-staging/release-2/screenshots/m2_05_because_rail_lush.png" width="30%" alt="Lush Recommendations" />
</p>

---

## ✨ Features

- **Music Streaming & Search:** Low-latency online catalog search and playback powered by InnerTubeX and AndroidX Media3.
- **Background Playback & MediaSession:** Foreground service with rich media notification controls, lock-screen artwork, and system media integration.
- **Queue & Playlists:** Persistent listening queues, queue reordering, and custom playlist creation.
- **Favorites:** Instant liking and bookmarking of tracks preserved across app launches.
- **Automatic Audio Caching:** On-device streaming cache for data-free replays and offline playback of cached tracks.
- **SYNAPSE Personalized Recommendations:** Autonomous on-device recommendation engine modeling short-term vibes and long-term taste.
- **Personalized Discovery:** Dynamic Quick Picks, Daily Mixes, Weekly Discover, and "Because You Listened To" shelves.
- **Recommendation Preferences:** Multi-select language curation across 40 languages and MMR exploration sliders.
- **Scroll-Aware Surprise Me FAB:** Dynamic floating action button that collapses during scrolling and smoothly expands when idle.
- **Adaptive Theming:** 7 official themes including True Black (AMOLED), Frosted Glass, Cyberpunk Neon, and Material You dynamic color.

---

## 📥 Download & Installation

### Requirements
- **Android Version:** Android 8.0 (Oreo, API 26) or higher.
- **Architecture:** `arm64-v8a`, `armeabi-v7a`, `x86_64`.
- **Permissions:** Internet and Foreground Service Audio.

### Installation Steps
1. Navigate to [GitHub Releases](https://github.com/rishibandaru3109/STEREO/releases).
2. Download `STEREO-v1.0.0-beta.1-release.apk` from the latest release.
3. Open the downloaded file on your Android device.
4. If prompted, confirm installation from unknown sources for your browser or file manager.
5. **Seamless In-Place Update:** If you have an existing compatible installation (Release 1 Alpha.4 or Alpha.5), you can install directly over it without uninstalling; your playlists, favorites, and history will be preserved.

---

## 🔨 Building From Source

### Prerequisites
- [Android Studio Ladybug (2024.2.1)](https://developer.android.com/studio) or newer.
- JDK 17 or higher.
- Android SDK Platform 34 and Build-Tools 34.0.0.

### Steps
```bash
# Clone the repository
git clone https://github.com/rishibandaru3109/STEREO.git
cd STEREO

# Run the complete test suite (165 passing tests across STEREO and Synapse)
./gradlew testDebugUnitTest :Synapse:test

# Build debug APK
./gradlew assembleDebug

# Build production release APK (requires local keystore configuration)
./gradlew assembleRelease
```
The compiled APK will be generated under `app/build/outputs/apk/`.

*Note: The SYNAPSE recommendation engine is embedded directly as a subproject module in `Synapse/` and builds automatically with Gradle.*

---

## 🔒 Privacy

- **100% On-Device Data Storage:** Your playlists, listening history, liked songs, and SYNAPSE taste profile are stored strictly on-device in a local SQLite Room database.
- **Zero Trackers or Telemetry:** There are no analytics SDKs, advertising IDs, or tracking services embedded in STEREO.
- **Network Disclosure:** STEREO requires internet access solely to query public music catalog metadata and stream audio from content delivery servers.

---

## ⚠️ Known Limitations

- **Automatic Caching vs. Downloads:** Automatic caching saves audio during normal playback for seamless replays; it is **not** an explicit, user-controlled offline song downloader.
- **Offline Playback Scope:** Offline replay is available only for audio tracks that have already been fully cached during playback.
- **Stream Availability:** Streaming audio availability depends on media source servers; expired stream URLs require a network refresh.
- **Future Capabilities:** Explicit offline music downloads, local file playback (MP3/FLAC), and a unified offline library are planned for **Release 3**.

---

## 🗺️ Roadmap

- [x] **Release 1:** 40-Language music streaming, Jetpack Compose UI, Media3 playback, and production release signing.
- [x] **Release 2:** Automatic Audio Caching, SYNAPSE personalized recommendations, MMR exploration controls, and scroll-aware FAB.
- [ ] **Release 3:** Explicit offline song downloads, local MP3/FLAC file playback, download manager, and unified local library.
- [ ] **Future:** Synced lyrics display, sleep timer with volume fade, and Android Auto support.

---

## 🤝 Contributing

Contributions are welcomed! Please read our [Contributing Guide](CONTRIBUTING.md) and review our [Code of Conduct](CODE_OF_CONDUCT.md).

To report bugs or suggest enhancements, please use our [GitHub Issue Templates](https://github.com/rishibandaru3109/STEREO/issues/new/choose).

---

## 📄 License & Acknowledgments

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)** — see the [LICENSE](LICENSE) file for details.

Special thanks to the authors and maintainers of:
- [InnerTubeX](https://github.com/MetrolistGroup/innertubex) and [QuackJS](https://github.com/MetrolistGroup/quackjs)
- [AndroidX Media3](https://github.com/androidx/media)
- [Koin](https://github.com/InsertKoinIO/koin)
- [Coil](https://github.com/coil-kt/coil)
