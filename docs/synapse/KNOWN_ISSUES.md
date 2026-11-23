# STEREO × SYNAPSE — Known Issues & Active Observability Log
**Milestone 2 Status**
**Date**: November 23, 2026

---

## 1. Resolved Issues in Milestone 2

1. **[RESOLVED] Floating Surprise Me FAB Overlap**:
   - Resolved via `LazyListState.isScrollInProgress` auto-collapse into circular 48.dp icon FAB + dynamic `contentPadding` (160dp without mini-player, 230dp with mini-player). Verified in `m2_02_home_scrolled_fab_collapsed.png` and `m2_03_home_bottom_clearance.png`.
2. **[RESOLVED] Settings MMR Calibration Text Wrapping & Alignment**:
   - Resolved with `weight(1f, fill = false)` on row title, rounded badge pill for `Familiar (70%)`, and weighted anchor labels with Start/Center/End alignment. Verified in `m2_04_settings_mmr_badge.png`.
3. **[RESOLVED] Title Truncation in Quick Picks & Track Cards**:
   - Resolved with widened cards (`304.dp` for Quick Picks, `152.dp` for TrackCard), `maxLines = 2`, `minLines = 2`, and `lineHeight = 16.sp/18.sp`. Verified in `m2_01_home_top.png`.
4. **[RESOLVED] Underfilled "Because You Listened To" Rail**:
   - Resolved by eliminating playlist container consumption of feed track IDs and adding complementary high-ranked candidate backfilling. Verified in `m2_05_because_rail_lush.png`.
5. **[RESOLVED] Single-Skip Artist Banishing**:
   - Resolved by requiring $\ge 2$ consecutive skips before applying artist-level negative delta, preserving catalog eligibility for accidental skips. Verified in unit tests and on device DB.

---

## 2. Low-Priority Edge Cases & Monitoring

1. **Local SQLite File Extraction on Android 16**:
   - Direct `adb shell sqlite3` is disabled on production userdebug Android 16 without root; extraction requires binary streaming via `adb exec-out run-as com.nova.music.debug cat databases/stereo_database.db`.
2. **Candidate Cache Refresh Rate**:
   - YouTube Music candidate cache has an offline TTL of 60 minutes. Users who manually wish to refresh candidate pools immediately can pull to refresh or change Discovery Calibration in Settings.
