package com.flowsmith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Loads the organisation knowledge base (patterns.txt) into Pattern objects.
 * This is the agent's "learning" step: it ingests the org's reusable patterns,
 * naming conventions and standards before reasoning about a requirement.
 *
 * Dependency-free: a tiny key=value block parser, no JSON library required.
 */
public class Catalog {

    private final List<Pattern> patterns = new ArrayList<>();

    public static Catalog load(Path file) throws IOException {
        Catalog c = new Catalog();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8))) {
            Pattern cur = null;
            String line;
            while ((line = r.readLine()) != null) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("#")) continue;
                if (t.equals("[pattern]")) {
                    cur = new Pattern();
                    c.patterns.add(cur);
                    continue;
                }
                int eq = t.indexOf('=');
                if (eq < 0 || cur == null) continue;
                String key = t.substring(0, eq).trim();
                String val = t.substring(eq + 1).trim();
                switch (key) {
                    case "id":              cur.id = val; break;
                    case "title":           cur.title = val; break;
                    case "integrationType": cur.integrationType = val; break;
                    case "connectivity":    cur.connectivity = val; break;
                    case "templateDir":     cur.templateDir = val; break;
                    case "appProject":      cur.appProject = val; break;
                    case "requiredTokens":  cur.requiredTokens = splitCsv(val); break;
                    case "optionalTokens":  cur.optionalTokens = splitCsv(val); break;
                    case "keywords":        cur.keywords = splitCsv(val); break;
                    case "description":     cur.description = val; break;
                    default: /* ignore unknown keys */ break;
                }
            }
        }
        return c;
    }

    private static List<String> splitCsv(String v) {
        if (v == null || v.trim().isEmpty()) return new ArrayList<>();
        return Arrays.stream(v.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public List<Pattern> patterns() {
        return patterns;
    }

    public Pattern byId(String id) {
        for (Pattern p : patterns) {
            if (p.id.equalsIgnoreCase(id)) return p;
        }
        return null;
    }
}
