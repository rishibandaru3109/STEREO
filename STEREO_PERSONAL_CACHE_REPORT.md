# STEREO personal automatic audio cache

Status: private APK built; deterministic tests and the real Android Media3 fixture passed. Actual-song acceptance is running.

## Scope and preservation

Private debug build only; no push, public release, Alpha.6 publication, signing change, package change or version bump. The production package remains com.nova.music; this APK retains the existing debug package com.nova.music.debug and debug certificate, so it updates the debug installation used in this session. It does not migrate or overwrite the production installation.

The existing uncommitted data optimizations were retained. Before this work, source, build configuration, tools and the earlier reports were backed up to scratch/stereo-before-personal-cache-20261122.zip. The earlier STEREO_AUDIO_CACHE_REPORT.md describes the previous provider-excluding build and is superseded by this report for the personal build.

## Inspection and reference

Inspected STEREO's Media3 1.3.1 wiring, ExoPlayer controller, InnerTubeX and fallback extractors, URL-resolution reuse, quality settings, network metering, traffic counters and prior cache implementation.

Read the current upstream versions of these Metrolist files using GitHub on 22 November 2026:

- androidApp/src/main/kotlin/com/metrolist/android/playback/AudioCacheManager.kt
- androidApp/src/main/kotlin/com/metrolist/android/playback/AndroidQueueTransitions.kt
- androidApp/src/main/kotlin/com/metrolist/android/playback/SabrPlaybackCache.kt
- shared/src/jvmSharedMain/kotlin/com/metrolist/shared/playback/ffmpeg/DirectPlaybackCache.kt
- shared/src/jvmSharedMain/kotlin/com/metrolist/shared/playback/PlaybackPreCache.kt
- androidApp/src/main/kotlin/com/metrolist/android/playback/SongDownloadManager.kt
- docs/audio-cache.md

Reference: https://github.com/MetrolistGroup/Metrolist/blob/main/docs/audio-cache.md

Metrolist separates playback capture from publication, validates coverage, uses reader ownership and generation invalidation, bounds pre-cache, and keeps explicit downloads separate. Its SABR capture depends on a SABR-aware input stream with representation and segment completeness checks. STEREO does not have that transport.

Metrolist is GPL-3.0. This implementation uses independently written integration code and Media3's cache APIs rather than copying Metrolist source. Existing third-party attribution and licenses are preserved.

## Playback and storage architecture

1. The existing extractor still resolves the playable stream. A model helper retains stable lmt representation revision, xtags audio-track information and clen length hints when present in the URL.
2. The repository checks complete disk audio before its short-lived signed-URL cache or extractor. Offline lookup may use a different already-cached quality to avoid making complete songs unavailable merely because Auto changed with connectivity.
3. The controller gives each progressive MediaItem its stable custom cache key, holds a playback pin until the track changes or the player releases, and retains position for quality changes and a bounded expired-URL retry.
4. A single app-wide SimpleCache sits in the actual player DataSource chain. It writes received bytes during normal playback, reads existing ranges first and locks missing ranges to prevent duplicate writers.
5. Audio lives under noBackupFilesDir/automatic_audio_v2, independently of artwork and explicit downloads. Partial and complete spans both survive app restarts. It is not Android's reclaimable cacheDir, and media is excluded from automatic backups through noBackupFilesDir.
6. Promotion requires contiguous coverage from zero to the known representation length, exact indexed file sizes, a SHA-256 of the assembled ranges, and MediaExtractor recognition of an audio track with positive duration and readable beginning/tail timestamps.
7. Completed audio is published through atomic SQLite content metadata over already-committed Media3 span files. No second full audio copy is made. SHA-256 is checked before complete offline lookup.
8. Completed playback uses a local-only upstream that throws if any required bytes disappear. It cannot silently contact an expired or placeholder URL. Missing/corrupt entries fall out of local lookup and can be streamed again online.
9. Eviction uses representation-level last access, protects the current playback key and active DataSource readers, and reserves 64 MiB of free storage. An active protected representation can temporarily keep usage above a newly lowered limit; it becomes reclaimable when released.
10. Clearing invalidates publication, persists removal tombstones, hides future lookup immediately, and defers physical removal of active audio until its final owner releases it. Restart reconciliation reclaims tombstoned files.

Keys hash provider, track/video ID, format/itag, codec, MIME type, bitrate, audio-track identity and representation revision. Signed URLs, signatures and request headers are not persisted. Length is checked on representation reuse rather than included in the key, so learning a previously unknown length does not strand received ranges. A changed known length invalidates an idle entry before writing.

The SHA-256 detects changes after capture; it is not an origin-provided authenticity proof. MediaExtractor/container validation is not a decode of every sample.

## Formats and provider behavior

