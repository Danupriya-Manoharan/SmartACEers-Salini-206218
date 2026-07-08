package com.flowsmith;

import java.util.*;

/**
 * Infers field mappings between XML and JSON based on sample files.
 * Uses simple 1:1 position-based mapping strategy.
 */
public class MappingInferencer {
    
    /**
     * Infer mappings from XML and JSON field lists using position-based matching
     * @param xmlFields List of XML field paths
     * @param jsonFields List of JSON field paths
     * @return Map of XML path -> JSON path mappings
     */
    public static Map<String, String> inferMappings(List<String> xmlFields, List<String> jsonFields) {
        Map<String, String> mappings = new LinkedHashMap<>();
        
        // Simple 1:1 position-based mapping
        int minSize = Math.min(xmlFields.size(), jsonFields.size());
        
        for (int i = 0; i < minSize; i++) {
            String xmlField = xmlFields.get(i);
            String jsonField = jsonFields.get(i);
            mappings.put(xmlField, jsonField);
        }
        
        // Warn if sizes don't match
        if (xmlFields.size() != jsonFields.size()) {
            System.out.println("WARNING: XML has " + xmlFields.size() + " fields but JSON has " + 
                             jsonFields.size() + " fields. Mapping first " + minSize + " fields only.");
        }
        
        return mappings;
    }
    
    /**
     * Infer mappings directly from sample files
     * @param xmlFile Path to sample XML file
     * @param jsonFile Path to sample JSON file
     * @return Map of XML path -> JSON path mappings
     */
    public static Map<String, String> inferMappingsFromFiles(String xmlFile, String jsonFile) throws Exception {
        System.out.println("Parsing XML structure from: " + xmlFile);
        List<String> xmlFields = SampleFileParser.parseXMLStructure(xmlFile);
        
        System.out.println("Parsing JSON structure from: " + jsonFile);
        List<String> jsonFields = SampleFileParser.parseJSONStructure(jsonFile);
        
        System.out.println("\nInferring mappings...");
        Map<String, String> mappings = inferMappings(xmlFields, jsonFields);
        
        System.out.println("\nInferred " + mappings.size() + " field mappings:");
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
        }
        
        return mappings;
    }
    
    /**
     * Convert inferred mappings to MappingDocument format for compatibility
     * @param mappings Map of XML path -> JSON path
     * @return MappingDocument with the mappings
     */
    public static MappingDocument toMappingDocument(Map<String, String> mappings) {
        MappingDocument doc = new MappingDocument();
        doc.setMappings(mappings);
        return doc;
    }
    
    /**
     * Generate ESQL code directly from sample files
     * @param xmlFile Path to sample XML file
     * @param jsonFile Path to sample JSON file
     * @param inputFormat Input format (e.g., "XMLNSC")
     * @param outputFormat Output format (e.g., "JSON")
     * @return Generated ESQL mapping code
     */
    public static String generateESQLFromSamples(String xmlFile, String jsonFile, 
                                                  String inputFormat, String outputFormat) throws Exception {
        Map<String, String> mappings = inferMappingsFromFiles(xmlFile, jsonFile);
        return ESQLMappingGenerator.generateMappingCode(mappings, inputFormat, outputFormat);
    }
    
    /**
     * Test the inferencer with sample files
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java MappingInferencer <xml-file> <json-file>");
            System.out.println("\nExample:");
            System.out.println("  java MappingInferencer sample-input.xml sample-output.json");
            return;
        }
        
        try {
            String xmlFile = args[0];
            String jsonFile = args[1];
            
            System.out.println("========================================");
            System.out.println("  Mapping Inference from Sample Files");
            System.out.println("========================================\n");
            
            // Infer mappings
            Map<String, String> mappings = inferMappingsFromFiles(xmlFile, jsonFile);
            
            // Generate ESQL
            System.out.println("\n========================================");
            System.out.println("  Generated ESQL Code");
            System.out.println("========================================\n");
            
            String esql = ESQLMappingGenerator.generateMappingCode(mappings, "XMLNSC", "JSON");
            System.out.println(esql);
            
            System.out.println("\n========================================");
            System.out.println("  Mapping inference complete!");
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

// Made with Bob
