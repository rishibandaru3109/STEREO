# STEREO × SYNAPSE Baseline Audit & Safety Checkpoint

**Date:** November 2026  
**Host Application:** STEREO (`com.nova.music`, debug `com.nova.music.debug`)  
**Host Repository:** `C:\Users\B.Rishi\Downloads\music player`  
**Base Commit on Master:** `69748b0 chore(release): bump version to 1.0.0-alpha.5 (code 6)`  
**Integration Branch:** `feature/synapse-recommendations`  
**Hardware Environment:** Windows 11 Laptop (Intel CPU, 16 GB RAM, NVIDIA GeForce RTX 4050 6GB VRAM)  
**Execution Condition:** Physical phone disconnected per user directive; 100% PC-based test execution.  

---

## 1. STEREO Baseline Working Tree Status

Before making integration edits, the STEREO working tree was inspected and preserved:
- **Branch:** Switched from `master` to `feature/synapse-recommendations`.
- **Pre-existing uncommitted modifications preserved:**
  - `NovaApplication.kt`, `InnerTubeXExtractor.kt`, `YouTubeMusicExtractor.kt`, `HomeRepositoryImpl.kt`, `MusicPreferencesRepositoryImpl.kt`, `MusicRepositoryImpl.kt`, `AppModule.kt`, `AudioStream.kt`, `MusicExtractor.kt`, `MusicPreferencesRepository.kt`, `MusicRepository.kt`, `SettingsScreen.kt`, `SettingsViewModel.kt`, `ExoPlayerController.kt`, `RecommendationPreferencesTest.kt`.
- **Pre-existing untracked files preserved:**
  - `STEREO_AUDIO_CACHE_REPORT.md`, `STEREO_DATA_OPTIMIZATION_REPORT.md`, `STEREO_PERSONAL_CACHE_REPORT.md`, `app/src/debug/`, `app/src/main/java/com/nova/music/core/artwork/`, `app/src/main/java/com/nova/music/core/cache/`, `app/src/main/java/com/nova/music/core/network/`, `app/src/main/java/com/nova/music/domain/model/AudioQualityMode.kt`, `app/src/test/java/com/nova/music/database/StreamResolutionTest.kt`, `app/src/test/java/com/nova/music/network/`, `tools/`.
- **Pre-integration Test Baseline:**
  - All 31 existing STEREO test suites passed cleanly (`BUILD SUCCESSFUL in 42s`).

---

## 2. SYNAPSE Recommendation Engine Verification

The standalone SYNAPSE engine at `C:\Users\B.Rishi\Downloads\Synapse`:
- **Branch:** `master` (Commit `25be514`).
- **Core Kotlin Modules:**
  - `model`: `DomainModels.kt`, `TasteModels.kt`
  - `event`: `EventIntelligence.kt` (Lu & Wu duration clamping, monotonic accounting, skip classification)
  - `taste`: `TasteIntelligence.kt` (dual-horizon exponential decay, category interest normalization)
  - `similarity`: `ContentSimilarityEngine.kt` ($O(K \cdot D)$ metadata TF-IDF & feature distance)
  - `candidate`: `CandidateInterfaces.kt` (CandidateProvider, Deduplicator, InMemoryProvider)
  - `ranking`: `CandidateRanker.kt` (Multi-factor hybrid ranker & explainability)
  - `discovery`: `DiscoveryIntelligence.kt` (MMR diversity re-ranking & artist consecutives limit)
  - `filter`: `EligibilityFilter.kt` (Strict language matching with graceful fallback)
  - `engine`: `SynapseRecommendationEngine.kt` (Pipeline orchestrator & execution telemetry)
  - `adapter`: `StereoAdapter.kt` (Domain mappers & STEREO bridge interface)
- **Unit Test Status:**
  - **28 tests completed, 0 failed, 0 skipped** (`BUILD SUCCESSFUL in 1m 29s`).
- **Empirical Benchmarks:**
  - MRR: `0.2799` (vs Popularity `0.1062`, Provider Automix `0.2081`).
  - Intra-List Diversity: `0.8332` (vs Top Artist `0.2216`, SASRec `0.2731`).
  - Artist Entropy: `3.6759` (vs Top Artist `0.6619`, SASRec `0.6883`).

---

## 3. Integration Plan & Protection Checklist

1. **Gradle Architecture:**
   - Add SYNAPSE source package into STEREO under `com.nova.music.core.synapse` or link it cleanly so STEREO builds as a unified project without external build dependencies.
2. **Database Integrity:**
   - Maintain `StereoDatabase` version 2 backward compatibility. Add version 3 non-destructive migration (`MIGRATION_2_3`) for `synapse_listening_events` and `synapse_taste_profile`, leaving existing `listening_history`, `liked_songs`, `tracks`, and `recommendation_cache` tables untouched.
3. **Feature Flag & Rollback Circuit Breaker:**
   - Store `KEY_SYNAPSE_RECOMMENDER_ENABLED` in `MusicPreferencesRepository` (defaulting to true in debug, with fallback toggle in Settings).
   - Any runtime failure in SYNAPSE automatically delegates to STEREO's legacy `RecommendationEngineImpl`.
4. **Playback & Cache Preservation:**
   - Zero changes to ExoPlayer caching or stream resolution.
   - Forward `ListeningSample` from `ListeningSession.kt` into SYNAPSE's `EventIntelligence`.