| Case | Implementation status | Device evidence |
| --- | --- | --- |
| Direct HTTP progressive audio | Connected to real Media3 playback cache | Pending |
| WebM/Opus | Stores the original representation; progressive playback and Android container validation | Pending actual song |
| AAC/M4A | Same byte-range pipeline; no remux or transcode | Pending |
| HTTP byte ranges / seeks | Media3 reuses received spans and requests holes | Pending real-device fixture and song seek |
| YouTube Music direct streams | Enabled in the private debug engine, including previously playable InnerTubeX/fallback streams | No observed device reuse yet |
| SABR / segmented streams | Unsupported; identity rejects these transports | Existing STEREO extractor/player exposes no SABR session or segment capture layer |
| Unknown length | May retain partial ranges in the personal build; cannot publish until reliable length becomes known | Pending |
| Raw Track.streamUrl without format/provider metadata | Existing direct playback remains available; automatic caching requires an identified AudioStream representation | Not extended in this change |

The private debug flag explicitly opts into storing already-playable direct YouTube-derived audio; it does not claim provider permission. Production configuration retains the provider exclusion. No DRM handling, authentication bypass, access-control bypass or new decipher mechanism was added.

## Mobile data and settings

Default: caching enabled, 500 decimal MB, optional prefetch disabled. Settings includes Disabled, 100 MB, 250 MB, 500 MB, 1 GB and Unlimited; Unlimited still reserves free device storage.

Settings reads actual disk state for audio bytes and unique complete/partial song counts, and shows independent artwork usage and clear controls. Protected audio pending removal remains counted until physically reclaimed.

Prefetch observes the queue, current track, selected quality, network and cache settings. It targets only the next remote song, waits five seconds, requires unmetered Wi-Fi, and reads at most 1,048,576 bytes through the same range cache. Mobile data, metered Wi-Fi, recommendations and playlists do not trigger speculative full-track downloads.

Data Saver, Balanced, High, Auto, bounded forward buffering, artwork limits and short-lived URL-resolution reuse remain in place. HTTP 401/403/410 playback failures can re-resolve once for the current playback request, retaining position and already-received audio.

## Automated and device verification

The JVM suite tests representation separation, debug-only YouTube eligibility, unsupported transports, quality identity, complete file coverage, partial gaps, cached seeks, truncation, missing files, same-size corruption, reopening file spans, eviction, playback pins and the free-space reserve. It also retains the existing queue, recommendation, preferences, theme, history and extractor tests.

The debug-only CacheVerificationActivity is excluded from release source sets. Its explicit fixture exercises a separate real Android Media3 disk cache using generated WAV data: first writes, partial reuse, complete publication, zero-upstream replay, restart, cached seek, concurrent readers, corruption recovery and clear with an attached reader. This fixture does not represent actual YouTube network measurements.

Its song phases exercise the production playback controller and shared OkHttp traffic instrumentation: a complete first play, replay, process restart, seek, background playback and optional disconnection. Actual consumed audio response payload bytes are recorded separately from API/artwork traffic. TCP/TLS overhead and unread operating-system/socket buffers are excluded. Failed phases produce failed JSON, never fabricated byte results.

Selected song: The Weeknd and Anitta, Sao Paulo official audio, video ID AQ5NlI-SJR0.
Source: https://www.youtube.com/watch?v=AQ5NlI-SJR0

Run in normal host PowerShell, with the already-authorized Realme connected:

    .\tools\Test-PersonalAudioCache.ps1 -TestOffline

This command installs the exact private APK, clears only automatic audio cache for a cold first play, and plays the selected song in full. It force-stops only the debug app between phases. The optional offline phase temporarily disables Wi-Fi/mobile data and restores their prior settings in finally. The background phase goes to the Home screen. Results and filtered diagnostics are saved under scratch/device-cache-<timestamp>/.

Phone results are claimed only when those JSON files exist and show the asserted measurements. A zero-byte replay must also have a complete local entry and a progressing playback position.

## Provider limitations

YouTube's published developer policies prohibit caching/storing audiovisual content without prior written approval and prohibit offline availability. They also restrict undocumented services and audio separation. Personal/private development does not itself establish an exemption. This prototype's enabled YouTube caching is therefore not presented as provider-compliant or suitable for public release.

Policy reference: https://developers.google.com/youtube/terms/developer-policies#e.-handling-youtube-data-and-content

## Exact files changed in this continuation

