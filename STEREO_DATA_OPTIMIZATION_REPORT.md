# STEREO data optimization preview

Workspace C:\Users\B.Rishi\Downloads\music player.

Prepared on 22 November 2026 from commit 69748b0. The workspace was clean at the start.
Verified recovery bundle at scratch/stereo-pre-data-optimization.bundle.

## Confirmed causes and limits

- The primary extractor used AUTO, which selects a high-scoring WebM format. The fallback explicitly favored the highest bitrate. There was no selectable data-saving quality.
- Media3 used its default forward buffering without an app-specific reduction for frequent skipping.
- Stream resolution had no repository-level reuse, so callers could repeat catalog/decipher work and native playability checks.
- Home's candidate fingerprint included the exact last-listen timestamp and count, so completed listens could trigger new candidate harvests.
- Large thumbnail URLs were used even in compact rows. Coil already provided caching. This change adds explicit limits and size-aware CDN variants.
- Persistent audio caching requires prior written YouTube approval according to its published developer policy. No approval was present. Under the user's condition, persistent audio caching and offline replay were excluded. There is no active LRU audio store or stable audio disk key.
- This checks the caching restriction. It does not certify broader source-service compliance.

Policy reference at https://developers.google.com/youtube/terms/developer-policies#e.-handling-youtube-data-and-content

## Implemented behavior

| Area | Result |
| --- | --- |
| Quality | Data Saver selects the lowest known bitrate in available Web Remix audio formats. Balanced selects the closest available bitrate to 96 kbps. High selects the highest. Video is excluded and default audio-language variants are preferred. |
| Fallback | If low/balanced Web Remix resolution fails, the native LOW/MP4 selector remains available. Its format list and codec preference may produce a higher bitrate. No bitrate is guaranteed. |
| Auto | Default for existing installations. Android metering selects Data Saver or High. Explicit modes override Auto. Metered Wi-Fi receives Data Saver. Unknown network state is treated as metered. |
| Mode changes | Re-resolve the active remote track and resume its position. Paused playback stays paused. Queue and current listening session are retained. Direct stream URLs cannot expose alternate formats. |
| Connectivity | Observe the default network without polling. An errored track can retry on reconnect if playback is still requested. Physical handoff testing remains necessary. |
| Buffering | Forward thresholds 5Ã¢â‚¬â€œ15 seconds, startup 1 second, rebuffer 2.5 seconds, retained back buffer zero. Actual reads can exceed the time estimate due to extractor/socket granularity. |
| Skips | Stop/clear the previous media item before resolving the next. Cancellation reaches the fallback player HTTP call and native coroutine path. Some synchronous native validation/decipher work may finish after cancellation, with stale playback blocked by request generation. |
| URL reuse | At most 32 in-memory results keyed by track and requested mode. Lifetime at most 2 minutes and before known expiry minus a safety margin. Concurrent successful resolutions are reused. Failed/expired URLs are retried and playback errors invalidate entries. Only URL metadata is held. |
| Probe | Existing native 1 KB range validation remains to preserve fallback for unusable URLs. |
| Recommendations | Exact listen timestamps no longer invalidate the candidate pool. A coarse ten-listen bucket refreshes candidates while current history rebuilds sections locally. Likes, exclusions, languages, explicit refresh and TTL affect regeneration. Healthy pools last one hour; thin/failing pools retry after five minutes. |
| Artwork | Singleton Coil loader shares the metered client. Explicit 64 MiB disk limit and 12% memory limit. Known Google artwork size suffixes use 128/256/512/768 pixel buckets matching display size. Unrecognized URLs stay intact. HTTP cache restrictions are respected. |
| Prefetch | No speculative audio prefetch is added on any network. Quality callbacks perform playback work only for an active/requested track. Network changes do not refresh recommendations. |
| Measurements | Debug Audio & Playback settings sample process-local audio/artwork/API response payload bytes, request counts, and metadata/artwork cache hits. Network interceptors include retries. Declared upload lengths are separate. |
| User data | New DataStore preference key only. No Room migrations or data resets. Production signing configuration, credentials, application IDs and version numbers were not edited. |

