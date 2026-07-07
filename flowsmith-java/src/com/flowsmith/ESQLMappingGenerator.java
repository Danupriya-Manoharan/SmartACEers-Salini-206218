package com.flowsmith;

import java.util.List;

/**
 * Generates ESQL mapping code from a MappingDocument.
 * Creates field-by-field assignments for XML to JSON transformations.
 */
public class ESQLMappingGenerator {
    
    /**
     * Generate ESQL mapping statements for XML to JSON conversion.
     * 
     * @param mappings List of field mappings
     * @param sourceFormat Source format (e.g., "XMLNSC", "DFDL", "BLOB")
     * @param targetFormat Target format (e.g., "JSON", "XMLNSC")
     * @return ESQL code as a string
     */
    public static String generateMappingCode(List<MappingDocument.FieldMapping> mappings,
                                            String sourceFormat, String targetFormat) {
        StringBuilder esql = new StringBuilder();
        
        esql.append("\t\t-- Auto-generated field mappings from mapping document\n");
        esql.append("\t\t-- Source: ").append(sourceFormat).append(" -> Target: ").append(targetFormat).append("\n");
        esql.append("\t\t\n");
        
        // Initialize output message
        esql.append("\t\t-- Initialize output message\n");
        esql.append("\t\tCREATE FIELD OutputRoot.").append(targetFormat).append(";\n");
        esql.append("\t\tCREATE FIELD OutputRoot.").append(targetFormat).append(".Data;\n");
        esql.append("\t\t\n");
        
        // Generate mapping statements
        esql.append("\t\t-- Field mappings\n");
        for (MappingDocument.FieldMapping mapping : mappings) {
            String sourcePath = convertToESQLPath(mapping.sourceField, sourceFormat);
            String targetPath = convertToESQLPath(mapping.targetField, targetFormat);
            
            esql.append("\t\tSET ").append(targetPath).append(" = ").append(sourcePath).append(";\n");
        }
        
        return esql.toString();
    }
    
    /**
     * Generate complete ESQL compute module with mappings.
     * 
     * @param schema BROKER SCHEMA (e.g., "PTP.SUBSYS.APPNM.FUNCNM.FIL")
     * @param moduleName Module name (e.g., "Adapter_Compute")
     * @param mappings Field mappings
     * @param sourceFormat Source format
     * @param targetFormat Target format
     * @return Complete ESQL module code
     */
    public static String generateCompleteModule(String schema, String moduleName,
                                               List<MappingDocument.FieldMapping> mappings,
                                               String sourceFormat, String targetFormat) {
        StringBuilder esql = new StringBuilder();
        
        esql.append("BROKER SCHEMA ").append(schema).append("\n\n\n");
        esql.append("CREATE COMPUTE MODULE ").append(moduleName).append("\n");
        esql.append("\tCREATE FUNCTION Main() RETURNS BOOLEAN\n");
        esql.append("\tBEGIN\n");
        esql.append("\t\tCALL CopyMessageHeaders();\n");
        esql.append("\t\t\n");
        
        // Add mapping code
        esql.append(generateMappingCode(mappings, sourceFormat, targetFormat));
        
        esql.append("\t\t\n");
        esql.append("\t\tRETURN TRUE;\n");
        esql.append("\tEND;\n\n");
        
        // Add helper procedures
        esql.append("\tCREATE PROCEDURE CopyMessageHeaders() BEGIN\n");
        esql.append("\t\tDECLARE I INTEGER 1;\n");
        esql.append("\t\tDECLARE J INTEGER;\n");
        esql.append("\t\tSET J = CARDINALITY(InputRoot.*[]);\n");
        esql.append("\t\tWHILE I < J DO\n");
        esql.append("\t\t\tSET OutputRoot.*[I] = InputRoot.*[I];\n");
        esql.append("\t\t\tSET I = I + 1;\n");
        esql.append("\t\tEND WHILE;\n");
        esql.append("\tEND;\n");
        esql.append("END MODULE;\n");
        
        return esql.toString();
    }
    
    /**
     * Convert user-friendly field path to ESQL path.
     * Examples:
     *   "customer/name" -> "InputRoot.XMLNSC.customer.name"
     *   "customer.name" -> "OutputRoot.JSON.Data.customer.name"
     */
    private static String convertToESQLPath(String fieldPath, String format) {
        // Determine root based on format
        String root = format.equals("JSON") || format.equals("XMLNSC") ? 
                     "OutputRoot" : "InputRoot";
        
        // Normalize path separators (/ or . to .)
        String normalizedPath = fieldPath.replace('/', '.');
        
        // Remove leading/trailing dots
        normalizedPath = normalizedPath.replaceAll("^\\.|\\.$", "");
        
        // Build ESQL path
        if (format.equals("JSON")) {
            return root + "." + format + ".Data." + normalizedPath;
        } else {
            return root + "." + format + "." + normalizedPath;
        }
    }
    
    /**
     * Inject mapping code into existing ESQL template.
     * Replaces placeholder comments with actual mapping code.
     */
    public static String injectMappingsIntoTemplate(String templateContent,
                                                   List<MappingDocument.FieldMapping> mappings,
                                                   String sourceFormat, String targetFormat) {
        String mappingCode = generateMappingCode(mappings, sourceFormat, targetFormat);
        
        // Look for common placeholder patterns
        String[] placeholders = {
            "-- CALL CopyEntireMessage();",
            "-- Add your mapping code here",
            "-- TODO: Add field mappings",
            "CALL CopyEntireMessage();"
        };
        
        for (String placeholder : placeholders) {
            if (templateContent.contains(placeholder)) {
                return templateContent.replace(placeholder, mappingCode.trim());
            }
        }
        
        // If no placeholder found, insert before RETURN TRUE
        return templateContent.replace("RETURN TRUE;", 
                                      mappingCode + "\t\tRETURN TRUE;");
    }
}

// Made with Bob
