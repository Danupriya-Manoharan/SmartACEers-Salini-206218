package com.flowsmith;

import java.util.ArrayList;
import java.util.List;

/**
 * The current (rule-based) reasoning engine: a deterministic intent matcher.
 *
 * It scores each learned pattern by how strongly the requirement text overlaps
 * the pattern's intent keywords, with a boost for naming the integration type.
 * No model call, fully offline and explainable - a stand-in for the LLM that
 * will replace it behind the {@link Recommender} interface.
 */
public class KeywordRecommender implements Recommender {

    @Override
    public String engineName() {
        return "rule-based intent matcher (LLM stand-in)";
    }

    @Override
    public List<Recommendation> recommend(String requirement, Catalog catalog) {
        String text = requirement == null ? "" : requirement.toLowerCase();
        List<Recommendation> out = new ArrayList<>();

        for (Pattern p : catalog.patterns()) {
            int score = 0;
            List<String> hits = new ArrayList<>();

            for (String kw : p.keywords) {
                if (text.contains(kw)) {
                    // multi-word phrase matches weigh more than single words
                    int weight = 2 + countSpaces(kw);
                    score += weight;
                    hits.add(kw);
                }
            }
            // small boost for explicitly naming the integration type
            String itype = p.integrationType.toLowerCase();
            if (containsWord(text, itype)) {
                score += 1;
                hits.add(itype);
            }

            if (score > 0) {
                String rationale = "matched: " + String.join(", ", hits);
                out.add(new Recommendation(p, score, rationale));
            }
        }

        out.sort((a, b) -> Integer.compare(b.score, a.score));
        return out;
    }

    private static int countSpaces(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == ' ') n++;
        return n;
    }

    private static boolean containsWord(String text, String word) {
        // crude word-boundary check, dependency-free
        int i = text.indexOf(word);
        while (i >= 0) {
            boolean leftOk = (i == 0) || !Character.isLetterOrDigit(text.charAt(i - 1));
            int end = i + word.length();
            boolean rightOk = (end >= text.length()) || !Character.isLetterOrDigit(text.charAt(end));
            if (leftOk && rightOk) return true;
            i = text.indexOf(word, i + 1);
        }
        return false;
    }
}
