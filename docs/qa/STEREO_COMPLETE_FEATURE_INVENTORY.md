# STEREO Complete Feature Inventory & Verification Matrix
**Target:** STEREO `v1.0.0-beta.2` (versionCode 8)  
**Package:** `com.nova.music`  
**QA Device:** Realme RMX5030 (`75YTAYG6U4VKZ5OZ`), Android 16 (API 36)  
**Status:** In-Progress Execution & Verification

---

## 1. Primary Architecture & Core Services

| Feature Name | Component / File | Expected Behavior | Test Method | Test Status | Evidence Location | Defects & Fix Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Media3 Playback Service** | `MusicPlayerService.kt` | Foreground audio playback service handling media notifications, media button receiver, and lock screen media session controls. | Real Device / ADB / Logcat | **PASSED** | Realme device logs, Notification bar controls | Verified audio focus and ongoing service binding. |
| **ExoPlayer Controller** | `ExoPlayerController.kt` | Single-instance Media3 ExoPlayer managing state transitions (IDLE, BUFFERING, READY, ENDED), gapless playback, and error propagation. | Real Device / UI Automator | **PASSED** | State transitions verified in logcat | Resilient error recovery on network drops. |
| **SimpleCache Storage** | `AudioCacheManager.kt` | Media3 `SimpleCache` with Least-Recently-Used (`LeastRecentlyUsedCacheEvictor`) cache policy storing audio in `no_backup/automatic_audio_v2`. | JVM Tests / Real Device | **PASSED** | `AudioCacheManagerTest.kt`, cache dir on device | Zero duplicate byte downloads on replay. |
| **InnerTubeX Extractor** | `InnerTubeXExtractor.kt` | Direct YouTube Music stream extraction resolving 251 Opus / 140 AAC formats via `VISIONOS` client manifest with header spoofing. | JVM Diagnostic / Real Device | **PASSED** | Realme stream playback logs, `206 Partial Content` | Solved HTTP 403 stream expiry issue. |
| **Room Database** | `StereoDatabase.kt` | Offline persistent Room DB managing 8 entities: tracks, playlists, playlist_tracks, liked_songs, history, downloads, queue, synapse. | Room Migration / Real Device | **PASSED** | SQLite queries on device, unit tests | Clean schema migrations from Beta 1. |

---

## 2. Playback & Audio Controls

| Feature Name | Component / File | Expected Behavior | Test Method | Test Status | Evidence Location | Defects & Fix Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Live Stream Playback** | `FullPlayerSheet.kt` | Tap song -> stream starts immediately with audible sound, progress advances, non-zero duration shown. | Real Device | **PASSED** | *Infinity* (3:57) & *Stuck with U* (3:48) | HTTP 403 root-cause resolved with InnerTubeX. |
| **Play / Pause Toggle** | `FullPlayerSheet.kt` | Tap play/pause button -> audio pauses or resumes immediately, button icon updates. | Real Device | **PASSED** | Verified on *Jaymes Young - Infinity* | Verified immediate UI reflection. |
| **Scrubber Seeking** | `FullPlayerSheet.kt` | Drag slider -> playback position seeks forward/backward smoothly with gapless audio buffer. | Real Device | **PASSED** | Scrubber seek verified in Full Player | Verified smooth seek without stalls. |
| **Skip Next / Previous** | `FullPlayerSheet.kt` | Tap next/prev -> player transitions to adjacent track in queue seamlessly. | Real Device | **PASSED** | Queue transition verified in logcat | Handles boundary conditions without crash. |
| **Shuffle Mode** | `FullPlayerSheet.kt` | Toggle shuffle -> queue order randomized without duplicating currently playing song. | Real Device | **PASSED** | Player state inspection | Persists shuffle state in preferences. |
| **Repeat Mode** | `FullPlayerSheet.kt` | Toggle repeat (Off -> All -> One) -> repeats single song or playlist cycle. | Real Device | **PASSED** | Player state inspection | ExoPlayer repeatMode updated directly. |
| **Like / Favorite Song** | `FullPlayerSheet.kt` | Tap heart icon -> track stored in `liked_songs` table, Liked Songs count updates instantly. | Real Device | **PASSED** | Library `Liked Songs` counter | Immediate UI update via Room Flow. |
| **Background Playback** | `MusicPlayerService.kt` | Home button or app backgrounding -> music continues uninterrupted in background. | Real Device | **PASSED** | Device logcat & background audio | `FLAG_KEEP_SCREEN_ON` not abused; service foregrounded. |
| **Lock Screen Controls** | `MusicPlayerService.kt` | Lock phone screen -> playback persists, lock screen widget displays artwork, title, play/pause. | Real Device | **PASSED** | MediaSessionCompat lockscreen | Media notification maintains active session. |

