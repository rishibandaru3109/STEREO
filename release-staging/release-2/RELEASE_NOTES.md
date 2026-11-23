# STEREO v1.0.0-beta.1 — SYNAPSE Intelligence & Automatic Audio Caching

STEREO Release 2 is the second major milestone in the evolution of the STEREO music player. This release introduces two headline architectural capabilities: **Automatic Audio Caching** for zero-redundancy streaming replays and the **SYNAPSE Personalized Recommendation Engine**, alongside comprehensive UI refinements, scroll-aware playback controls, and seamless upgrade compatibility.

---

## 🌟 Highlights

1. **Automatic Audio Caching:** Audio streamed during regular playback is automatically cached on-device via AndroidX Media3 `SimpleCache`. Replaying a cached song reuses local byte ranges with zero network requests and enables offline listening for cached tracks.
2. **SYNAPSE Personalized Recommendations:** An original, autonomous on-device recommendation engine that learns continuously from your listening habits—boosting affinity for finished songs, adapting to session vibes, and avoiding premature artist suppression on accidental skips.

---

## 🚀 What's New

- **Automatic Audio Reuse:** Streamed audio writes contiguous byte spans to dedicated app storage (`no_backup/automatic_audio_v2`), eliminating duplicate network data transfers on repeat listens.
- **Offline Playback for Cached Tracks:** Complete cached tracks can be played seamlessly when disconnected from the internet.
- **Configurable Cache Governance:** Choose between 100 MB, 250 MB, 500 MB (default), 1 GB, or Unlimited cache quotas in Settings with automated LRU cleanup and mid-song eviction protection.
- **Personalized Music Recommendations:**
  - **Quick Picks:** Fast entry shelf curated from your highest affinity songs.
  - **Daily Mixes:** Curated personalized mixes featuring 4-quadrant dynamic album art covers.
  - **Weekly Discover:** Fresh music tailored to your vibe that you haven't heard yet.
  - **Because You Listened To [Artist]:** Lush multi-track carousels blending unplayed catalog songs with similar artists via on-device Content Similarity modeling.
- **Confidence-Weighted Skip Penalties:** Accidental single skips ($N=1$) no longer banish entire artist catalogs; artist suppression requires repeated skips ($N \ge 2$) and recovers on subsequent listens or likes.
- **Recommendation Controls (MMR):** Calibrate discovery in Settings using the Maximal Marginal Relevance slider between Adventurous (0.2), Balanced (0.65), and Familiar (0.9).
- **Fail-Safe Fallback:** Integrated circuit breaker delegates cleanly to legacy discovery if the recommendation engine is disabled or encounters an issue.

---

## 🎨 UI Improvements

- **Scroll-Aware Surprise Me FAB:** The floating action button auto-collapses to a compact 48.dp circular icon button during feed scrolling, cutting screen overlay area by ~70%, and smoothly expands back on idle.
- **Card Readability & Two-Line Title Alignment:** Shelf cards feature clean 2-line title wrapping with fixed minimum line counts, eliminating ragged carousel alignments.
- **Lush Recommendation Shelves:** "Because You Listened To" shelves now feature rich 4–8 track carousels instead of solitary cards.
- **Navigation Clearance:** Extended bottom content padding ensures all tracks, bottom shelves, and 3-dot overflow menus clear system navigation bars cleanly.
- **Settings Typography:** Refined exploration slider anchors and rounded cyan badge pills with zero text collision.

---

## 🛡️ Stability & Compatibility

- **Seamless In-Place Upgrade:** 100% compatible with existing Alpha.4 and Alpha.5 installations. Signed with the identical permanent production RSA 4096-bit certificate (`7E:38:21:4D:3F:83:E4:FD:CC:1B:03:50:32:85:19:41:83:92:DB:E4:F6:B2:F1:AC:5F:B2:BD:03:96:D6:C0:64`).
- **Automated Database Migration:** Smooth Room migration from Schema 2 to Schema 3 creating local SYNAPSE event indices while keeping existing playlists, favorites, and history intact.
- **Strict Release Quality:** Clean release build with `checkReleaseBuilds = true` and `abortOnError = true` passing with 0 lint errors, alongside 165/165 passing automated unit tests (136 in STEREO, 29 in SYNAPSE).
- **Real-Device Verified:** Extensively validated on physical Android 16 hardware (Realme RMX5030).

---

## 📥 Installation

1. Download **`STEREO-v1.0.0-beta.1-release.apk`** below.
2. Open the downloaded file on your Android device (Android 8.0+ / API 26+).
3. Confirm installation when prompted (Android may request permission to install APKs outside an app store).
4. **Existing Users:** Install directly over Alpha.4 or Alpha.5 without uninstalling; all your playlists and favorites are preserved.

---

## ⚠️ Known Limitations

- **Automatic Caching is Not Permanent Downloads:** Audio is cached opportunistically during playback for efficient replay; this is **not** an explicit song downloader.
- **Offline Playback Scope:** Offline playback requires the relevant audio to have been cached locally during prior streaming.
- **Release 3 Roadmap:** Explicit user-managed song downloads, local MP3/FLAC music playback, and a unified local library are scheduled for **Release 3**.

---

## 💬 Feedback & Bug Reports

We welcome your feedback! Please report any issues or suggest enhancements on our [GitHub Issues](https://github.com/rishibandaru3109/STEREO/issues) page.

---

## 📦 Package Information

- **Package ID:** `com.nova.music`
- **Version Name:** `1.0.0-beta.1`
- **Version Code:** `7`
- **APK File:** `STEREO-v1.0.0-beta.1-release.apk`
- **APK Size:** 18,415,227 bytes (~18.4 MB)
- **SHA-256 Checksum:** `8de248781e2e4bf810bbcd58b87291eddfd56e4e1d6c3ded251ef4b5c852eb3e`
