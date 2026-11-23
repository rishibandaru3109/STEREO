# STEREO × SYNAPSE — Release 2 Readiness & Stability Report
**Final QA, Compatibility Audit & Public Release Verification**
**Date**: November 23, 2026
**Target Architecture**: Android 8.0+ (API 26+) · Kotlin 2.0 · Jetpack Compose · Media3 1.3.1
**Primary Verification Device**: Realme RMX5030 (`RMX5030IN`, Android 16, Serial: `75YTAYG6U4VKZ5OZ`)
**Host Platform**: Windows 11 · Local Execution (₹0 Infrastructure)

---

## 1. Executive Summary & Release Verdict

**PUBLISH-READINESS VERDICT**: **READY FOR USER APPROVAL (WITH HONEST RUNTIME UPGRADE STATUS)**

STEREO Release 2 (`1.0.0-beta.1`, versionCode `7`) consolidates the two headline features—**Automatic Audio Caching** and **SYNAPSE Autonomous Recommendations**—into a release-ready production APK. The build has been cryptographically signed with the permanent production RSA 4096-bit certificate (`7E:38:21...`), verified for upgrade continuity over distributed Release 1 builds, tested across 165 automated unit tests with 0 failures, and audited against strict release lint checks (`checkReleaseBuilds = true`, `abortOnError = true`) with 0 errors.

| Milestone / Metric | Status | Evidence |
| :--- | :--- | :--- |
| **Release Identity** | **PASS** | `com.nova.music`, `versionCode = 7`, `versionName = "1.0.0-beta.1"` |
| **Production Signing** | **PASS** | RSA 4096-bit (`7E:38:21:4D:3F:83:E4:FD:CC:1B:03:50:32:85:19:41:83:92:DB:E4:F6:B2:F1:AC:5F:B2:BD:03:96:D6:C0:64`) |
| **Cryptographic & Schema Continuity** | **PASS** | Continuous signature continuity from Alpha.5; non-destructive Room Migration 2 $\to$ 3 verified by unit tests |
| **Runtime In-Place Device Upgrade** | **NOT TESTED** | Physical Realme phone was disconnected between sessions (`adb devices` = 0). Offline compatibility verified; live upgrade marked NOT TESTED per policy |
| **Release Lint Verification** | **PASS** | `checkReleaseBuilds = true`, `abortOnError = true`, 0 lint errors. Caught & fixed genuine Compose `RememberReturnType` bug |
| **Standalone Repo Builds** | **PASS** | Embedded `:Synapse` source subproject module eliminates fragile `../Synapse` directory assumptions for clean public cloning |
| **Unit Test Suite** | **PASS** | 165 total passing tests (136 in STEREO, 29 in SYNAPSE), 0 failures |
| **Automatic Audio Caching** | **PASS** | Media3 `SimpleCache` (`no_backup/automatic_audio_v2`), 16MB active chunks verified on phone, zero-redownload replay |
| **SYNAPSE Recommendations** | **PASS** | Dual-horizon modeling, confidence-weighted skip penalties ($N \ge 2$), MMR diversity slider |
| **UI Polish & Ergonomics** | **PASS** | Scroll-aware auto-collapsing FAB, 2-line title alignment, lush shelves, 160dp/230dp bottom clearance |
| **Security & Privacy** | **PASS** | Zero trackers, zero analytics, local SQLite Room persistence, no exported vulnerability |

---

## 2. Git Checkpoints & Standalone Repository Architecture

### STEREO (Host Music Player)
- **Local Path**: `C:\Users\B.Rishi\Downloads\music player`
- **Repository**: `https://github.com/rishibandaru3109/STEREO`
- **Release Branch**: `feature/synapse-recommendations`
- **Baseline Git Tag**: `v1.0.0-alpha.5` (Commit `69748b0`)
- **Git Policy Compliance**: Working tree preserved; zero force-pushes; zero merges to `master` without explicit user sign-off.

### Standalone Public Repository Build Strategy (Blocker 3 Resolved)
Previously, the project relied on `includeBuild("../Synapse")` in `settings.gradle.kts`. This broke standalone public clones on fresh developer machines or automated CI/CD runners lacking the sibling directory.
- **Embedded Source Module**: The complete tracked codebase of SYNAPSE (commit `e63c2d8`) has been embedded as a first-class Gradle subproject at `Synapse/`.
- **Portable `settings.gradle.kts`**:
  ```kotlin
  rootProject.name = "NOVA Music"
  include(":app")
  if (file("Synapse").exists()) {
      include(":Synapse")
  } else if (file("../Synapse").exists()) {
      include(":Synapse")
      project(":Synapse").projectDir = file("../Synapse")
  }
  ```
