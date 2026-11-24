# STEREO v1.0.0-beta.2 — Playlists, Imports & Offline Downloads

STEREO Beta 2 brings a massive upgrade to your listening experience: a complete, full-fidelity **Playlist Management System**, multi-source **Playlist Importing & Exporting** (including Spotify public playlists), a persistent **Offline Downloads Hub** with airplane-mode playback, and crucial **HTTP 403 stream resolution repairs** powered by InnerTubeX VisionOS.

---

### 🌟 What's New in Beta 2

#### 1. Complete Playlist Management System
- **Comprehensive Lifecycle Controls:** Create, rename, edit descriptions, set custom artwork URLs, pin, duplicate, clear, and delete playlists with zero media loss.
- **Dynamic 2x2 Collage Covers:** Automatic quad-artwork generated from top playlist tracks (`PlaylistCollageCover`) with graceful single/multi-artwork fallbacks.
- **Interactive Track Sequencing:** Drag-and-drop / Move Up / Move Down reordering stored persistently in Room database.
- **8 Dynamic Sorting Orders:** Custom Order (saved), Recently Added, Oldest Added, Title (A-Z), Title (Z-A), Artist (A-Z), Artist (Z-A), and Duration.
- **Live In-Playlist Filtering:** Real-time fast search filter inside any playlist with instant query debouncing.
- **Queue Actions:** Play All, Shuffle Play, Play Next, and Add to Queue without player re-instantiation.

#### 2. Universal Add-to-Playlist Bottom Sheet
- **Everywhere Access:** Accessible across all app surfaces: Search results, Home shelves, Full Player, Queue, Liked Songs, and Playlist track rows.
- **In-Sheet Creation & Multi-Select:** Create new playlists directly inside the sheet and add to multiple playlists simultaneously.

#### 3. Multi-Format Playlist Import & Export
- **Multi-Source Importer:** Public Spotify playlist URLs (`open.spotify.com/playlist/...`), YouTube Music playlists, M3U/M3U8 playlists, CSV files, and STEREO Native JSON backups.
- **Smart Track Matching:** `TrackMatchingEngine` normalizes video titles, strips remaster/HD tags, matches catalog songs, and filters out covers/karaoke.
- **Multi-Format Exporter:** Export playlists to STEREO JSON, CSV, or M3U8 via the standard Android Sharesheet.

#### 4. Persistent Offline Downloads & Dedicated Downloads Screen
- **Full Track Downloads:** Save songs for verified offline playback into private app storage (`files/offline_downloads/`).
- **Dedicated Downloads Hub:** Separate tabs for Downloaded, Active Queue (with live byte & % progress), and Failed transfers.
- **Airplane-Mode Playback:** Downloaded audio plays reliably with zero active network connection.
- **Startup Recovery:** Interrupted transfers automatically resume when the app launches.
- **Wi-Fi Only Toggle:** Protect mobile data with configurable Wi-Fi only download preference in Settings.

#### 5. Streaming Stability & Responsive UI
- **HTTP 403 Permanent Fix:** InnerTubeX VisionOS client profile ensures unthrottled streaming without stream expiry or 403 Forbidden errors.
- **Responsive Layouts:** Repaired tab label wrapping on narrow phone displays (`DownloadsScreen` and `LibraryScreen`).

---

### 📥 Download & Verification

| Asset | Size | SHA-256 Checksum |
| :--- | :--- | :--- |
| **`STEREO-v1.0.0-beta.2-release.apk`** | 17.75 MB | `a280c35f348d8d6d9af06bded3248685ac7ac4e61ceeb603a98bd6768bd23c8e` |
| **`SHA256SUMS.txt`** | 100 bytes | `a280c35f348d8d6d9af06bded3248685ac7ac4e61ceeb603a98bd6768bd23c8e  STEREO-v1.0.0-beta.2-release.apk` |

#### Permanent Direct Download URL (Always Latest)
`https://github.com/rishibandaru3109/STEREO/releases/download/latest-beta/STEREO-latest.apk`

---

### 🔒 Cryptographic Signing & Compatibility
- **Release Certificate:** RSA 4096-bit
- **SHA-256 Fingerprint:** `7E:38:21:4D:3F:83:E4:FD:CC:1B:03:50:32:85:19:41:83:92:DB:E4:F6:B2:F1:AC:5F:B2:BD:03:96:D6:C0:64`
- **In-Place Upgrades:** 100% compatible with existing Alpha.4, Alpha.5, and Beta.1 installations. Your playlists, favorites, listening history, and cached audio will be preserved without data loss.
