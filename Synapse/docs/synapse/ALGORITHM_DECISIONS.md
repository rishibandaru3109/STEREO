# SYNAPSE Algorithm Decisions & Architecture Matrix

**Project:** SYNAPSE Recommendation Engine  
**Host Application:** STEREO (`com.nova.music`)  
**Document:** `docs/synapse/ALGORITHM_DECISIONS.md`  
**Date:** November 2026  
**Status:** Approved Architectural Baseline  

---

## 1. Decision Classification Framework

Every proposed algorithm, model architecture, and heuristic is evaluated against five engineering criteria:
1. **Predictive & Discovery Quality:** Does it meaningfully improve user satisfaction, serendipity, or ranking accuracy?
2. **Computational Feasibility on Mobile:** Can it execute in under $10\text{ ms}$ on an ARM64 Android device without draining battery or exceeding $25\text{ MB}$ RAM?
3. **Infrastructure Budget ($\mathbf{₹0}$):** Does it operate without paid external servers, third-party subscriptions, or proprietary API keys?
4. **Data Availability in STEREO:** Can it function using local listening events and unauthenticated YouTube Music / InnerTubeX metadata?
5. **Privacy & Local-First Isolation:** Does it keep the user's private listening habits safely on their personal device?

Based on these criteria, each component is classified into one of four states:
- **[TIER 1 - IMMEDIATE]:** Fully implemented in SYNAPSE V1 core production engine.
- **[TIER 2 - EXPERIMENTAL]:** Evaluated and benchmarked offline in Milestone 5 against the baseline.
- **[TIER 3 - DEFERRED]:** Architecturally preserved via interfaces; deferred until large-scale user interaction telemetry exists.
- **[TIER 4 - REJECTED]:** Prohibited due to memory explosion, prohibitive latency, licensing violations, or unverified claims.

---

## 2. Algorithm Classification Matrix

| Subsystem / Algorithm | Origin / Reference | Decision Status | Primary Rationale & Mathematical Justification |
|---|---|---|---|
| **Bounded Listening Clamping ($\kappa_{cz}$)** | Lu & Wu (2025), Eq. 9–10 | **[TIER 1 - IMMEDIATE]** | Prevents long tracks (10-minute symphonies) from drowning out short tracks (2-minute pop songs). Uses $\delta=5\text{s}$ noise floor and $\chi=180\text{s}$ saturation cap. |
| **Normalized Category Interest ($Int_{cz}$)** | Lu & Wu (2025), Eq. 11 | **[TIER 1 - IMMEDIATE]** | Mathematically computes user preference distribution across genres/languages; forms the basis for calibrated recommendations. |
| **Dual-Horizon Preference Modeling** | Lu & Wu (2025) & STEREO Arch | **[TIER 1 - IMMEDIATE]** | Separates persistent long-term taste ($\lambda = 0.07$, half-life ~10 days) from short-term session intent ($\lambda = 0.5$, half-life ~1.4 hours). Prevents temporary sessions from corrupting permanent profiles. |
| **Contextual Skip Intelligence** | arXiv:2301.03881 & arXiv:2406.04488 | **[TIER 1 - IMMEDIATE]** | Distinguishes immediate dislike ($<10\text{s}$) from mood mismatch ($10\text{--}30\text{s}$), queue seeking (burst skips), and outro transitions ($>80\%$). A skip is not an automatic dislike. |
| **Maximal Marginal Relevance (MMR)** | Carbonell & Goldstein (1998) | **[TIER 1 - IMMEDIATE]** | Balances relevance vs diversity. Powered by the user's **Familiar $\leftrightarrow$ Adventurous** discovery slider. Enforces max consecutive tracks per artist $\le 2$. |
| **Strict Language Eligibility Filter** | STEREO Spec & InnerTubeX | **[TIER 1 - IMMEDIATE]** | Hard candidate filter enforcing explicit language choices (Telugu, Hindi, English, etc.) with safe graceful fallback if candidate pool is exhausted. |
| **Lazy Metadata Similarity Vectorizer** | Ref B (GFG) & Ref C (Kaggle) | **[TIER 1 - IMMEDIATE]** | Calculates token overlap and normalized feature distance on-the-fly over candidate pool ($K \le 100$). Zero precomputed global matrix. $O(K \cdot D)$ memory footprint. |
| **Explainable Scoring Engine** | RecSys Best Practices | **[TIER 1 - IMMEDIATE]** | Every recommended track carries an authentic explanation ("Because you love [Artist]", "Matches your current session vibe", "Fresh discovery in [Genre]"). No fake explanations. |
| **GRU4Rec Sequential Model** | Hidasi et al. (2015) | **[TIER 2 - EXPERIMENTAL]** | Implemented and benchmarked in Milestone 5 Python suite. Evaluates if recurrent state transition outperforms decayed linear heuristics on session-based next-track prediction. |
| **SASRec (Self-Attentive Recommender)** | Kang & McAuley (2018) | **[TIER 2 - EXPERIMENTAL]** | Benchmarked in Milestone 5 to quantify attention vs GRU tradeoff on small sequence lengths ($L=20$). |
| **Contextual Bandits (LinUCB / Thompson)** | Li et al. (2010), arXiv:1003.0146 | **[TIER 2 - EXPERIMENTAL]** | Adds exploration variance bonus to long-tail discovery candidates; tested in offline simulation before live activation. |
| **LightFM Hybrid Factorization** | Kula (2015) | **[TIER 3 - DEFERRED]** | Requires cross-user collaborative interaction logs. In a local-first single-user Android client, collaborative filtering cannot function until opt-in federated learning or privacy-preserving telemetry is built. |
| **Implicit Alternating Least Squares (iALS)** | Hu, Koren, Volinsky (2008) | **[TIER 3 - DEFERRED]** | Matrix factorization over user-item matrix requires thousands of user rows. Kept as an offline benchmark algorithm. |
| **Full Acoustic Transformer (MERT / MERT-v2)** | Liang et al. (2024), HF m-a-p | **[TIER 4 - REJECTED]** | $1.3\text{ GB}$ model size and multi-second CPU inference violate Android mobile resource limits and ₹0 infrastructure budget. Replaced with lightweight metadata/graph heuristics. |
| **Global Pairwise Cosine Matrix ($N \times N$)** | Ref B (GeeksforGeeks) | **[TIER 4 - REJECTED]** | Requires $O(N^2)$ RAM. 20,000 tracks $= 1.6\text{ GB}$, guaranteeing an immediate Out-Of-Memory crash on Android. |
| **Spotify Audio Features Reliance** | Ref A & Ref C | **[TIER 4 - REJECTED]** | Spotify API deprecated its audio features endpoint in 2024; YouTube Music provides no danceability/valence metrics. SYNAPSE must not depend on unavailable external features. |
| **TSMC 65nm Multi-Core Hardware Design** | Lu & Wu (2025), Section 4 | **[TIER 4 - REJECTED]** | Irrelevant academic filler with zero applicability to Android runtime or software architecture. |
| **BERT4Rec Bidirectional Cloze Recommender** | Sun et al. (2019) | **[TIER 4 - REJECTED]** | Bidirectional token masking is computationally excessive and conceptually ill-suited for real-time sequential streaming prediction compared to unidirectional SASRec/GRU. |

