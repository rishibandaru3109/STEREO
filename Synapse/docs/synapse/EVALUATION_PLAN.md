# SYNAPSE Evaluation Methodology & Benchmark Plan

**Project:** SYNAPSE Recommendation Engine  
**Host Application:** STEREO (`com.nova.music`)  
**Document:** `docs/synapse/EVALUATION_PLAN.md`  
**Date:** November 2026  
**Status:** Approved Experimental Design  

---

## 1. Evaluation Philosophy

Recommendation quality cannot be measured by a single classification accuracy percentage. In real-world music listening, accuracy without diversity creates an echo chamber, while novelty without relevance creates user frustration.

SYNAPSE establishes a multi-dimensional, reproducible evaluation suite measuring:
1. **Ranking Quality & Accuracy:** Did the user want to listen to the recommended tracks?
2. **Diversity & Serendipity:** Are the tracks artistically varied, preventing monopolization by a single popular artist?
3. **Behavioral Responsiveness:** Does the engine adapt when the user skips or changes mood?
4. **On-Device Efficiency:** Latency, CPU spikes, and memory footprint on mobile hardware.

---

## 2. Quantitative Evaluation Metrics

### 2.1 Relevance & Ranking Metrics

- **Recall@K:**
  $$\text{Recall}@K = \frac{|\text{Top-}K \cap \text{Relevant}|}{|\text{Relevant}|}$$
- **Normalized Discounted Cumulative Gain (NDCG@K):**
  $$\text{DCG}@K = \sum_{i=1}^K \frac{2^{rel_i} - 1}{\log_2(i + 1)}, \quad \text{NDCG}@K = \frac{\text{DCG}@K}{\text{IDCG}@K}$$
- **Hit Rate@K (HR@K):**
  $$\text{HR}@K = \mathbb{I}(|\text{Top-}K \cap \text{Relevant}| > 0)$$
- **Mean Reciprocal Rank (MRR):**
  $$\text{MRR} = \frac{1}{|U|} \sum_{u=1}^{|U|} \frac{1}{\text{rank}_u^*}$$

### 2.2 Diversity & Novelty Metrics

- **Intra-List Diversity (ILD):**
  $$\text{ILD}(R) = \frac{2}{|R|(|R| - 1)} \sum_{i=1}^{|R|} \sum_{j=i+1}^{|R|} \text{Distance}(\mathbf{t}_i, \mathbf{t}_j)$$
  Where $\text{Distance} = 1 - \text{CosineSimilarity}(\mathbf{t}_i, \mathbf{t}_j)$.
- **Artist Entropy / Coverage:**
  $$H_{\text{artist}}(R) = -\sum_{a \in \text{Artists}(R)} p(a) \log_2 p(a)$$
- **Novelty (Self-Information):**
  $$\text{Novelty}(R) = \frac{1}{|R|} \sum_{t \in R} -\log_2 P_{\text{catalog}}(t)$$
- **Repetition Rate:** Fraction of tracks recommended in the current turn that were listened to within the preceding 24 hours.

### 2.3 System & Mobile Performance Constraints

| Performance Dimension | Target Threshold | Hard Boundary |
|---|---|---|
| **P50 Latency (Ranking 100 Candidates)** | $\le 4\text{ ms}$ | $\le 10\text{ ms}$ |
| **P99 Latency (Cold-Start + Diversity)** | $\le 12\text{ ms}$ | $\le 25\text{ ms}$ |
| **Peak Heap Allocation during Ranking** | $\le 2.5\text{ MB}$ | $\le 8.0\text{ MB}$ |
| **Storage per 10,000 History Events** | $\le 1.2\text{ MB}$ | $\le 3.5\text{ MB}$ |
| **Main Thread Blocking Time** | $0.0\text{ ms}$ (100% Coroutine Dispatcher.Default) | $0.0\text{ ms}$ |

---

## 3. Standardized Comparative Baselines

To prove measurable superiority, SYNAPSE is benchmarked against four industry baselines under identical candidate pools and chronological splits:

1. **Baseline 1 — Most Popular Candidates (Popularity):** Ranks candidates solely by global/catalog play frequency.
2. **Baseline 2 — Top Artist / Recent Bias (Heuristic):** Strictly prioritizes tracks by the user's most played artist.
3. **Baseline 3 — Metadata Cosine Similarity Only:** Unweighted TF-IDF metadata similarity without behavioral feedback or skip modeling.
4. **Baseline 4 — Provider Automix Unchanged:** The raw order returned by YouTube Music / InnerTubeX song radio.
5. **SYNAPSE Core Hybrid Recommender:** Our multi-factor adaptive scoring engine with MMR diversity and contextual skip intelligence.

---

## 4. Ablation Study Methodology

To verify the necessity of every sub-component, we conduct four ablation experiments:
- **Ablation A:** Remove Duration Clamping ($\kappa_{cz} \to 1.0$). Tests short vs long track bias.
- **Ablation B:** Remove Contextual Skip Discounting (Treat all skips as $-30.0$ flat). Tests whether differentiating $<10\text{s}$ vs $>80\%$ prevents false negative penalties.
- **Ablation C:** Remove Dual-Horizon Taste Profiles (Use only a single decaying profile). Tests whether rapid session shifts erase persistent taste.
- **Ablation D:** Remove MMR Diversity Re-ranking (Raw score sorting only). Measures artist clustering and repetition degradation.
