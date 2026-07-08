package com.flowsmith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a CSV mapping document (.csv) that defines field mappings
 * between source and target formats (e.g., XML to JSON).
 *
 * Pure-Java implementation - no external libraries required, so the tool
 * builds and runs with just the JDK (no Apache POI needed).
 *
 * Expected format (two columns, comma-separated):
 *   Column A: Source field (e.g., XML tag like "customer/name")
 *   Column B: Target field (e.g., JSON field like "customer.name")
 */
public class MappingDocument {

    public static class FieldMapping {
        public String sourceField;
        public String targetField;

        public FieldMapping(String source, String target) {
            this.sourceField = source;
            this.targetField = target;
        }

        @Override
        public String toString() {
            return sourceField + " -> " + targetField;
        }
    }

    private final List<FieldMapping> mappings = new ArrayList<>();

    /**
     * Load mapping document from a CSV file.
     * Expects Column A = source, Column B = target. Skips a header row if present
     * and ignores blank lines.
     */
    public static MappingDocument load(Path csvFile) throws IOException {
        MappingDocument doc = new MappingDocument();

        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        boolean firstRow = true;

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) continue;

            String[] cols = splitCsv(line);
            if (cols.length < 2) continue;

            String source = cols[0].trim();
            String target = cols[1].trim();

            // Skip a header row if the first line looks like column titles
            if (firstRow) {
                firstRow = false;
                String val = source.toLowerCase();
                if (val.contains("source") || val.contains("xml") ||
                    val.contains("input") || val.contains("from")) {
                    continue;
                }
            }

            if (!source.isEmpty() && !target.isEmpty()) {
                doc.mappings.add(new FieldMapping(source, target));
            }
        }

        return doc;
    }

    /**
     * Minimal CSV field splitter. Handles optional double-quoted values
     * (with "" as an escaped quote) so a value containing a comma still works.
     * Field paths like "customer/id" have no commas, so this stays simple.
     */
    private static String[] splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;              // skip the escaped quote
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    out.add(cur.toString());
                    cur.setLength(0);
                } else {
                    cur.append(c);
                }
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    public List<FieldMapping> getMappings() {
        return mappings;
    }

    public boolean isEmpty() {
        return mappings.isEmpty();
    }

    public int size() {
        return mappings.size();
    }
}

// Made with Bob
