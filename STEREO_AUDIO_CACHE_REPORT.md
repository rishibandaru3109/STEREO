# STEREO audio cache continuation

This continues the interrupted work after `scratch/stereo-before-audio-cache.zip`, preserving the data optimization changes and backup. It supplements the earlier `STEREO_DATA_OPTIMIZATION_REPORT.md` snapshot.

## Behavior

- One lazily opened Media3 `SimpleCache` stores audio bytes in app cache storage. Android may reclaim these files; this is not a download library.
- Default maximum is 500 decimal MB. Settings supports 100 MB, 250 MB, 500 MB and 1 GB, persists the selection, and trims older spans when the limit decreases.
- Keys include provider, track, format, codec, MIME type, bitrate, content length, audio track and representation revision. Signed URLs and request headers stay in memory, outside persisted descriptors.
- Cache eligibility requires explicit provider permission, an HTTP(S) audio representation and known length. YouTube provider IDs and YouTube/Googlevideo hosts bypass storage. **The current YouTube extractors do not enable persistent audio caching.**
- Eligible playback uses cached ranges and streams missing ranges. A fully cached matching quality can resolve locally without requesting a fresh signed URL. Partial files require online resolution; no speculative audio prefetch was added.
- Settings shows audio and artwork disk usage, separate clear actions and storage errors. Artwork keeps its existing independent 64 MiB limit. Clearing audio may be followed by new segments from active playback.
- Malformed descriptors are ignored. Cache file failures can fall back to upstream where a usable URL exists. A failed cache initialization releases its directory lock before subsequent attempts.

## Validation scope

New deterministic tests cover stable identity across URL rotation, representation separation, permission and source exclusions, invalid media rejection, storage-limit validation, least-recently-used eviction, immediate trimming and space reservation for incoming fragments.

Physical disk replay, partial seek coverage, disk-full recovery and clear-during-playback require Android device verification. ADB server initialization is blocked in this execution environment. No measured playback savings or offline YouTube replay is claimed.

## Reproduce

Normal local verification:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

The restricted runtime uses `scratch/build-stereo-cache.ps1` to launch Gradle and the Kotlin compiler in process with writable temporary build state. `scratch/run-stereo-junit.ps1` runs compiled deterministic classes directly because Gradle's test workers need a blocked loopback socket. These scripts and logs are temporary verification aids, outside the tracked source changes.

## Verified result

- Gradle `assembleDebug`, `compileDebugUnitTestKotlin` and `writeDebugUnitTestClasspath`: **BUILD SUCCESSFUL**, 3 minutes 56 seconds. Kotlin ran in process. Existing compiler deprecation and opt-in warnings remain.
- Direct JUnit execution of the final compiled deterministic suite: **110 tests passed** (102 existing plus eight new cache tests), 2.844 seconds. Live network tests were excluded. This does not claim a successful Gradle test-worker run.
- `git diff --check`: passed.
- APK signature: verified with v2 signing. Certificate SHA-256 matches the prior debug APK: `c7a123376dca9dc33d84e7b287dc0a15ee321bdfd8a3a9edaff36a64f65d1fc1`.
- Package: `com.nova.music.debug`, label STEREO, version `1.0.0-alpha.5` (code 6), minimum SDK 26, target SDK 34. Release signing and version configuration were preserved.
- APK: `app/build/outputs/apk/debug/app-debug.apk`, 26,365,742 bytes.
- APK SHA-256: `E0008961999741CD64F11FF7770B477FEE5E325CE031E497230F516515CD4A11`.
- Convenient identical copy: `scratch/artifacts/STEREO-cache-preview-debug.apk`.
- Logs: `scratch/stereo-cache-build.log` and `scratch/stereo-cache-junit.log`.
- ADB initialization failed with `could not read ok from ADB Server`; no phone install or playback verification was completed.
