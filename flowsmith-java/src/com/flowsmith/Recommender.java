package com.flowsmith;

import java.util.List;

/**
 * ============================================================================
 *  AI INTEGRATION POINT
 * ============================================================================
 *  This is the single seam where the "intelligence" of ACE FlowSmith lives.
 *
 *  Given a plain-English functional requirement and the organisation's learned
 *  patterns, a Recommender returns a ranked list of candidate patterns.
 *
 *  TODAY:  KeywordRecommender - a deterministic intent matcher (no model call).
 *          It is a faithful STAND-IN for the language model, at the exact spot
 *          the model will sit.
 *
 *  LATER:  WatsonxRecommender / GraniteRecommender / ClaudeRecommender - call an
 *          LLM (e.g. IBM watsonx.ai Granite) to interpret the requirement and,
 *          eventually, compose brand-new subflows. Swapping the implementation
 *          here changes NOTHING else in the agent: same interface, same loop.
 *
 *  i.e. AI is not bolted on later - the architecture is already agentic
 *  (perceive -> reason -> act -> human review); only this reasoning component
 *  is upgraded from rules to a model.
 * ============================================================================
 */
public interface Recommender {

    /** One scored candidate produced by the reasoning step. */
    class Recommendation {
        public final Pattern pattern;
        public final int score;
        public final String rationale;
        public Recommendation(Pattern pattern, int score, String rationale) {
            this.pattern = pattern;
            this.score = score;
            this.rationale = rationale;
        }
    }

    /**
     * @param requirement plain-English description of what to build
     * @param catalog     the organisation's learned patterns
     * @return candidates ranked best-first (may be empty if nothing matches)
     */
    List<Recommendation> recommend(String requirement, Catalog catalog);

    /** Human-readable name of the reasoning engine (shown in the AI trace). */
    String engineName();
}
