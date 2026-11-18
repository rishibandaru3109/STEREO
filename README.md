<p align="center">
  <img src="docs/branding/stereo_app_icon.png" width="128" height="128" alt="STEREO App Icon" />
</p>

<h1 align="center">STEREO</h1>

<p align="center">
  <strong>An independent, modern, open-source Android music player powered by Jetpack Compose and Material 3.</strong>
</p>

<p align="center">
  <a href="https://github.com/rishibandaru3109/STEREO/releases"><img src="https://img.shields.io/badge/Release-v1.0.0--alpha.4-blue.svg?style=flat-square" alt="Latest Release" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-green.svg?style=flat-square" alt="License: GPL-3.0" /></a>
  <a href="https://developer.android.com/about/versions/oreo"><img src="https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-orange.svg?style=flat-square" alt="Android 8.0+" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.0-purple.svg?style=flat-square" alt="Kotlin" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-teal.svg?style=flat-square" alt="Compose M3" /></a>
  <a href="CONTRIBUTING.md"><img src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square" alt="PRs Welcome" /></a>
</p>

---

## 🌟 Overview

**STEREO** is a native Android music streaming and discovery application built from the ground up with Jetpack Compose, Kotlin Coroutines, and AndroidX Media3. STEREO combines high-fidelity audio playback with an advanced, personalized recommendation engine covering **40 mainstream languages** across the globe.

Everything is local-first: zero trackers, zero telemetry, zero mandatory user accounts, and zero cloud lock-in.

---

## 📸 Screenshots

<p align="center">
  <img src="docs/screenshots/01_home_recommendations.png" width="30%" alt="Home & Recommendations" />
  &nbsp;
  <img src="docs/screenshots/02_music_search.png" width="30%" alt="Search & Instant Results" />
  &nbsp;
  <img src="docs/screenshots/03_fullscreen_player.png" width="30%" alt="Full-Screen Player" />
</p>

<p align="center">
  <img src="docs/screenshots/04_library_playlists.png" width="30%" alt="Library & Playlists" />
  &nbsp;
  <img src="docs/screenshots/05_40_language_selection.png" width="30%" alt="40-Language Regional Picker" />
  &nbsp;
  <img src="docs/screenshots/06_settings_customization.png" width="30%" alt="Settings & Preferences" />
</p>

---

## ✨ Key Features

### 🌍 40-Language Music Discovery
Discover music in the languages you actually love. STEREO features a searchable, multi-select language dialog organized across 5 regions with native typography and multi-script Unicode classification:
- **South Asia (11):** Hindi, Telugu, Tamil, Malayalam, Kannada, Bengali, Punjabi, Marathi, Gujarati, Urdu, Nepali.
- **Europe & Americas (11):** English, Spanish, Portuguese, French, German, Italian, Russian, Ukrainian, Polish, Dutch, Greek.
- **East & Southeast Asia (10):** Korean, Japanese, Mandarin Chinese, Cantonese, Indonesian, Malay, Thai, Vietnamese, Filipino, Burmese.
- **Middle East & Africa (6):** Arabic, Turkish, Persian, Hebrew, Swahili, Amharic.
- **Additional (2):** Romanian, Hungarian.

### 🎯 Personalized Recommendation Pipeline
- **YouTube Music Song Radio:** Dynamic candidates seeded from your liked songs, repeated plays, and listening habits.
- **Smart Metadata Classification:** 18 script regex engines and romanized transliteration keywords prevent language mismatches without cutting off valid tracks.
- **Native Pull-to-Refresh:** Smooth Material 3 swipe-down gestures on Home and Search refresh feeds on demand without interrupting music playback.
- **Safe Fallback:** Guaranteed non-empty home feeds even during niche single-language cold starts.
- **Surprise Me FAB:** An instant randomized playback mode seeded by user taste affinities.

### 🎵 Premium Playback Experience
- **AndroidX Media3 / ExoPlayer:** Low-latency audio streaming with automated buffer management.
- **Background Playback & Lock-Screen:** Foreground service with rich media notification controls and gapless transitions.
- **Persistent Queues & Shuffle:** Save, inspect, reorder, and shuffle active listening queues.