## Before and after evidence

Live playback traffic was not measured. ADB could not start its local server and the host could not resolve music.youtube.com.
No physical-phone, actual 30-minute, observed replay savings, or Wi-Fi/mobile handoff results are available.

The examples below are synthetic fixed-rate fixtures or arithmetic checks using identical durations.
They exclude artwork, APIs, headers, TLS, DNS, retransmissions and socket overhead.

| Scenario | Baseline example | Optimized example | Evidence |
| --- | --- | --- | --- |
| 30 minutes | 36.0 MB at 160 kbps | 10.8 MB at 48 kbps | Formula and synthetic response-body fixture |
| Nominal balanced rate, 30 minutes | 36.0 MB at 160 kbps | 21.6 MB at 96 kbps | Estimate, 96 kbps may be unavailable |
| Replay 30 minutes | Full audio payload again | Full audio payload again | Persistent audio cache disabled, no audio-byte saving claimed |
| 30 skips after 5 seconds, assuming full forward buffers at 160 kbps | 33 MB with 50 seconds ahead | 12 MB with 15 seconds ahead | Synthetic arithmetic including played audio |
| Unplayed portion of that skip scenario | 30 MB | 9 MB | Estimate, not observed download waste |
| Eight concurrent resolutions | Previously each resolved independently | One successful extractor call | Deterministic fake-extractor assertion |
| Artwork cache effectiveness | No measured baseline | Counters provided | Requires repeated navigation on a device |

MB is decimal 1,000,000 bytes. Buffer limits constrain time, not an exact cellular byte budget.
The counters exclude transport overhead and unread bytes in socket/OS buffers.
Use Android per-app data totals to assess carrier usage.

## Reproduce measurements

1. Install the debug APK over a compatible debug installation. Package com.nova.music.debug is separate from production com.nova.music.
2. Keep phone, playlist/order, app foreground/background state, network and start positions identical between runs. Record modes and actual format/bitrate diagnostics.
3. In Settings Ã¢â€ â€™ Audio & Playback, tap Sample traffic before the scenario. Play the fixed playlist for 30 minutes, pause, and sample again. Keep the process alive.
4. Export the snapshots and calculate the delta using the commands below.
5. Repeat for replaying the same songs, then for 30 skips five seconds apart. Label cold/warm artwork-cache runs.
6. Test Auto on mobile, unmetered Wi-Fi and metered Wi-Fi. Switch while playing, paused and resolving. Check position, queue, pause state, notification controls and reconnection.
7. Capture Android per-app totals separately. Comparing the old alpha and preview under identical conditions gives a real total before/after baseline. The older alpha lacks category counters, so category comparisons need equivalent instrumentation.

    adb logcat -d -v epoch -s STEREO_TRAFFIC:I '*:S' > scratch/traffic-run.log

    .\tools\Summarize-Traffic.ps1 -LogPath scratch/traffic-run.log -Scenario saver-30min -OutputPath scratch/saver-30min.json

The summarizer uses the last two snapshots, rejects resets/non-increasing times, and reports elapsed time, byte/request/cache-hit deltas.
Its synthetic parser fixture was verified locally. No fixture output represents phone measurements.

## Validation

- 102 deterministic tests passed against the final compiled classes through JUnit 4.13.2.
- Eleven new tests cover quality policy, source-format selection, metering changes and overrides, quality persistence, duplicate resolution, expired URL/error handling, cancellation, byte counting, synthetic playback/replay/skip scenarios and artwork sizing.
- Existing cache regression coverage now checks that completing a listen does not force another candidate request.
- Existing queue/shuffle, themes, history, playlists, likes, preference and recommendation tests ran in the deterministic suite.
- Gradle successfully compiled main/test Kotlin and packaged the final debug APK. An earlier assemble/classpath invocation finished BUILD SUCCESSFUL.
- The final combined Gradle invocation completed assembleDebug and writeDebugUnitTestClasspath, but testDebugUnitTest failed because its worker could not connect to 127.0.0.1 with Permission denied: getsockopt. The final classes were then executed directly with JUnit and all 102 tests passed. The Gradle test task remains blocked by the runtime socket restriction.
- Traffic summarizer fixture passed with 1800 seconds, 10,800,000 audio bytes, 1000 artwork bytes, 500 API bytes, two metadata hits and five artwork hits. Those values are synthetic test input.
- APK signature verification passed using v2 signing and matches the previous debug certificate.
- Package and SDK metadata verified with aapt2. git diff --check passed.
- ADB initialization failed. A direct request to music.youtube.com failed DNS resolution. No live integration or phone verification succeeded in this session.