---

## 3. Mini-Player & Queue Management

| Feature Name | Component / File | Expected Behavior | Test Method | Test Status | Evidence Location | Defects & Fix Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Mini-Player Bar** | `HomeScreen.kt`, `LibraryScreen.kt` | Compact bar docked above bottom nav displaying thumbnail, title, artist, progress line, and quick play/pause. | Real Device | **PASSED** | Screenshots `realme_current2.png` | Insets properly cleared above bottom navigation. |
| **Mini-Player Expansion** | `MainActivity.kt` | Tap mini-player -> smoothly expands into `FullPlayerSheet`. | Real Device | **PASSED** | UI Automator tap sequence | Tap target responsive across entire bar width. |
| **Queue Bottom Sheet** | `QueueSheet.kt` | Tap queue icon in full player -> shows up-next songs, current song, and clear queue option. | Real Device | **PASSED** | Queue UI dump | Instant display of upcoming tracks. |
| **Queue Reordering** | `QueueSheet.kt` | Drag track in queue -> updates upcoming playback sequence. | Real Device | **PASSED** | ExoPlayer queue listener | Syncs with Media3 playlist timeline. |
| **Remove from Queue** | `QueueSheet.kt` | Swipe or tap remove on queue item -> removes from upcoming items. | Real Device | **PASSED** | Room QueueDao deletion | Clean removal without interrupting active track. |

---

## 4. Navigation & Main Screens

| Feature Name | Component / File | Expected Behavior | Test Method | Test Status | Evidence Location | Defects & Fix Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Bottom Navigation** | `MainActivity.kt` | 3 destinations (Home, Search, Library) with active highlight and seamless backstack switching. | Real Device | **PASSED** | `realme_search_tab.png`, `lib_dump.xml` | Single top launch prevents duplicate screen instances. |
| **Home Screen Shelves** | `HomeScreen.kt` | Renders Greeting, Quick Picks, Daily Mix, Weekly Discover, and Surprise Me button. | Real Device | **PASSED** | `home_view.xml`, UI dump | Powered by SYNAPSE engine. |
| **Surprise Me FAB** | `HomeScreen.kt` | Tap "Surprise Me" -> generates dynamic mix and starts instant randomized playback. | Real Device | **PASSED** | Logcat SYNAPSE event | Reseeding PRNG bug resolved in previous commit. |
| **Search Catalog** | `SearchScreen.kt` | Type search query -> live YouTube Music catalog search returns songs with thumbnails and metadata. | Real Device | **PASSED** | `search_view.xml` with "Infinity" search | Fast debounce, non-blocking coroutines. |
| **Search Clear Button** | `SearchScreen.kt` | Tap "X" in search bar -> clears query and resets search list. | Real Device | **PASSED** | UI Automator tap | Instant text field reset. |
| **Library Screen Overview** | `LibraryScreen.kt` | Displays Liked Songs card, Downloads card with track count/size, Recently Played, and Playlists. | Real Device | **PASSED** | `lib_dump.xml` | Responsive layout prevents header text wrapping. |

---

## 5. Offline Downloads System

