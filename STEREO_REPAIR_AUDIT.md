# STEREO — MASTER V2 REPAIR & FUNCTIONAL AUDIT

**Date:** November 17, 2026  
**Auditor:** Principal Android Architect & QA Lead  
**Application:** STEREO (`com.nova.music`, debug `com.nova.music.debug`)  
**Base Commit:** `978465c`  
**Target Hardware:** Realme RMX5030, Android 16  

---

## 1. AUDIT OVERVIEW & CURRENT STATUS

A comprehensive repository and functional audit was executed across the STEREO application codebase. The core music streaming engine (Media3/ExoPlayer), Room persistence, DataStore repositories, and basic UI foundations are functional. However, significant UX, navigation, launcher branding, and recommendation architecture shortcomings were identified.

---

## 2. DETAILED DEFECT INVENTORY

### Issue 1: Header Overcrowding with Redundant Technical Actions
- **Location:** `StereoTopHeader.kt`, `HomeScreen.kt`, `SearchScreen.kt`, `LibraryScreen.kt`
- **Current Behavior:** The main top header renders a reload/refresh button and a theme palette shortcut button directly in the primary browsing view.
- **Expected Behavior:** A clean, compact header (<56dp) displaying exclusively:
  1. The compact STEREO brand logo (reel-to-reel deck)
  2. Contextual greeting ("Good morning / afternoon / evening") or screen title
  3. Settings gear icon (which reliably routes to Settings)
  No reload button, no palette shortcut button, and no technical clutter. Refreshing should occur automatically or contextually via pull-to-refresh.
- **Root Cause:** Legacy development shortcuts were embedded in `StereoTopHeader` and passed from `HomeScreen`.
- **Severity:** HIGH (Degrades visual polish and confuses navigation).
- **Proposed Repair:** Remove `actions` parameter and theme button from `StereoTopHeader`. Update `HomeScreen`, `SearchScreen`, and `LibraryScreen` to pass only `onOpenSettings`. Retain manual refresh via pull-to-refresh / automatic stale-time checks in `HomeViewModel`.
- **Verification Criteria:** Visual confirmation on device that all 3 tabs display only Logo, Title/Greeting, and Settings Gear.

---

### Issue 2: Incorrect Android Launcher Icon on Physical Device
- **Location:** `AndroidManifest.xml`, `app/src/main/res/drawable/ic_stereo_launcher.xml`, `ic_stereo_launcher_foreground.xml`
- **Current Behavior:** The installed APK on Android 16 displays an outdated, generic vinyl record vector icon with an unwanted flat background. The newly supplied brand logo is visible only inside the app header, not on the phone's Home screen or app drawer.
- **Expected Behavior:** The official analog reel-to-reel audio tape deck logo must be the primary app launcher icon on the Android Home screen and App Drawer, properly framed within Android's adaptive icon safe zone (66dp circle in 108dp canvas) across all standard mipmap densities (`mipmap-mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`, `mipmap-anydpi-v26`).
- **Root Cause:** The manifest was pointing to `ic_stereo_launcher.xml`, which referenced legacy vector drawings created before the user provided the official brand reference. Mipmap directories were absent.
- **Severity:** CRITICAL (First-party brand identity broken on device launcher).
- **Proposed Repair:** Process the high-resolution brand reference (`media_1794926896416_a26503b6.jpg`), crop outer margins cleanly, position within the adaptive icon safe zone, create `mipmap-*` density directories with PNG assets, and update adaptive icon definitions.
- **Verification Criteria:** Physical launcher inspection on Realme RMX5030 showing the official reel-to-reel deck icon with circular and rounded-square adaptive masking.

---

### Issue 3: Unstructured, Monolithic Settings Screen Layout
- **Location:** `SettingsScreen.kt`, `SettingsViewModel.kt`
- **Current Behavior:** Settings is rendered as a single flat scrollable page with mixed appearance options, audio specs, taste resets, privacy switches, and cache buttons stacked vertically without clean sub-navigation.
- **Expected Behavior:** A structured, hierarchical settings architecture:
  - **Main Settings:** Clean category cards with clear titles, icons, and short descriptions:
    1. Appearance
    2. Audio & Playback
    3. Recommendations & Taste
    4. Content Preferences
    5. Library & Data
    6. Storage & Cache
    7. Privacy
    8. About
  - **Appearance Sub-Screen:**
    - A. Theme Gallery (compact previews of the 7 official themes with clear selection)
    - B. Card Styles (`Default Cards` vs `Theme Cards`, strict theme correspondence)
    - C. Player Background (Default, Album artwork, Custom image via photo picker with blur/dimming sliders)
    - D. Advanced Customization (navigation link to deep controls)
  - **Advanced Customization Sub-Screen:**
    - Batch playback control colors
    - Player surfaces & miniplayer styling
    - Effect controls with live preview
    - "Reset to Theme Defaults" (preserves active theme, resets overrides)
