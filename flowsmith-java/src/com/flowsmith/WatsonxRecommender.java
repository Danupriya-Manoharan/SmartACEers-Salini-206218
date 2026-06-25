package com.flowsmith;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 *  AI INTEGRATION POINT - live implementation
 * ============================================================================
 *  Reasoning engine backed by IBM watsonx.ai (a Granite foundation model).
 *
 *  Flow:
 *    1. exchange the IBM Cloud API key for an IAM bearer token
 *    2. send the requirement + the learned patterns to the model
 *    3. the model replies with the best pattern id
 *
 *  Dependency-free: built-in HttpsURLConnection + hand-built/parsed JSON, so it
 *  drops straight into the existing jar with no SDK.
 *
 *  Robust by design: if watsonx is not configured or the call fails, it falls
 *  back to the rule-based {@link KeywordRecommender} and annotates why - so a
 *  live demo never breaks.
 * ============================================================================
 */
public class WatsonxRecommender implements Recommender {

    private static final String IAM_URL = "https://iam.cloud.ibm.com/identity/token";
    private static final String GEN_PATH = "/ml/v1/text/generation?version=2023-05-29";

    private final WatsonxConfig cfg;
    private final Recommender fallback;

    public WatsonxRecommender(WatsonxConfig cfg, Recommender fallback) {
        this.cfg = cfg;
        this.fallback = fallback;
    }

    @Override
    public String engineName() {
        return "IBM watsonx.ai (" + cfg.modelId + ")  [rule-based fallback if unavailable]";
    }

    @Override
    public List<Recommendation> recommend(String requirement, Catalog catalog) {
        if (!cfg.isComplete()) {
            return fallbackWith(requirement, catalog, "watsonx not configured");
        }
        try {
            String token = iamToken(cfg.apikey);
            String reply = generate(token, buildPrompt(requirement, catalog));
            Pattern p = matchPattern(reply, catalog);
            if (p == null) {
                return fallbackWith(requirement, catalog,
                        "model reply not recognised: \"" + reply.trim() + "\"");
            }
            List<Recommendation> out = new ArrayList<>();
            out.add(new Recommendation(p, 10,
                    "watsonx.ai selected '" + p.id + "' (model reply: \"" + reply.trim() + "\")"));
            return out;
        } catch (Exception e) {
            return fallbackWith(requirement, catalog, "watsonx call failed: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------- prompt
    private String buildPrompt(String requirement, Catalog catalog) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an integration pattern classifier for IBM App Connect Enterprise.\n");
        sb.append("Choose the SINGLE best pattern for the requirement.\n\n");
        sb.append("Patterns:\n");
        for (Pattern p : catalog.patterns()) {
            sb.append("- ").append(p.id).append(": ").append(p.title)
              .append(". ").append(p.description).append("\n");
        }
        sb.append("\nRequirement: \"").append(requirement).append("\"\n\n");
        sb.append("Answer with ONLY the pattern id from the list above, nothing else.\n");
        return sb.toString();
    }

    /** Map the model's free text back to a catalog pattern (first id mentioned). */
    private Pattern matchPattern(String reply, Catalog catalog) {
        if (reply == null) return null;
        String r = reply.toLowerCase();
        Pattern best = null;
        int bestPos = Integer.MAX_VALUE;
        for (Pattern p : catalog.patterns()) {
            int idx = r.indexOf(p.id.toLowerCase());
            if (idx >= 0 && idx < bestPos) { bestPos = idx; best = p; }
        }
        return best;
    }

    // ------------------------------------------------------------- watsonx HTTP
    private String iamToken(String apikey) throws Exception {
        String body = "grant_type=" + URLEncoder.encode(
                "urn:ibm:params:oauth:grant-type:apikey", "UTF-8")
                + "&apikey=" + URLEncoder.encode(apikey, "UTF-8");
        String resp = httpPost(IAM_URL, body,
                "application/x-www-form-urlencoded", null);
        String token = jsonStringValue(resp, "access_token");
        if (token == null) throw new IllegalStateException("no access_token in IAM response");
        return token;
    }

    private String generate(String token, String prompt) throws Exception {
        String json = "{"
                + "\"model_id\":\"" + jsonEscape(cfg.modelId) + "\","
                + "\"project_id\":\"" + jsonEscape(cfg.projectId) + "\","
                + "\"input\":\"" + jsonEscape(prompt) + "\","
                + "\"parameters\":{\"decoding_method\":\"greedy\",\"max_new_tokens\":12,"
                + "\"repetition_penalty\":1}"
                + "}";
        String resp = httpPost(trimSlash(cfg.url) + GEN_PATH, json,
                "application/json", token);
        String text = jsonStringValue(resp, "generated_text");
        if (text == null) throw new IllegalStateException("no generated_text in watsonx response");
        return text;
    }

    private static String httpPost(String urlStr, String body, String contentType,
                                   String bearer) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(urlStr).openConnection();
        con.setRequestMethod("POST");
        con.setConnectTimeout(15000);
        con.setReadTimeout(30000);
        con.setRequestProperty("Content-Type", contentType);
        con.setRequestProperty("Accept", "application/json");
        if (bearer != null) con.setRequestProperty("Authorization", "Bearer " + bearer);
        con.setDoOutput(true);
        try (OutputStream os = con.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        int code = con.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? con.getInputStream() : con.getErrorStream();
        String resp = readAll(is);
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("HTTP " + code + " from " + urlStr + ": " + resp);
        }
        return resp;
    }

    private static String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) >= 0) bos.write(buf, 0, n);
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------- tiny JSON
    private static String jsonEscape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Extract the string value of the first occurrence of "key":"value". */
    static String jsonStringValue(String json, String key) {
        if (json == null) return null;
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return null;
        i = json.indexOf(':', i);
        if (i < 0) return null;
        i++;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length() || json.charAt(i) != '"') return null;
        i++;
        StringBuilder sb = new StringBuilder();
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char n = json.charAt(i + 1);
                switch (n) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '"': sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/');  break;
                    default: sb.append(n);
                }
                i += 2;
                continue;
            }
            if (c == '"') break;
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private static String trimSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    // ------------------------------------------------------------- fallback
    private List<Recommendation> fallbackWith(String req, Catalog catalog, String why) {
        String reason = why == null ? "" : why.replaceAll("\\s+", " ").trim();
        if (reason.length() > 120) reason = reason.substring(0, 117) + "...";
        List<Recommendation> base = fallback.recommend(req, catalog);
        List<Recommendation> out = new ArrayList<>();
        for (Recommendation r : base) {
            out.add(new Recommendation(r.pattern, r.score,
                    "[rule-based fallback - " + reason + "] " + r.rationale));
        }
        return out;
    }
}