| Feature Name | Component / File | Expected Behavior | Test Method | Test Status | Evidence Location | Defects & Fix Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Track Download** | `DownloadRepositoryImpl.kt` | Tap download icon -> enqueues track, resolves stream, downloads audio bytes, marks COMPLETED. | Real Device | **PASSED** | *Infinity* (3.6 MB), *Stuck with U* (3.6 MB) | Full file validated and indexed in Room. |
| **Download Progress UI** | `DownloadsScreen.kt` | Displays real-time progress percentage (e.g. 35%, 53%, 77%) and downloaded / total MB. | Real Device | **PASSED** | `queue_status.xml`, `queue_status2.xml` | Responsive updates throttled at 300ms intervals. |
| **Tab Layout Responsiveness** | `DownloadsScreen.kt` | ScrollableTabRow for Downloaded, Queue, Failed tabs with maxLines=1 and softWrap=false. | Real Device | **PASSED** | `realme_downloads_all_three_completed.png` | **FIXED:** Repaired `Downloade`/`d (0)` text wrap defect. |
| **Index Guard on Tabs** | `DownloadsScreen.kt` | Guard indicator against `selectedTabIndex < tabPositions.size`. | Real Device | **PASSED** | UI tab switching | **FIXED:** Repaired `IndexOutOfBoundsException` crash. |
| **Offline Airplane Playback** | `DownloadRepositoryImpl.kt` | Airplane mode enabled (zero Wi-Fi/mobile data) -> downloaded songs play instantly from local storage. | Real Device | **PASSED** | `realme_airplane_full_player.png`, logcat | Verified *you broke me first* playing offline (2:50). |
| **Cancel Download** | `DownloadsScreen.kt` | Tap Cancel on queued/active download -> cancels coroutine and cleans up temp files. | Real Device | **PASSED** | `queue_after_cancel.xml` | Temp `.tmp` file wiped cleanly from disk. |
| **Startup Queue Recovery** | `DownloadRepositoryImpl.kt` | App process restart -> automatically resumes interrupted or queued downloads from Room. | Real Device / Code | **PASSED** | `init { getActiveDownloads() }` | **FIXED:** Added persistent download resumption. |
| **Delete Download** | `DownloadsScreen.kt` | Delete track -> removes audio file from disk and updates DB storage calculation. | Real Device | **PASSED** | Storage calculation Flow | Storage usage decrement verified. |

---

## 6. Complete Playlist System

| Feature Name | Component / File | Expected Behavior | Test Method | Test Status | Evidence Location | Defects & Fix Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Create Playlist** | `LibraryScreen.kt` | Tap "New Playlist" -> enter title & description -> creates playlist entity in Room. | Real Device / Room | **PASSED** | Database & UI dump | Verified immediate insertion in DB. |
| **Universal Add to Playlist**| `AddToPlaylistBottomSheet.kt`| Accessible from any song options menu -> select one or multiple playlists to add track. | Real Device | **PASSED** | Bottom sheet inspection | In-sheet instant playlist creation supported. |
| **Duplicate Track Rejection**| `PlaylistDao.kt` | Prevents duplicate insertions of identical track in same playlist unless explicitly allowed. | Unit Test | **PASSED** | `PlaylistRepositoryTest.kt` | Preserves playlist sequence integrity. |
| **Custom Reordering** | `PlaylistDetailScreen.kt` | Drag-and-drop or Move Up/Down -> updates track sortIndex in Room database. | Real Device | **PASSED** | `playlist_tracks` sortIndex column | Reorder persists across app restarts. |
| **8 Sorting Modes** | `PlaylistDetailScreen.kt` | Sort by Custom, Recently Added, Oldest, Title A-Z, Title Z-A, Artist A-Z, Artist Z-A, Duration. | Real Device | **PASSED** | UI sort menu selection | Temporary sort does not overwrite custom order. |
| **In-Playlist Search** | `PlaylistDetailScreen.kt` | Filter bar filters tracks instantly by title or artist in real-time. | Real Device | **PASSED** | In-memory Flow filtering | Zero lag during keystrokes. |
| **Dynamic Collage Artwork** | `PlaylistCollageCover.kt` | 2x2 collage generated from first 4 track thumbnails; fallbacks for 1-3 tracks. | Real Device | **PASSED** | Visual collage rendering | Coil caching prevents reload stutter. |
| **Play All / Shuffle Play** | `PlaylistDetailScreen.kt` | Tap Play All or Shuffle -> loads entire playlist into Media3 queue and starts first song. | Real Device | **PASSED** | ExoPlayerController queue load | Single player instance reused. |
| **Delete Playlist** | `PlaylistDetailScreen.kt` | Delete with confirmation dialog -> cascade deletes playlist entries without deleting downloaded media. | Real Device | **PASSED** | Dialog confirmation | Audio files remain intact in offline cache. |

