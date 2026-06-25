package com.flowsmith;

import java.util.List;

/**
 * One reusable ACE integration pattern, as learned from the organisation
 * knowledge base (patterns.txt). Plain data holder.
 */
public class Pattern {
    public String id;
    public String title;
    public String integrationType;   // PTP / PUB / SUB
    public String connectivity;      // FILE
    public String templateDir;       // relative to the repo root
    public String appProject;        // e.g. SUBSYS_PTP_APPNM_FUNCNM_FIL
    public List<String> requiredTokens;
    public List<String> optionalTokens;
    public List<String> keywords;    // intent terms used by the Recommender
    public String description;

    @Override
    public String toString() {
        return id + " (" + title + ")";
    }
}
