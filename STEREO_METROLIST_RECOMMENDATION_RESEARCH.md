# STEREO — METROLIST & INNERTUBEX RECOMMENDATION ARCHITECTURE RESEARCH

**Document Version:** 1.0  
**Target Application:** STEREO (`com.nova.music`, debug `com.nova.music.debug`)  
**Target Hardware:** Realme RMX5030, Android 16  
**Reference Sources:** InnerTubeX (`com.metrolist.innertubex`), Metrolist Reference Architecture  

---

## 1. EXECUTIVE SUMMARY & ARCHITECTURAL FOUNDATION

The previous recommendation generation in STEREO suffered from a critical flaw: `CatalogCandidateProvider` was generating candidate pools exclusively by string-interpolating search queries (e.g., `"$genre music hits"`, `"$artist songs"`) and running generic searches against YouTube Music. This produced noisy candidate pools containing interview snippets, video essays, podcast clips, reactions, and generic top 40 tracks disconnected from the listener's actual musical identity.

To achieve industrial-grade recommendation fidelity without paid third-party APIs (Spotify Web API, Last.fm, or OpenAI embeddings), STEREO integrates the proven reverse-engineered YouTube Music architecture pioneered by **Metrolist** and powered by **InnerTubeX**.

By leveraging YouTube Music's native graph endpoints—specifically `browse` (`FEmusic_home`, `FEmusic_explore`) and `next` (song radio seed graph)—combined with STEREO's on-device listening history, Bayesian implicit taste weights, and strict cross-section deduplication, STEREO produces authentic, responsive music recommendations.

---

## 2. INNERTUBEX & METROLIST ENDPOINT SPECIFICATIONS

InnerTubeX communicates with Google's internal InnerTube RPC endpoints via Ktor client engines.

### 2.1 The `browse` Endpoint (`FEmusic_home` & `FEmusic_explore`)

- **Method:** `innerTube.browse(client = YouTubeClient.WEB_REMIX, browseId = "FEmusic_home")`
- **Payload:** `BrowseBody(context = WEB_REMIX, browseId = "FEmusic_home")`
- **Authentication:** Unauthenticated / Visitor session. No Google account login is required.
- **Returned Data:**
  - Carousels and shelves representing YouTube Music's curated feeds.
  - Root container: `contents.singleColumnBrowseResultsRenderer.tabs[0].tabRenderer.content.sectionListRenderer.contents`
  - Shelf renderers: `musicCarouselShelfRenderer` and `musicShelfRenderer`.
  - Item renderers:
    - `musicTwoRowItemRenderer`: Contains `title.runs[0].text`, `subtitle.runs[*].text` (artist, album, year), `thumbnailRenderer`, and `navigationEndpoint.watchEndpoint.videoId`.
    - `musicResponsiveListItemRenderer`: High-density list item containing video ID, artist runs, duration text, and thumbnail URLs.

### 2.2 The `next` Endpoint (Song Radio / Related Tracks)

- **Method:** `innerTube.next(client = YouTubeClient.WEB_REMIX, videoId = trackId)`
- **Alternative / Radio Form:** `innerTube.next(client = YouTubeClient.WEB_REMIX, videoId = trackId, playlistId = "RDAMVM$trackId")`
- **Authentication:** None required.
- **Returned Data:**
  - YouTube Music's native automix queue for that seed track.
  - Root container: `contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs[0].tabRenderer.content.musicQueueRenderer.content.playlistPanelRenderer.contents`
  - Item renderer: `playlistPanelVideoRenderer`.
  - Each item includes:
    - `videoId`: The target playable audio track ID.
    - `title.runs[0].text`: The official song title.
    - `shortBylineText.runs[*].text`: Artist name and duration text.
    - `lengthText.runs[0].text`: Exact duration (e.g. `3:45`).
    - `thumbnail.thumbnails`: Image URLs across resolutions.

### 2.3 The `search` Endpoint with WEB_REMIX

- **Method:** `innerTube.search(client = YouTubeClient.WEB_REMIX, query = query)`
- **Behavior:** Querying with `WEB_REMIX` targets YouTube Music's music index directly, prioritizing official albums, singles, and tracks over YouTube's general video corpus.

---

## 3. NON-MUSIC CONTENT FILTERING HEURISTICS

YouTube Music contains occasional non-music uploads (podcast episodes, talk show interviews, reaction clips, unboxing videos, audiobooks). STEREO introduces a dedicated `ContentFilter` module with two user-configurable modes:

1. **`ContentFilterMode.MUSIC_ONLY` (Default & Recommended):**
   - **Exclusion Keywords (Regex & Tokenized):**
     - Case-insensitive blacklist in track title and artist name:
       - `interview`, `podcast`, `reaction`, `review`, `audiobook`, `audio book`, `commentary`, `unboxing`, `tutorial`, `gameplay`, `live stream`, `q&a`, `teaser`, `trailer`, `vlog`.
   - **Duration Boundaries:**
     - Reject items with `durationSeconds < 30` (stings, notification chimes, soundbites).
     - Reject items with `durationSeconds > 1200` (20 minutes+) unless explicitly titled "DJ Mix", "Continuous Mix", "Live Set", or "Full Album".
   - **Artist Validation:**
     - Reject channels known for reaction or commentary content (e.g., channels ending in "TV", "Reacts", "Clips", "Podcast").

2. **`ContentFilterMode.ALL_CONTENT` (User Opt-In):**
   - Preserves all incoming tracks without algorithmic filtering for users who explicitly wish to listen to spoken word or longform recordings.

