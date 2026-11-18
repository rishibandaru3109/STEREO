# Privacy Policy for STEREO

**Last updated:** November 18, 2026

STEREO is committed to respecting and protecting user privacy. As an open-source, local-first Android music application, STEREO is designed so that your listening activity and library remain entirely under your personal control.

---

## 1. Information Collected and Processed

### Data Stored Strictly On Your Device
All of the following data is stored locally in your device's private application sandbox using Android Room (SQLite) and Android Jetpack DataStore:
- **Listening History:** Timestamps, track titles, artists, duration, and completion count.
- **Favorites & Liked Songs:** User-initiated likes.
- **Custom Playlists:** Playlist names, descriptions, and track lists.
- **Queue State:** Active queue entries and shuffle order for playback resumption.
- **Taste & Recommendation Preferences:** Selected discovery languages, genre affinities, and seed listening profiles.
- **App Theme & Visual Preferences:** Theme preset selection, custom color values, and wallpaper preferences.

This data never leaves your device and is never uploaded to any remote server or cloud service operated by STEREO.

### Network Communications
STEREO connects to the internet solely to fetch music metadata, search suggestions, recommendation feeds, and audio streams:
- **Direct YouTube Music Communications:** When you search, browse recommendations, or play a track, STEREO sends HTTP requests directly from your device to public YouTube Music endpoints (via InnerTubeX).
- **No Proxy Server:** Requests are sent directly from your device to the content delivery network; STEREO operates no intermediary servers, logs no user IP addresses, and performs no man-in-the-middle logging.
- **No User Account Required:** STEREO does not require a Google or YouTube account, does not prompt for user login, and does not store account tokens.

---

## 2. Analytics, Tracking & Advertising

- **Zero Analytics SDKs:** STEREO does not contain Firebase Analytics, Google Analytics, Sentry, Mixpanel, or any other telemetry framework.
- **Zero Ads:** STEREO contains no advertising SDKs, banner networks, or behavioral trackers.
- **Zero Crash Telemetry:** Crash logs remain in standard Android system logcat and are never automatically transmitted over the internet.

---

## 3. Permissions Used

- **`INTERNET` & `ACCESS_NETWORK_STATE`:** Required to stream music and query public music catalogs.
- **`FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK`:** Required by AndroidX Media3 to maintain audio playback when the application is backgrounded or the screen is locked.
- **`POST_NOTIFICATIONS`:** Required on Android 13+ to display media playback controls and album artwork in the system notification shade.
- **`READ_EXTERNAL_STORAGE` / `READ_MEDIA_AUDIO` / `READ_MEDIA_IMAGES`:** Optional; only requested if you choose to set custom local background wallpapers in Wallpaper Studio.

---

## 4. Third-Party Services

When streaming content, your device communicates with YouTube/Google content distribution servers. Your interaction with those endpoints is subject to Google's standard network privacy policies. STEREO does not control and is not responsible for how content hosts handle network packets.

---

## 5. User Control & Data Deletion

You can clear all application data at any time through standard Android system settings:
`Settings > Apps > STEREO > Storage & Cache > Clear Storage`.

Uninstalling STEREO completely removes all local databases, settings, and cached data from your device.

---

## 6. Contact

For privacy-related questions or audit inquiries, contact:
`rishibandaru.3109@gmail.com`