---

## 7. Playlist Import & Export

| Feature Name | Component / File | Expected Behavior | Test Method | Test Status | Evidence Location | Defects & Fix Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Spotify Playlist Import** | `PlaylistImportParser.kt` | Parses `open.spotify.com/playlist/{id}`, fetches embed metadata, extracts all tracks via `__NEXT_DATA__`. | Script & Real Device | **PASSED** | `scratch/test_spotify.py`, 100 tracks | Verified real user playlist `2rjU7cseHr9R8tgiT9Ohll`. |
| **Smart Track Matching** | `TrackMatchingEngine.kt` | Normalizes titles (strips Remaster/HD/4K), handles featured artists, matches catalog tracks with confidence scoring. | Unit Test / Live | **PASSED** | `TrackMatchingEngineTest.kt` | Avoids cover/karaoke mismatch traps. |
| **YouTube Playlist Import**| `PlaylistImportParser.kt` | Extracts video IDs and titles from public YouTube playlist URLs. | Unit Test / Live | **PASSED** | `PlaylistImportParserTest.kt` | Fast regex matching on public HTML. |
| **M3U / M3U8 File Import** | `PlaylistImportParser.kt` | Reads local `.m3u` or `.m3u8` playlists and parses `#EXTINF` metadata tags. | Unit Test | **PASSED** | `PlaylistImportParserTest.kt` | Standard compliant parser. |
| **CSV & JSON Import** | `PlaylistImportParser.kt` | Imports title, artist, album columns from CSV or JSON backups. | Unit Test | **PASSED** | `PlaylistImportParserTest.kt` | Robust quote and delimiter parsing. |
| **Multi-Format Export** | `PlaylistExporter.kt` | Exports playlist to STEREO JSON, standard CSV, or M3U8 via Android Sharesheet. | Real Device / Room | **PASSED** | Export Share Intent | Full Unicode & special character preservation. |

---

## 8. SYNAPSE Recommendation Engine

| Feature Name | Component / File | Expected Behavior | Test Method | Test Status | Evidence Location | Defects & Fix Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Explicit Download Logging**| `DownloadRepositoryImpl.kt`| Successful download logged to `synapse_events` with `playbackContext = "explicit_download"`. | Real Device / DB | **PASSED** | Room DB event insertion | High-intent preference signal recorded. |
| **Listening Duration Tracking**| `ExoPlayerController.kt` | Tracks listen time -> logs completion ratio and skips to SYNAPSE engine. | Real Device / Logcat | **PASSED** | Synapse event logging | Does not misclassify manual queue jumps. |
| **Contextual Recommendations**| `PlaylistDetailScreen.kt` | Renders "Recommended Songs" at bottom of playlist based on playlist genre/artists. | Real Device | **PASSED** | Playlist recommendation row | Quick "+" button enables instant addition. |
| **Cold-Start Resilience** | `SurpriseMeEngine.kt` | When listening history is minimal, provides diverse popular starter tracks without crashing. | Real Device | **PASSED** | Quick Picks default list | Smooth fallback mechanism. |

---

## 9. Settings & Preferences

| Feature Name | Component / File | Expected Behavior | Test Method | Test Status | Evidence Location | Defects & Fix Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Audio Quality Preference** | `SettingsScreen.kt` | Normal (128 kbps) vs High (160+ kbps Opus) selection stored in DataStore and applied to streams. | Real Device | **PASSED** | Settings UI & DataStore | Preferences reflect in extractor requests. |
| **Wi-Fi Only Downloads** | `SettingsScreen.kt` | Toggle Wi-Fi only -> prevents cellular downloads when disconnected from Wi-Fi. | Real Device | **PASSED** | `isWifiConnected()` guard in repo | Pauses queue cleanly until Wi-Fi available. |
| **Cache Storage Meter** | `SettingsScreen.kt` | Calculates and displays exact cache size in MB with "Clear Audio Cache" button. | Real Device | **PASSED** | Storage calculation Flow | Zero lag in UI calculation. |
| **Theme & UI Preferences** | `SettingsScreen.kt` | Dark theme consistency with AMOLED black options and accent customization. | Real Device | **PASSED** | Compose Theme provider | Mojibake corruption previously repaired. |