### 🎨 Deep Theming & Visuals
- **7 Official Themes:** Classic Dark, True Black (AMOLED), Frosted Glass, Vintage Deck, Cyberpunk Neon, Pastel Breeze, and Deep Midnight.
- **2 Card Layout Modes:** Toggle between clean minimalist cards and detailed rich media cards.
- **Themed App Icon:** Full adaptive launcher icon suite and vector monochrome asset for Android 13+ Material You icon theming.

### 🔒 Privacy & Local Storage
- **100% On-Device Data:** Playlists, listening history, liked songs, and preferences are stored strictly in local SQLite via Room.
- **No Account Required:** No Google login, no subscription paywalls, no behavioral tracking, and no external telemetry SDKs.

---

## 📥 Download & Installation

### Requirements
- **Android Version:** Android 8.0 (Oreo, API 26) or higher.
- **Architecture:** `arm64-v8a`, `armeabi-v7a`, `x86_64`.
- **Permissions:** Internet and Foreground Service Audio.

### Get the Latest APK
1. Download `STEREO-1.0.0-alpha.4.apk` from the [Latest GitHub Release](https://github.com/rishibandaru3109/STEREO/releases).
2. Open the downloaded file on your Android device.
3. If prompted, allow installation from unknown sources for your browser or file manager.
4. Launch STEREO, select your favorite music languages, and enjoy!

---

## 🛠️ Technology Stack

| Layer | Framework / Library | Purpose |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.0 | Modern concise Android development |
| **UI Toolkit** | Jetpack Compose (BOM 2024.06.00) | 100% declarative UI |
| **Design System** | Material 3 | Dynamic color tokens and adaptive theming |
| **Media Player** | AndroidX Media3 (1.3.1) / ExoPlayer | Streaming audio and system MediaSession |
| **Extraction** | InnerTubeX (v0.11.7) | YouTube Music catalog and stream resolution |
| **Database** | AndroidX Room (2.6.1) | Local persistence for history, likes, and playlists |
| **Preferences** | AndroidX DataStore (1.1.1) | Reactive preference storage for themes & languages |
| **DI** | Koin (3.5.6) | Lightweight dependency injection |
| **Image Loading** | Coil (2.6.0) | High-performance asynchronous image rendering |
| **Networking** | OkHttp (4.12.0) & Ktor (3.0.0) | Resilient HTTP streaming |

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

# Run unit tests (98 passing tests)
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```
The compiled APK will be generated at `app/build/outputs/apk/`.

---

## ⚠️ Legal & Disclaimer

- **Unofficial API:** STEREO uses community-maintained extraction libraries (InnerTubeX) to interact with public YouTube Music endpoints.
- **Stream Availability:** Extracted audio stream URLs are temporary and generated on-the-fly by content servers; pausing playback for several hours may require refreshing the stream.
- **Trademark Notice:** YouTube and YouTube Music are registered trademarks of Google LLC. STEREO is an independent open-source project and is not affiliated with, authorized, maintained, sponsored, or endorsed by Google LLC or any of its affiliates.
- **Fair Use:** This application is provided for personal, non-commercial use only. Respect your local copyright laws.

---

## 🗺️ Roadmap

- [ ] Synced, interactive lyrics display.
- [ ] Offline audio caching and downloaded track playback.
- [ ] Android Auto playback support.
- [ ] Multi-select batch playlist management.
- [ ] Sleep timer with gradual volume fade-out.

---

## 🤝 Contributing

Contributions are welcomed! Please read our [Contributing Guide](CONTRIBUTING.md) and review our [Code of Conduct](CODE_OF_CONDUCT.md).

To report bugs or suggest enhancements, please use our [GitHub Issue Templates](https://github.com/rishibandaru3109/STEREO/issues/new/choose).

---

## 📄 License & Acknowledgments

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)** — see the [LICENSE](LICENSE) file for complete details.

Special thanks to the authors and maintainers of:
- [InnerTubeX](https://github.com/MetrolistGroup/innertubex) and [QuackJS](https://github.com/MetrolistGroup/quackjs)
- [AndroidX Media3](https://github.com/androidx/media)
- [Koin](https://github.com/InsertKoinIO/koin)
- [Coil](https://github.com/coil-kt/coil)