- **Root Cause:** All settings composables were placed sequentially inside a single vertical scroll column without sub-page routing.
- **Severity:** HIGH (Causes cramped navigation, difficult discovery of settings).
- **Proposed Repair:** Introduce sub-screen state (`SettingsDestination`) inside `SettingsScreen` or `MainActivity`, providing dedicated, beautifully formatted detail pages with predictable back navigation.
- **Verification Criteria:** Navigate through Main Settings -> Appearance -> Advanced Customization and back smoothly without glitches.

---

### Issue 4: Recommendation Engine Relies on Weak Generic YouTube Search
- **Location:** `CandidateProvider.kt`, `RecommendationEngine.kt`
- **Current Behavior:** `CatalogCandidateProvider` generates recommendation candidates solely by executing generic YouTube search queries (e.g. `"$genre music hits"`, `"$lang top songs"`). This frequently returns live concert recordings, talk shows, video memes, and generic trending songs unrelated to the listener's actual musical profile.
- **Expected Behavior:** Incorporate genuine music recommendation sources:
  1. YouTube Music `browse` (`FEmusic_home` shelves, `FEmusic_explore`) via InnerTubeX for authentic music discovery.
  2. Song Radio / Related Tracks via InnerTubeX's `next` endpoint based on user's liked and recently played songs.
  3. Local taste ranking and scoring (signal weighting: likes, repeats, completion rate, intentional skips).
  4. Strict cross-section deduplication so no song is repeated across Home sections.
- **Root Cause:** `CandidateProvider` was implemented as a mock/stub that only executed basic text searches rather than using InnerTubeX's available browse and next endpoints.
- **Severity:** CRITICAL (Directly degrades music listening experience).
- **Proposed Repair:** Build a YouTube Music-first recommendation pipeline that fetches related tracks for top user seeds using `innerTube.next` and home shelves via `innerTube.browse`, merging them with local signal weights and deduplication.
- **Verification Criteria:** Evaluation against synthetic taste profiles (English pop, Telugu film, Electronic/EDM) verifying high relevance and zero generic video filler.

---

### Issue 5: Missing Music-Only Content Filtering
- **Location:** `CandidateProvider.kt`, `InnerTubeXExtractor.kt`, `SearchViewModel.kt`
- **Current Behavior:** Queries on InnerTubeX / YouTube return nonmusic content (interviews, podcasts, reactions, vlogs, audiobooks, tutorials) alongside music tracks.
- **Expected Behavior:** Default to **Music Only** filtering across Search, Home, and Recommendations:
  - Keep: Official songs, albums, singles, music videos, remixes, covers, legitimate DJ mixes.
  - Suppress: Podcasts, interviews, reaction videos, commentary, vlogs, audiobooks, spoken word.
  - Add user setting: `Settings → Content Preferences → Content Visibility` (`Music Only` [default] vs `All Content`).
- **Root Cause:** No content filtering or classification logic was applied to incoming candidate items or search results.
- **Severity:** HIGH (Contaminates music player with video/spoken-word content).
- **Proposed Repair:** Implement `ContentFilter` with classification heuristics (renderer types, title/channel heuristics, duration boundaries, and music metadata tags), persisted via DataStore with a toggle in Content Preferences.
- **Verification Criteria:** Test queries for famous artists to confirm nonmusic interviews/reactions are filtered out in Music Only mode and accessible in All Content mode.

---

## 3. CHECKPOINT CONFIRMATION

- Clean Git checkpoint established at commit `978465c`.
- Working playback, themes, database, and settings data verified intact.
- Progression order: Header correction → Settings restructuring → Launcher icon repair → Metrolist/InnerTubeX research → YouTube Music recommendation pipeline → Music-only filtering → Device validation.
