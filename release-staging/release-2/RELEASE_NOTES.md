# STEREO v1.0.0-beta.1 (Release 2) — Release Notes

STEREO Release 2 introduces two major architectural capabilities to the music player: **Automatic Audio Caching** and the **SYNAPSE Personalized Recommendation Engine**, alongside comprehensive UI refinements, scroll-aware playback controls, and complete data continuity.

---

## 🚀 Headline Features

### 1. Automatic Audio Caching
- **Seamless Streaming Replay:** Audio received during normal playback is automatically cached on-device in `no_backup/automatic_audio_v2` using AndroidX Media3's `SimpleCache` architecture.
- **Zero Redundant Data Transfers:** Replaying a cached song reuses local byte ranges immediately with zero network requests.
- **Offline Playback for Validated Tracks:** Fully cached audio tracks can be played without an active internet connection.
- **Configurable Storage Limits:** Choose between 100 MB, 250 MB, 500 MB (default), 1 GB, or Unlimited directly from Settings. Least-recently-used eviction automatically reclaims space when limits are reached while strictly preserving currently playing audio.

### 2. SYNAPSE Autonomous Recommendation Engine
- **Behavioral Intelligence:** Learns dynamically from your real listening habits: tracks played to completion increase artist affinity, while accidental or mood-mismatch skips are weighted with confidence to prevent premature artist banishing.
- **Dual-Horizon Taste Modeling:** Separates short-term session vibes from long-term taste centroids so a casual workout session doesn't overwrite your favorite genres.
- **Dynamic Rails:**
  - **Quick Picks:** Start listening instantly with tracks chosen by SYNAPSE adaptive intelligence.
  - **Daily Mixes:** Curated personalized mixes built from your top affinity clusters.
  - **Weekly Discover:** Fresh music tailored to your vibe that you haven't heard yet.
  - **Because You Listened To [Artist]:** Lush carousels blending unplayed catalog tracks with similar artists via on-device Content Similarity modeling.
- **Maximal Marginal Relevance (MMR):** Calibrate your discovery preference in Settings between Adventurous (0.2), Balanced (0.65), and Familiar (0.9).

---

## 🎨 UI & Ergonomic Improvements

- **Scroll-Aware Surprise Me Button:** The floating action button automatically collapses into a compact 48.dp icon button during feed scrolling, preventing content occlusion, and expands back into the labeled pill when idle.
- **Two-Line Title Alignment:** Card titles wrap cleanly across two lines with uniform heights, eliminating ragged horizontal carousel layouts.
- **Dynamic Navigation Clearance:** Added comprehensive bottom content padding (160.dp / 230.dp with mini-player) ensuring all bottom tracks and 3-dot menus remain fully reachable above the navigation bar.
- **Refined Settings Typography:** Beautiful rounded badge pills and evenly spaced calibration anchors.

---

## 🔒 Security, Privacy & Compatibility

- **Seamless Upgrade:** 100% compatible with existing Alpha.4 and Alpha.5 installations. Signed with the identical permanent production RSA 4096-bit certificate (`7E:38:21:4D:3F:83:E4:FD:CC:1B:03:50:32:85:19:41:83:92:DB:E4:F6:B2:F1:AC:5F:B2:BD:03:96:D6:C0:64`). Existing playlists, favorites, and history are preserved without data loss.
- **Automated Database Migration:** Smooth Room migration from Schema 2 to Schema 3 creating local SYNAPSE event indices.
- **Zero Trackers:** No telemetry, no external accounts, no cloud dependencies, and ₹0 infrastructure footprint.

---

## 📦 Package Information

- **Package ID:** `com.nova.music`
- **Version Name:** `1.0.0-beta.1`
- **Version Code:** `7`
- **APK File:** `STEREO-v1.0.0-beta.1-release.apk`
- **APK Size:** 18,415,227 bytes
- **SHA-256 Checksum:** `8de248781e2e4bf810bbcd58b87291eddfd56e4e1d6c3ded251ef4b5c852eb3e`