Normal local test/build command, outside the restricted runtime when necessary

    .\\gradlew.bat testDebugUnitTest assembleDebug

Restricted-runtime verification used the generated classpath from scratch/stereo-test-classpath.init.gradle
and scratch/run-stereo-junit.ps1. The final JUnit output is scratch/stereo-data-junit-final.log.
Build output is scratch/stereo-data-final.log. Compiler deprecation/opt-in warnings remain.


## Debug artifact

APK path

    C:\Users\B.Rishi\Downloads\music player\app\build\outputs\apk\data-preview\STEREO-data-optimization-debug.apk

Package com.nova.music.debug. Minimum SDK 26, target SDK 34.
Size 26343869 bytes.
SHA-256 72B47EB6EEF7DA8D49ADB84FAA614C7028FA3AF4B8B9F99C3C981929AAEDBFDC.

The signer certificate SHA-256 matches the previous debug APK

    c7a123376dca9dc33d84e7b287dc0a15ee321bdfd8a3a9edaff36a64f65d1fc1


Internal version remains 1.0.0-alpha.5, code 6. This is a local preview for Alpha.6 preparation.

## Remaining verification

- Obtain device measurements before claiming percentage savings or release stability.
- Check low-quality fallback availability across songs.
- Exercise skips, seeking, notification controls, background playback, pause, reconnection, queues and shuffle on a phone.
- Smaller buffers may rebuffer sooner on poor connections. Tune them using measurements.
- Persistent audio replay requires an authorized caching arrangement or a source that permits it.

## Exact changed files

- STEREO_DATA_OPTIMIZATION_REPORT.md
- app/src/main/java/com/nova/music/NovaApplication.kt
- app/src/main/java/com/nova/music/core/artwork/ArtworkSizingInterceptor.kt
- app/src/main/java/com/nova/music/core/network/NetworkMonitor.kt
- app/src/main/java/com/nova/music/core/network/TrafficMeter.kt
- app/src/main/java/com/nova/music/data/extractor/InnerTubeXExtractor.kt
- app/src/main/java/com/nova/music/data/extractor/YouTubeMusicExtractor.kt
- app/src/main/java/com/nova/music/data/repository/HomeRepositoryImpl.kt
- app/src/main/java/com/nova/music/data/repository/MusicPreferencesRepositoryImpl.kt
- app/src/main/java/com/nova/music/data/repository/MusicRepositoryImpl.kt
- app/src/main/java/com/nova/music/di/AppModule.kt
- app/src/main/java/com/nova/music/domain/model/AudioQualityMode.kt
- app/src/main/java/com/nova/music/domain/repository/MusicExtractor.kt
- app/src/main/java/com/nova/music/domain/repository/MusicPreferencesRepository.kt
- app/src/main/java/com/nova/music/domain/repository/MusicRepository.kt
- app/src/main/java/com/nova/music/feature/settings/SettingsScreen.kt
- app/src/main/java/com/nova/music/feature/settings/SettingsViewModel.kt
- app/src/main/java/com/nova/music/playback/ExoPlayerController.kt
- app/src/test/java/com/nova/music/database/RecommendationPreferencesTest.kt
- app/src/test/java/com/nova/music/database/StreamResolutionTest.kt
- app/src/test/java/com/nova/music/network/DataEfficiencyTest.kt
- tools/Summarize-Traffic.ps1
