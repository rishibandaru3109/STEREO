# STEREO Beta 2 — Device Testing Checklist

**Build Under Test:** `release-staging\beta-2\STEREO-v1.0.0-beta.2-test.apk`  
**Package:** `com.nova.music`  
**Version:** `v1.0.0-beta.2` (versionCode `8`)  
**SHA-256:** `516CB97025EFDE74A68A499BB594A8E153860996CC28CC77E10F6ABCDFF77698`  
**Target Hardware:** Realme RMX5030 (Android 16) or any Android 8.0+ device

---

## 0. Quick Installation (Preserving User Data)

Connect your device via USB with USB Debugging enabled, then execute:

```powershell
# 1. Verify device connection
adb devices

# 2. Install update while preserving all existing user data, history, and cache
adb install -r -d "release-staging\beta-2\STEREO-v1.0.0-beta.2-test.apk"

# 3. Launch STEREO
adb shell am start -n com.nova.music/.MainActivity
```

---

## 1. Test Suite: HTTP 403 Playback Fix Verification

- [ ] **Stream Multiple Real Songs:**
  - Search and play:
    1. *Viva La Vida* — Coldplay (English)
    2. *Tum Hi Ho* — Arijit Singh (Hindi)
    3. *Samajavaragamana* — Sid Sriram (Telugu)
  - Verify audio starts within 1–2 seconds with clean waveform and no 403 Forbidden errors.
- [ ] **Mid-Track Seeking:**
  - Jump to 0:30, 1:45, 2:30. Verify playback resumes smoothly without buffering stalls or playback abortion.
- [ ] **Connection Switching & Cellular Playback:**
  - Switch from Wi-Fi to Mobile Data while playing. Verify playback does not crash or abort with HTTP 403.
- [ ] **Check Logcat for Error Absence:**
  ```powershell
  adb logcat -s STEREO_EXOPLAYER:I InnerTubeX:I STEREO_CACHE:I
  ```
  - Confirm: Zero `HTTP 403`, zero `Refreshed stream URL expired`, clean `STATE_READY` transitions.

---

## 2. Test Suite: Complete Playlist System

- [ ] **Create New Playlist:**
  - Go to **Library** tab → Tap **+ New Playlist** → Enter Name "Test Favorites" and Description.
  - Verify empty playlist screen opens with default gradient art.
- [ ] **Add Songs via Universal Bottom Sheet:**
  - From **Search**: Tap `⋮` on any song → Tap **Add to Playlist** → Select "Test Favorites". Verify checkmark appears and toast confirms addition.
  - From **Home**: Tap `⋮` on a Quick Pick track → Tap **Add to Playlist**.
  - From **Full Player**: Tap the `+` playlist icon in top bar → Select "Test Favorites".
- [ ] **Dynamic 2x2 Collage Artwork:**
  - Add 1–3 songs: verify single album cover displays.
  - Add 4+ songs with distinct covers: verify high-resolution 2x2 collage generates automatically on the playlist header and in the Library list.
- [ ] **In-Playlist Search & Sorting:**
  - Open "Test Favorites" → Use search bar: verify instant filtering of tracks.
  - Open Sort Menu: test sorting by **Title (A–Z)**, **Artist (A–Z)**, **Date Added**, and **Duration**.
- [ ] **Batch Multi-Select Actions:**
  - Tap `Checklist` icon in playlist toolbar to enter multi-select mode.
  - Select 2 songs → Tap **Add to Playlist** (copy to another playlist).
  - Select 1 song → Tap **Delete** (confirm batch removal).
- [ ] **Reordering:**
  - Tap `Sort/Reorder` icon → Use Up/Down arrows to move song position.
  - Exit reorder mode → Verify custom order is persisted in Room database across app restart.
- [ ] **Playlist Exporting:**
  - Tap `⋮` in playlist header → Tap **Export Playlist**.
  - Test export as:
    - **STEREO Backup (JSON)**
    - **CSV Spreadsheet**
    - **M3U8 Playlist**
  - Verify Android Sharesheet opens with generated export file.

---

## 3. Test Suite: Multi-Source Playlist Importing

- [ ] **Spotify Public Embed Import:**
  - Open **Library** → Tap **Import Playlist** icon.
  - Paste a public Spotify playlist URL (e.g. `https://open.spotify.com/playlist/...`).
  - Tap **Parse & Match**.
  - Verify extraction progress bar runs, displays extracted track titles, and assigns confidence badges (`EXACT` / `HIGH`).
  - Tap **Import to Library** → Verify a new playlist is created with all matched tracks.
- [ ] **YouTube / YouTube Music Playlist Import:**
  - Paste a public YouTube playlist URL (`https://music.youtube.com/playlist?list=PL...`).
  - Verify parsing succeeds and imports tracks into Room.
- [ ] **File Import (SAF):**
  - Tap **Select File** → Pick a local `.m3u8` or `.csv` playlist file.
  - Verify parser extracts track titles/artists accurately.

---

## 4. Test Suite: Persistent Offline Downloads & Airplane Mode

- [ ] **Download Single Song:**
  - Tap `⋮` on any track in Search or Home → Tap **Download**.
  - Verify toast "Downloading [Song Title]..." appears.
  - In **Full Player**: verify top download button animates to `DownloadDone` once completed.
- [ ] **Download Full Playlist:**
  - Open any playlist → Tap `⋮` in header → Tap **Download Playlist**.
  - Verify background queue starts downloading tracks sequentially (concurrency limited to 2).
- [ ] **Inspect Downloads Screen:**
  - Go to **Library** → Tap **Downloads** Hero Card.
  - Verify storage usage header displays total tracks and disk usage (e.g. "12 tracks · 48.6 MB").
  - Test tabs:
    - **Downloaded**: List of completed tracks.
    - **Queue**: Active/pending downloads with live progress bars.
    - **Failed**: Any errored downloads with Retry/Clear options.
- [ ] **Wi-Fi Only Setting Guard:**
  - Go to **Settings** → **Audio & Playback** → Toggle **Download over Wi-Fi only** ON.
  - Disconnect Wi-Fi (enable cellular data only).
  - Try downloading a new song: verify download pauses/skips with "Waiting for Wi-Fi connection".
  - Reconnect Wi-Fi: verify download proceeds to completion.
- [ ] **100% Airplane Mode Offline Playback (CRITICAL):**
  - Enable **Airplane Mode** on the device (disable Wi-Fi and Mobile Data completely).
  - Open **Downloads** screen.
  - Tap **Play All** or select any downloaded song.
  - **Expected Result:**
    - Song plays instantly (zero buffering).
    - Seeking across the track is instantaneous.
    - Media session notifications and lockscreen controls work normally.
    - No network error dialogs appear.

---

## 5. Test Suite: Data Continuity & Upgrade Integrity

- [ ] Verify existing **Liked Songs** from Beta 1 are completely preserved.
- [ ] Verify existing custom playlists from Beta 1 remain intact.
- [ ] Verify custom themes, wallpaper settings, and audio quality preferences are retained.
- [ ] Verify previously cached audio survives the update and replays without re-downloading.

---

## 6. Test Suite: SYNAPSE Autonomous Intelligence Regression

- [ ] Verify **Surprise Me** button on Home feed loads personalized mixes without freezing or crashes.
- [ ] Check that downloading 3–4 songs of a specific genre/artist noticeably boosts relevant recommendations on the Home tab after refresh.
