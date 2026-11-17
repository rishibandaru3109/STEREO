# STEREO — Open-Source Dependency & License Compliance Audit

**Project:** STEREO (Android Music Player)  
**Date of Audit:** 2026-11-17  
**Scope:** Core streaming, extraction, player, and architecture dependencies.

---

## 1. Third-Party Dependencies Audit

| Dependency Artifact | Resolved Version | Stated License | Source / Upstream Repository | Distribution / Copyleft Obligations |
|---|---|---|---|---|
| `com.github.MetrolistGroup.innertubex:innertubex-desktop` | `v0.11.7` | **GNU GPL v3.0** | [GitHub: MetrolistGroup/innertubex](https://github.com/MetrolistGroup/innertubex) | Strong copyleft. Any distributed binary linking to InnerTubeX must make complete source code available under GPL-3.0. |
| `libs/quackjs.jar` (`com.github.MetrolistGroup.quackjs`) | Embedded JAR | **GNU GPL v3.0** (QuickJS core is MIT) | [GitHub: MetrolistGroup/quackjs](https://github.com/MetrolistGroup/quackjs) | Strong copyleft for the wrapper JAR. |
| `androidx.media3:media3-*` (ExoPlayer & Session) | `1.3.1` | **Apache 2.0** | [Google AndroidX Media3](https://github.com/androidx/media) | Permissive. Requires notice and license attribution. |
| `androidx.room:room-*` (Database & Runtime) | `2.6.1` | **Apache 2.0** | [Google AndroidX Room](https://developer.android.com/jetpack/androidx/releases/room) | Permissive. Notice and license attribution. |
| `androidx.datastore:datastore-preferences` | `1.1.1` | **Apache 2.0** | [Google AndroidX DataStore](https://developer.android.com/topic/libraries/architecture/datastore) | Permissive. |
| `androidx.compose.*` (Jetpack Compose BOM) | `2024.06.00` | **Apache 2.0** | [Google AndroidX Compose](https://developer.android.com/jetpack/compose) | Permissive. |
| `io.insert-koin:koin-*` (Dependency Injection) | `3.5.6` | **Apache 2.0** | [InsertKoin.io](https://github.com/InsertKoinIO/koin) | Permissive. |
| `com.squareup.okhttp3:okhttp` | `4.12.0` | **Apache 2.0** | [Square OkHttp](https://github.com/square/okhttp) | Permissive. |
| `io.coil-kt:coil-compose` | `2.6.0` | **Apache 2.0** | [Coil-kt](https://github.com/coil-kt/coil) | Permissive. |
| `io.ktor:ktor-client-*` | `3.0.0` | **Apache 2.0** | [JetBrains Ktor](https://github.com/ktorio/ktor) | Permissive. |
| `org.jetbrains.kotlinx:kotlinx-coroutines-*` | `1.8.1` | **Apache 2.0** | [JetBrains Coroutines](https://github.com/Kotlin/kotlinx.coroutines) | Permissive. |

---

## 2. Copyleft & Distribution Assessment

1. **GPL-3.0 Linkage:** Because STEREO incorporates `innertubex-desktop:v0.11.7` and `quackjs.jar` directly into its APK build pipeline, any public binary release (e.g. F-Droid, GitHub Releases, Google Play) of the compiled STEREO application falls under the terms of the GNU General Public License v3.0.
2. **Current Status:** The application is developed strictly for private and internal testing. No public distribution is occurring during this phase.
3. **Future Distribution Policy:** When distributed publicly, the STEREO source code repository must be made fully available under GPL-3.0, preserving all copyright notices and including a full copy of the GPL-3.0 license text.