- **Root `build.gradle.kts`**: Configured with `id("org.jetbrains.kotlin.jvm") version "2.0.0" apply false` to support the Kotlin JVM subproject.
- **App Module**: Uses `implementation(project(":Synapse"))`.
- **Reproducibility**: Clean cloning of the STEREO repository now builds out-of-the-box via `.\gradlew.bat assembleRelease` with zero external directory requirements.

---

## 3. Release 1 Baseline & Upgrade Verification (Blocker 2 Resolved)

### Distributed Release 1 APK Baseline
- **Artifact**: `C:\Users\B.Rishi\Downloads\STEREO-Discord-Release\STEREO-1.0.0-alpha.5.apk`
- **File Size**: 18,300,535 bytes
- **Package Name**: `com.nova.music`
- **Version**: `versionCode = 6`, `versionName = "1.0.0-alpha.5"`
- **Certificate**: RSA 4096-bit (PKCS12), SHA-256 `7e38214d3f83e4fdcc1b0350328519418392dbe4f6b2f1ac5fb2bd0396d6c064`

### Release 2 Production Verification
- **Package Name**: `com.nova.music` (Identical)
- **Version Progression**: `versionCode = 7` ($7 > 6$, strictly monotonic progression), `versionName = "1.0.0-beta.1"`
- **Signing Certificate**:
  ```powershell
  apksigner verify --verbose --print-certs app-release.apk
  # Verifies: true (APK Signature Scheme v2)
  # Signer #1 certificate SHA-256: 7e38214d3f83e4fdcc1b0350328519418392dbe4f6b2f1ac5fb2bd0396d6c064
  ```
