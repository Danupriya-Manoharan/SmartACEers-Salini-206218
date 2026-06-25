package com.flowsmith;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Configuration for the watsonx.ai reasoning engine.
 *
 * Credentials are NEVER hardcoded or committed. They are read from (in order,
 * later sources win):
 *   1. a git-ignored file  flowsmith-java/watsonx.properties
 *   2. environment variables  WATSONX_URL / WATSONX_APIKEY / WATSONX_PROJECT_ID
 *                             / WATSONX_MODEL_ID
 *
 * See watsonx.properties.example for the file format.
 */
public class WatsonxConfig {

    public String url;        // e.g. https://us-south.ml.cloud.ibm.com
    public String apikey;     // IBM Cloud API key
    public String projectId;  // watsonx.ai project id
    public String modelId = "ibm/granite-3-8b-instruct";

    public boolean isComplete() {
        return notBlank(url) && notBlank(apikey) && notBlank(projectId);
    }

    public static WatsonxConfig load(Path home) {
        WatsonxConfig c = new WatsonxConfig();

        // 1. optional properties file next to the jar
        try {
            Path f = home.resolve("watsonx.properties");
            if (Files.exists(f)) {
                Properties p = new Properties();
                try (InputStream in = Files.newInputStream(f)) { p.load(in); }
                c.url       = p.getProperty("watsonx.url", c.url);
                c.apikey    = p.getProperty("watsonx.apikey", c.apikey);
                c.projectId = p.getProperty("watsonx.projectId", c.projectId);
                c.modelId   = p.getProperty("watsonx.modelId", c.modelId);
            }
        } catch (Exception ignored) { /* fall through to env */ }

        // 2. environment variables override the file
        c.url       = envOr("WATSONX_URL", c.url);
        c.apikey    = envOr("WATSONX_APIKEY", c.apikey);
        c.projectId = envOr("WATSONX_PROJECT_ID", c.projectId);
        c.modelId   = envOr("WATSONX_MODEL_ID", c.modelId);
        return c;
    }

    private static String envOr(String key, String cur) {
        String v = System.getenv(key);
        return (v != null && !v.trim().isEmpty()) ? v.trim() : cur;
    }

    private static boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }
}