---

## 3. Immediate Implementation Blueprint (Tier 1)

### 3.1 Composite Scoring Function
For any candidate track $c$ in context $X$, the raw score $S(c \mid X)$ is given by:

$$S(c \mid X) = w_{\text{radio}} \cdot \mathbb{I}(c \in \text{Radio}) + w_{\text{artist}} \cdot \text{Affinity}_{\text{artist}}(c) + w_{\text{session}} \cdot \text{Sim}_{\text{session}}(c) + w_{\text{genre}} \cdot Int(g_c) - P_{\text{repetition}}(c) - P_{\text{skip}}(c)$$

Where:
1. $\mathbb{I}(c \in \text{Radio})$: Indicator if track was retrieved via seed song automix graph (proven contextual fit). Default weight: $+30.0$.
2. $\text{Affinity}_{\text{artist}}(c)$: User's exponential-decayed long-term artist score. Clamped in $[0, 50]$.
3. $\text{Sim}_{\text{session}}(c)$: Cosine token similarity to the current session centroid. Weighted by session recency.
4. $Int(g_c)$: Normalized category/genre engagement ratio from Lu & Wu Eq. (11).
5. $P_{\text{repetition}}(c)$: Repetition penalty for tracks played in recent sessions:
   $$P_{\text{repetition}}(c) = \beta \cdot \exp\left(-\frac{\Delta t}{\tau_{\text{repeat}}}\right)$$
6. $P_{\text{skip}}(c)$: Time-decayed skip penalty if the track or artist was recently skipped:
   $$P_{\text{skip}}(c) = \gamma \cdot \exp\left(-\frac{\Delta t}{\tau_{\text{skip}}}\right)$$

### 3.2 Discovery & MMR Transformation
To prevent echo chambers and popularity lock-in, the top ranked candidates are re-ordered using MMR parameterized by user discovery mode $\alpha \in [0.2, 0.9]$:
$$\text{RankScore}(c) = \alpha \cdot \frac{S(c \mid X) - S_{\min}}{S_{\max} - S_{\min}} - (1 - \alpha) \cdot \max_{s \in \text{Selected}} \text{Sim}_{\text{artist}}(c, s)$$
Artist diversity constraint: No more than 2 consecutive tracks from the same artist, and no more than 4 tracks from the same artist in any 20-track recommendation mix.