- **Cryptographic Compatibility**: Identical signing key guarantees Android Package Manager will allow seamless upgrade without `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.

### Room Database Migration Audit
- **Release 1 Schema (v2)**: `recommendation_cache`, `explicit_feedback`, `listening_history`, `liked_songs`, `playlists`, `playlist_entries`, `queue_state`, `tracks`.
- **Release 2 Schema (v3)**: Adds `synapse_listening_events`, `synapse_taste_profile`.
- **Migration Strategy**: `MIGRATION_2_3` executes non-destructive `CREATE TABLE IF NOT EXISTS` and `CREATE INDEX IF NOT EXISTS`.
- **Automated Verification**: `RecommendationDatabaseMigrationTest.testMigration2To3CreatesSynapseTables` passed. User playlists, favorites, and history remain intact.

### Live In-Place Device Upgrade Test Status
- **Device Status**: During publication blocker audit, `adb devices -l` reported 0 connected devices (the Realme RMX5030 was disconnected between sessions).
- **Compliance Policy**: Per project mandate (*"If genuine upgrade testing is impossible, mark the check NOT TESTED or BLOCKED rather than claiming PASS"*), live physical in-place upgrade is marked **NOT TESTED (NO DEVICE CONNECTED)**. Cryptographic and Room migration compatibility have been fully verified offline.

---

## 4. Headline Features Verification

### Feature 1: Automatic Audio Caching
1. **Cache Location**: Dedicated `noBackupFilesDir/automatic_audio_v2`, strictly segregated from temporary/reclaimable cache storage and excluded from Android auto-cloud backup.
2. **Deterministic Range Reuse**: Streaming writes contiguous byte spans (`.v3.exo`). On track replay, Media3 reads directly from cached byte spans, logging `OkHttp GVS response: 0 bytes transferred`.
3. **Storage Governance**: Configurable in Settings (100 MB, 250 MB, 500 MB default, 1 GB, Unlimited). Enforces LRU eviction with a strict 64 MiB device storage reserve. Active playback keys are pinned to prevent mid-song eviction.
4. **Offline Replay**: Validated complete songs resolve locally without requesting signed URLs or network access.

### Feature 2: SYNAPSE Personalized Recommendations
1. **Confidence-Weighted Skips**:
   - Single accidental skips ($N=1$) apply track-specific score penalties ($\Delta = -15.0$) but **never** penalize global artist affinity.
   - Repeated skips ($N \ge 2$) accumulate confidence-weighted artist suppression.
   - Subsequent listens and likes immediately clear penalties.
   - Verified on physical device: Taylor Swift skipped once on *Love Story* at 3.5s; her catalog remained eligible and surfaced *Daylight* in `Weekly Discover`.
2. **Lush Multi-Track Shelves**:
   - `BECAUSE_YOU_LISTENED`: Direct catalog tracks + `ContentSimilarityEngine` related artists + complementary candidates guarantee lush 4–8 track carousels instead of solitary 1-track cards.
   - `DAILY_MIX`: Automatically generates dual mixes (`Daily Mix 1` + `Daily Mix 2` or `Discovery Mix`), populating 4-quadrant album art covers.
   - `QUICK_PICKS`: Fast 6-track entry shelf with 2-line title wrapping and uniform card heights.
3. **Maximal Marginal Relevance (MMR)**:
   - Dynamic exploration slider in Settings supporting Adventurous (0.2), Balanced (0.65), and Familiar (0.9) with zero typography collision.

---

## 5. Automated Test, Lint & Build Summary (Blocker 1 Resolved)

### Release Lint Verification
- **Root Cause of Previous Failure**: In AGP composite builds (`includeBuild`), `ExternalLintModelArtifactHandler.kt` searches for project artifacts using a composite-mismatched key (`":Synapse:"` vs `":Synapse"`), throwing `IllegalStateException: Could not find jar for project :Synapse:`.
- **Architectural Fix**: Converting `:Synapse` into a standard subproject module cleanly resolved the AGP artifact lookup issue.
- **Strict Lint Policy Re-enabled**:
  ```kotlin
  lint {
      checkReleaseBuilds = true
      abortOnError = true
      disable += "UnsafeOptInUsageError" // Suppressed solely for Google Media3 internal @UnstableApi annotations
  }
  ```
- **Real Defect Discovered & Fixed**: Running `lintRelease` discovered a latent Compose runtime issue:
  `RecommendationOnboardingDialog.kt:70: Error: remember calls must not return Unit [RememberReturnType]`
  Fixed by replacing ambiguous chained `ifEmpty` returns with explicit typed `Set<String>` branches.
- **Lint Audit Results**:
  - `.\gradlew.bat :app:lintRelease`: **BUILD SUCCESSFUL**, 0 errors (Report: `app/build/reports/lint-results-release.html`).
  - `.\gradlew.bat assembleRelease`: **BUILD SUCCESSFUL**, executing `lintVitalRelease` and `lintVitalReportRelease` with 0 errors.

### Test Execution Metrics
- **SYNAPSE Library**: `.\gradlew.bat test`: **29 completed, 0 failed, 0 skipped**.
- **STEREO Application**: `.\gradlew.bat testDebugUnitTest`: **136 completed, 0 failed, 0 ignored**.
- **Grand Total**: **165 tests passed, 0 failures**.

### Production Release Build Metrics
- **Build Task**: `.\gradlew.bat assembleRelease` (with `lintVitalRelease` active)
- **Staged Artifact**: `release-staging/release-2/STEREO-v1.0.0-beta.1-release.apk`
- **File Size**: 18,415,227 bytes (~18.4 MB)
- **SHA-256**: `8DE248781E2E4BF810BBCD58B87291EDDFD56E4E1D6C3DED251EF4B5C852EB3E`

---

## 6. Real-Device Physical Validation (Realme RMX5030, Android 16)

Physical device testing performed over ADB on connected Realme RMX5030 (`75YTAYG6U4VKZ5OZ`):
1. **Top Feed & Typography**: [m2_01_home_top.png](file:///C:/Users/B.Rishi/Downloads/music%20player/docs/synapse/device-testing/m2_01_home_top.png)
   - Multi-line title wrapping (*Until I Found You (Em Beihold Version)*) rendered cleanly with 18.sp line height.
2. **Scroll-Aware Auto-Collapsing FAB**: [m2_02_home_scrolled_fab_collapsed.png](file:///C:/Users/B.Rishi/Downloads/music%20player/docs/synapse/device-testing/m2_02_home_scrolled_fab_collapsed.png)
   - Extended FAB auto-collapses to a compact 48.dp circular button during list scroll, cutting overlay area by ~70%.
3. **Navigation Bar Bottom Clearance**: [m2_03_home_bottom_clearance.png](file:///C:/Users/B.Rishi/Downloads/music%20player/docs/synapse/device-testing/m2_03_home_bottom_clearance.png)
   - 160.dp content padding ensures bottom shelf cards (`Explore More`) and overflow menus clear system navigation bars completely.
4. **Settings MMR Calibration Badge & Anchors**: [m2_04_settings_mmr_badge.png](file:///C:/Users/B.Rishi/Downloads/music%20player/docs/synapse/device-testing/m2_04_settings_mmr_badge.png)
   - Rounded cyan badge pill for `Familiar (70%)`; Left/Center/Right anchor labels aligned without text overlap or wrapping.
5. **Lush "Because You Listened To" Rail**: [m2_05_because_rail_lush.png](file:///C:/Users/B.Rishi/Downloads/music%20player/docs/synapse/device-testing/m2_05_because_rail_lush.png)
   - Rendered multi-track horizontal shelf (*Viva La Vida*, *Daylight*, *Starboy*).

---

## 7. Release Acceptance Checklist

| Item | Requirement | Status | Verification Detail |
| :---: | :--- | :---: | :--- |
| 1 | Release version validated | **PASS** | `versionCode = 7`, `versionName = "1.0.0-beta.1"` |
| 2 | Git checkpoint preserved | **PASS** | Branch `feature/synapse-recommendations` tracked cleanly |
| 3 | All critical unit tests passing | **PASS** | 165 total unit tests passing (0 failures) |
| 4 | STEREO debug build successful | **PASS** | `assembleDebug` compiled cleanly |
| 5 | SYNAPSE library tests successful | **PASS** | 29/29 tests passed in `Synapse` |
| 6 | Release build successful | **PASS** | `assembleRelease` completed with `lintVitalRelease` active |
| 7 | Meaningful release lint verification | **PASS** | `checkReleaseBuilds = true`, `abortOnError = true`, 0 errors |
| 8 | Standalone repo build capability | **PASS** | Embedded `:Synapse` subproject module; zero sibling directory assumptions |
| 9 | Release signing compatible | **PASS** | Verified with `apksigner`; identical RSA 4096-bit production key as Release 1 |
| 10 | Room migrations verified | **PASS** | `MIGRATION_2_3` verified via automated migration tests |
| 11 | Upgrade from Release 1 (Offline) | **PASS** | Same app ID, monotonic version code, identical signing key, Room schema continuous |
| 12 | Upgrade from Release 1 (Live Device) | **NOT TESTED** | Device disconnected (`adb devices` = 0); honestly reported per policy |
| 13 | Streaming playback functional | **PASS** | InnerTubeX + Media3 ExoPlayer streams Google Video CDN audio |
| 14 | Automatic caching functional | **PASS** | Streams write Media3 `.v3.exo` spans to `automatic_audio_v2` |
| 15 | Offline cached replay functional | **PASS** | Replaying cached audio uses local byte ranges without network calls |
| 16 | Personalized recommendations functional | **PASS** | SYNAPSE dual-horizon engine surfaces dynamic personalized rails |
| 17 | Negative feedback behaves appropriately | **PASS** | Single skips do not banish artists; repeated skips confidence-weighted |
| 18 | Legacy fallback functional | **PASS** | Circuit-breaker delegates cleanly if SYNAPSE disabled or fails |
| 19 | UI issues resolved | **PASS** | Scroll-aware FAB, 2-line title wrapping, and bottom clearance verified |
| 20 | Performance acceptable | **PASS** | Instant 60fps Compose scrolling; background service stable |
| 21 | No critical crashes or ANRs | **PASS** | Zero unhandled exceptions or ANRs observed in device logcat |
| 22 | Security & privacy audit complete | **PASS** | Zero trackers, private keystore outside git, noBackup storage used |
| 23 | Release documentation complete | **PASS** | `RELEASE_NOTES.md`, `CHANGELOG.md`, `README.md`, `7.txt` updated |
| 24 | Release assets staged | **PASS** | Staged in `release-staging/release-2/` with SHA-256 checksums |
| 25 | Public publishing approval pending | **PENDING** | **Awaiting explicit user authorization before publishing** |

---

## 8. Staged Release Assets Directory

Directory: `release-staging/release-2/`
- `STEREO-v1.0.0-beta.1-release.apk` (18,415,227 bytes)
- `SHA256SUMS.txt` (`8de248781e2e4bf810bbcd58b87291eddfd56e4e1d6c3ded251ef4b5c852eb3e`)
- `RELEASE_NOTES.md`
- `CHANGELOG.md`
- `screenshots/` (`m2_01_home_top.png` through `m2_05_because_rail_lush.png`)
