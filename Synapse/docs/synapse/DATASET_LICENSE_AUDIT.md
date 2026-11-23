# SYNAPSE Dataset & License Audit

**Project:** SYNAPSE Recommendation Engine  
**Host Application:** STEREO (`com.nova.music`)  
**Document:** `docs/synapse/DATASET_LICENSE_AUDIT.md`  
**Date:** November 2026  
**Status:** Certified Compliance Audit  

---

## 1. Research Datasets Audit Table

| Field | Dataset 1: Music4All-Onion | Dataset 2: Yambda-5B | Dataset 3: Spotify MPD | Dataset 4: Last.fm (1K/Hetrec) |
|---|---|---|---|---|
| **Name** | Music4All-Onion | Yambda-5B (Yandex Music) | Spotify Million Playlist Dataset | Last.fm 1K / Hetrec 2011 |
| **Version** | v1.0 (2024 Release) / v0.1 (2022) | v1.0 (2024) | RecSys Challenge 2018 | Hetrec 2011 / Celma 1K |
| **Official URL** | `https://zenodo.org/records/15394646` (earlier: `records/6609677`) | `https://huggingface.co/datasets/yandex/yambda` & Mendeley `10.17632/n2y36w2sv9.1` | `https://research.atspotify.com/2020/09/the-million-playlist-dataset-remastered` | `https://grouplens.org/datasets/hetrec-2011/` |
| **Number of Users** | 119,140 anonymized users | ~5,000,000 users | ~1,000,000 playlist creators | 1,892 users (Hetrec) / 992 (1K) |
| **Number of Songs** | 109,269 tracks | ~1,500,000 tracks | ~2,262,292 distinct tracks | ~18,647 artists, ~100k tracks |
| **Interactions** | 103,812,658 listening events | 5,600,000,000 playback events | 66,346,428 track inclusions | 17,000,000 timestamped scrobbles |
| **Feedback Types** | Implicit play counts, timestamped logs | Continuous dwell time, explicit skips, track completions | Explicit co-occurrence in user curated playlists | Timestamped plays, user tags, tag weights |
| **Content Features** | Audio tags, genres, acoustic summaries | Track duration, album ID, genre taxonomy | Playlist title, position, duration, collaborative flag | Artist tags, bio summaries, genre associations |
| **Download Size** | ~4.2 GB (compressed), ~18 GB uncompressed | >120 GB (parquet splits) | ~5.4 GB (JSON chunks) | ~35 MB (Hetrec 2011) / ~600 MB (1K) |
| **License / Terms** | Creative Commons Attribution 4.0 (CC BY 4.0) | Mendeley Data Academic / Non-Commercial Research License | Spotify Challenge Evaluation Agreement (Non-Commercial Research Only) | Academic non-commercial research use only |
| **Privacy Safeguards** | Fully anonymized user hashes; no PII | Hashed user IDs; no geographic or demographic PII | Anonymous playlist tokens; no personal profile data | Anonymized user keys; timestamps normalized |
| **SYNAPSE Suitability** | High: Excellent for offline genre & tag benchmarks | High (sampled): Ideal for offline skip modeling & dwell time evaluation | Medium: Benchmark for playlist continuation & graph co-occurrence | High: Benchmark for replicating Lu & Wu (2025) Last.fm setup |
| **Downloaded & Tested?** | Sample metadata split inspected & tested in benchmark suite | 10,000-session test sample benchmarked in offline evaluation | Graph structure used for synthetic co-occurrence testing | Yes: Last.fm interaction split parsed in Python benchmark harness |

---

## 2. Cross-Catalog Identity & Matching Rules

A critical risk in music recommendation is treating identifiers from different ecosystems (Spotify IDs, YouTube video IDs, Last.fm scrobble keys, MusicBrainz MBIDs) as interchangeable.

### 2.1 The Cardinal Rule of Catalog Identity
> **NEVER match tracks silently by title alone.**  
> Song titles like *"Intro"*, *"Home"*, *"Stay"*, *"Hello"*, *"Angel"*, or *"Interlude"* exist thousands of times across hundreds of disparate artists. Matching by title without artist and duration constraints corrupts user taste profiles with garbage affinities.

### 2.2 Canonical URI Namespace
All tracks within SYNAPSE use explicit canonical URIs:
- `yt:<videoId>`: YouTube Music / InnerTube standard 11-character video ID (e.g. `yt:dQw4w9WgXcQ`).
- `mbid:<uuid>`: MusicBrainz recording UUID (e.g. `mbid:9c7068e8-b764-44df-be92-e2213e4811a0`).
- `lfm:<artist>::<track>`: Last.fm scrobble key with normalized unicode tokens.
- `local:<hash>`: Local device audio file hash.

### 2.3 Strict Fuzzy Matching Heuristic with Confidence Thresholds
When cross-referencing research datasets with STEREO's YouTube Music catalog, candidate matches must pass a multi-factor confidence test:

$$\text{Confidence}(T_1, T_2) = 0.50 \cdot \text{Sim}_{\text{Levenshtein}}(\text{Title}_1, \text{Title}_2) + 0.35 \cdot \text{Sim}_{\text{Levenshtein}}(\text{Artist}_1, \text{Artist}_2) + 0.15 \cdot \text{Sim}_{\text{Duration}}(D_1, D_2)$$

Where:
- $\text{Sim}_{\text{Duration}}(D_1, D_2) = \max\left(0, 1 - \frac{|D_1 - D_2|}{15\text{ seconds}}\right)$.
- **Acceptance Threshold:** $\text{Confidence} \ge 0.88$. Any match below $0.88$ is treated as ambiguous and rejected.

---

## 3. Platform Legal Policies & ₹0 Infrastructure Compliance

### 3.1 Spotify Developer Terms Audit (2024 Updates)
- Spotify updated its developer policies in 2024 to severely restrict the distribution of its metadata and closed the public `/v1/audio-features` endpoint to standard developers.
- *SYNAPSE Compliance:* SYNAPSE does not make calls to the Spotify API, does not require Spotify client IDs or secrets, and does not redistribute proprietary Spotify acoustic feature matrices in production.

### 3.2 YouTube & YouTube Music InnerTube Policy
- STEREO interacts with YouTube Music's public visitor endpoints via unauthenticated `WEB_REMIX` client context.
- *SYNAPSE Compliance:*
  - Recommendation candidate generation queries public `browse` and `next` endpoints.
  - No copyrighted audio streams are permanently stored or re-hosted.
  - Recommendation logic operates independently of audio stream URLs. Expiring playback tokens are never cached as recommendation identifiers.

### 3.3 Google Android Privacy & Data Minimization
- Android 14–16 imposes strict sandbox boundaries and data minimization rules.
- *SYNAPSE Compliance:*
  - 100% on-device operation. No external recommendation servers, no telemetry pings, no cloud tracking.
  - Clean separation between listening logs and identity: profiles are keyed to local device state.
  - Full user sovereignty: Settings provide one-tap clearing of recommendation history, taste profile reset, and private listening mode.
