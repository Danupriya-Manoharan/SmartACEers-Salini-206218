package com.flowsmith;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory field-mapping document: an ordered list of source -> target field
 * pairs (e.g., XML tag "customer/id" -> JSON field "customer.customerId").
 *
 * Mappings are inferred 1:1 from a sample input XML and sample output JSON by
 * {@link MappingInferencer} - there is no mapping-file format to parse.
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

    public List<FieldMapping> getMappings() {
        return mappings;
    }
    
    /**
     * Set mappings from a Map (used by MappingInferencer)
     */
    public void setMappings(java.util.Map<String, String> mappingMap) {
        mappings.clear();
        for (java.util.Map.Entry<String, String> entry : mappingMap.entrySet()) {
            mappings.add(new FieldMapping(entry.getKey(), entry.getValue()));
        }
    }

    public boolean isEmpty() {
        return mappings.isEmpty();
    }

    public int size() {
        return mappings.size();
    }
}

// Made with Bob