---

## 4. STEREO RECOMMENDATION PIPELINE ARCHITECTURE

```
+---------------------------------------------------------------------------------+
|                                 USER ACTIVITY                                   |
|   (Likes +30, Skips -20, Listens >30s +30, Onboarding Genres & Languages)       |
+---------------------------------------------------------------------------------+
                                        │
                                        ▼
+---------------------------------------------------------------------------------+
|                          LISTENING PROFILE & SEEDS                              |
|   - Top Artists (weighted by recency & repeat count)                            |
|   - Top Seed Track IDs (favorite & recently looped songs)                       |
|   - Excluded Track IDs (explicit "Don't Recommend", skipped within 5s)          |
+---------------------------------------------------------------------------------+
                                        │
                                        ▼
+---------------------------------------------------------------------------------+
|                       CATALOG CANDIDATE HARVESTING                              |
|                                                                                 |
|   [Source 1: Song Radio via innerTube.next(seedTrackId)]                        |
|   - Fetches authentic YouTube Music automix graph for top 2-3 seed tracks       |
|                                                                                 |
|   [Source 2: Curated Discovery via innerTube.browse("FEmusic_home")]            |
|   - Fetches trending and genre shelves matching user preferred genres           |
|                                                                                 |
|   [Source 3: Targeted Catalog Query via innerTube.search()]                     |
|   - Supplemented only when cold start or candidate pool < 30                    |
+---------------------------------------------------------------------------------+
                                        │
                                        ▼
+---------------------------------------------------------------------------------+
|                           CONTENT FILTERING                                     |
|   - Apply ContentFilter (MUSIC_ONLY mode)                                       |
|   - Suppress interviews, reactions, podcasts, spoken word                       |
+---------------------------------------------------------------------------------+
                                        │
                                        ▼
+---------------------------------------------------------------------------------+
|                    SCORING, TASTE WEIGHTING & SECTIONS                          |
|                                                                                 |
|   1. QUICK PICKS: Blends recent top favorites + high-confidence automix seeds   |
|   2. DAILY MIX: Themed cluster mixes (genre/language based)                     |
|   3. RECENT: Pure chronologically ordered playback history                      |
|   4. WEEKLY DISCOVER: Unheard tracks from related artist radio                  |
|   5. BECAUSE YOU LISTENED: Song radio focused on a specific beloved seed track  |
|                                                                                 |
|   STRICT DEDUPLICATION: `consumedTrackIds` registry guarantees 0 cross-section  |
|   duplicates across the entire Home feed.                                       |
+---------------------------------------------------------------------------------+
```

---

## 5. SYNTHETIC TASTE PROFILE BENCHMARKS

To validate recommendation fidelity prior to physical device deployment, the pipeline is evaluated across five distinct synthetic listener profiles:

### Profile A: English Pop Enthusiast
- **Taste Seeds:** Dua Lipa, Harry Styles, Taylor Swift, The Weeknd.
- **Preferred Genres:** Pop, Dance-Pop, Synth-Pop.
- **Languages:** English.
- **Acceptance Criteria:**
  - Quick Picks must feature upbeat pop tracks.
  - Daily Mix must group contemporary pop and synth-wave.
  - Zero spoken-word podcasts or interview clips.

### Profile B: Telugu Film Music Fan
- **Taste Seeds:** Anirudh Ravichander, Devi Sri Prasad, Thaman S, Sid Sriram.
- **Preferred Genres:** Telugu Cinema, Film Score, Melody.
- **Languages:** Telugu.
- **Acceptance Criteria:**
  - Recommendations must prioritize authentic Telugu tracks and film albums.
  - No generic Hindi/English chart bleed into Telugu-specific shelves.

### Profile C: Electronic / EDM Purist
- **Taste Seeds:** Deadmau5, Rufus Du Sol, Eric Prydz, Lane 8.
- **Preferred Genres:** Melodic House, Techno, Progressive, EDM.
- **Languages:** English, Instrumental.
- **Acceptance Criteria:**
  - Candidates must focus on electronic releases, club edits, and label tracks.
  - Acoustic ballad suppression.

### Profile D: Mixed-Language Explorer
- **Taste Seeds:** A.R. Rahman, Coldplay, Diljit Dosanjh, Daft Punk.
- **Preferred Genres:** Pop, Indie, World, Electronic.
- **Languages:** English, Hindi, Punjabi.
- **Acceptance Criteria:**
  - Balanced multi-lingual shelves without one language drowning out others.
  - Distinct mixes grouped by language/vibe.

### Profile E: New User (Cold Start)
- **Taste Seeds:** None (empty listening history).
- **Onboarding Selections:** English & Hindi, Genres: Pop & Rock.
- **Acceptance Criteria:**
  - Rapid pop/rock onboarding shelves populated within 1.5 seconds.
  - Zero crashes, zero empty sections, smooth first-session onboarding.

---

## 6. IMPLEMENTATION ROADMAP

1. **InnerTubeXExtractor Extension:**
   - Add `getSongRadio(trackId: String): Result<List<Track>>`.
   - Add `getHomeShelves(): Result<List<RecommendationSection>>`.
2. **ContentFilter Module:**
   - Implement `ContentFilter.filterTracks(tracks: List<Track>, mode: ContentFilterMode): List<Track>`.
3. **CatalogCandidateProvider Refinement:**
   - Combine Song Radio seeds from user's top liked/recent tracks with browse home shelves.
4. **Unit Test Verification:**
   - Execute test suites validating candidate generation and content filtering across synthetic profiles.