- app/src/main/java/com/nova/music/core/cache/AudioCacheEngine.kt
- app/src/main/java/com/nova/music/core/cache/AudioCacheIdentity.kt
- app/src/main/java/com/nova/music/core/cache/ConfigurableAudioEvictor.kt
- app/src/main/java/com/nova/music/core/cache/AudioFileRanges.kt
- app/src/main/java/com/nova/music/core/cache/AudioPrefetcher.kt
- app/src/main/java/com/nova/music/core/network/NetworkMonitor.kt
- app/src/main/java/com/nova/music/data/repository/MusicRepositoryImpl.kt
- app/src/main/java/com/nova/music/domain/model/AudioStream.kt
- app/src/main/java/com/nova/music/di/AppModule.kt
- app/src/main/java/com/nova/music/playback/ExoPlayerController.kt
- app/src/main/java/com/nova/music/feature/settings/SettingsViewModel.kt
- app/src/main/java/com/nova/music/feature/settings/SettingsScreen.kt
- app/src/debug/AndroidManifest.xml
- app/src/debug/java/com/nova/music/testing/CacheVerificationActivity.kt
- app/src/test/java/com/nova/music/network/AudioCacheIdentityTest.kt
- app/src/test/java/com/nova/music/network/AudioCacheEvictionTest.kt
- app/src/test/java/com/nova/music/network/AudioFileRangesTest.kt
- tools/Test-PersonalAudioCache.ps1
- STEREO_PERSONAL_CACHE_REPORT.md

Temporary verification scripts/logs remain under scratch and are not intended for Git. Earlier unrelated uncommitted files remain intact.

## Build, test and measurement results

- Gradle final build: BUILD SUCCESSFUL in 3 minutes 30 seconds. assembleDebug, compileDebugUnitTestKotlin and writeDebugUnitTestClasspath completed. Gradle/Kotlin ran in process to avoid blocked daemon sockets.
- Deterministic JUnit suite: 132 passed. The direct runner now includes deterministic extractor tests; the earlier runner excluded that whole package along with live tests. The new cache checks also passed. No Gradle test-worker success is claimed because its loopback connection is unavailable in this runtime.
- Live integration suite: seven executed, six failed. Search failures report music.youtube.com; an independent .NET DNS lookup returned "No such host is known." Stream-resolution assertions failed as well. The radio test has a conditional assertion and its pass does not prove live connectivity.
- PowerShell device runner: syntax checked and native flag forwarding checked. Its initial -W common-parameter binding bug was corrected before the next device attempt.
- git diff --check: passed.
- APK signature verification: v2 verified, with the same debug certificate SHA-256 c7a123376dca9dc33d84e7b287dc0a15ee321bdfd8a3a9edaff36a64f65d1fc1.
- APK: C:\Users\B.Rishi\Downloads\music player\scratch\artifacts\STEREO-personal-cache-debug.apk
- APK size: 26,448,137 bytes.
- APK SHA-256: 420706E5E0991ECD24C7DD26A9D77964E016B6D7D599BA76EE794E213EB0ADC3.
- Package com.nova.music.debug, version 1.0.0-alpha.5, code 6, minimum SDK 26 and target SDK 34. Production signing, application IDs and version configuration were not edited.
- Runtime ADB still fails with "could not read ok from ADB Server"; a foreground server attempt reports a failed interrupt socketpair. Host PowerShell can reach the attached Realme and has run the fixture successfully. Actual-song phases are running through that same host runner.
- Logs: scratch/stereo-personal-final-build.log, scratch/stereo-personal-junit.log, scratch/stereo-personal-live-tests.log and scratch/stereo-personal-signature.log.

| Actual song measurement | Result |
| --- | --- |
| First playback audio-network bytes | Not measured yet |
| Replay audio-network bytes | Not measured yet |
| Replay after process restart | Not measured yet |
| Offline playback | Not verified yet |
| Seek through cached ranges | Not verified yet |

Real-device fixture result: scratch/device-cache-20261122-222605/fixture.json, run ID 78129900-7e56-4639-aa89-62cd53b1083a. Passed on the connected Realme RMX5030. First upstream payload 32,044 bytes, replay additional payload zero, simultaneous readers combined payload 32,044 bytes. The fixture also passed partial-range reuse, complete publication, local-only replay, engine reopening, seeking, corruption rejection/recovery and clear with an attached reader. These are generated WAV fixture reads through the real Android Media3 disk cache, not YouTube network measurements.

The real-song acceptance criterion is not satisfied by the JVM tests, APK build or generated fixture alone. No measured data-saving percentage is claimed. Actual-song results will be appended when available.

## Remaining limitations

- SABR/segmented playback needs a new transport and complete-capture implementation; it is not supported by the existing STEREO stack.
- Raw Track.streamUrl playback without representation metadata bypasses automatic capture.
- Complete validation depends on the Android container parser exposing a positive duration and valid audio samples. A fully received but unrecognized container is retained as partial rather than falsely published offline.
- Partial-range files have size/coverage checks; the end-to-end checksum is established at complete publication and checked on complete lookup. This is not an origin-authenticated checksum for bytes captured from the network.
- Prefetch cancellation is checked between reads; an already-blocked network read can remain until it returns or times out. No further read is started by the canceled job.
- Actual phone replay, reconnect behavior, notification controls, large-file eviction and physical disk-full recovery still require device evidence. The JVM storage-exhaustion test does not fill the user's phone storage.
