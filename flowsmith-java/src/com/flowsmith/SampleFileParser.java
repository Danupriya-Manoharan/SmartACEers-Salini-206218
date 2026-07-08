package com.flowsmith;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.json.*;

/**
 * Parses sample XML and JSON files to extract field structures
 * for automatic mapping inference.
 */
public class SampleFileParser {
    
    /**
     * Parse XML file and extract all field paths
     * @param xmlFile Path to XML file
     * @return List of XML field paths (e.g., "customer.id", "customer.name")
     */
    public static List<String> parseXMLStructure(String xmlFile) throws Exception {
        List<String> fields = new ArrayList<>();
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(xmlFile));
        doc.getDocumentElement().normalize();
        
        // Start from root element
        Element root = doc.getDocumentElement();
        extractXMLFields(root, "", fields);
        
        return fields;
    }
    
    /**
     * Recursively extract XML field paths
     */
    private static void extractXMLFields(Element element, String parentPath, List<String> fields) {
        String currentPath = parentPath.isEmpty() ? element.getNodeName() : parentPath + "." + element.getNodeName();
        
        NodeList children = element.getChildNodes();
        boolean hasElementChildren = false;
        
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                hasElementChildren = true;
                extractXMLFields((Element) child, currentPath, fields);
            }
        }
        
        // If no element children, this is a leaf node (actual field)
        if (!hasElementChildren) {
            fields.add(currentPath);
        }
    }
    
    /**
     * Parse JSON file and extract all field paths
     * @param jsonFile Path to JSON file
     * @return List of JSON field paths (e.g., "customer.customerId", "customer.customerName")
     */
    public static List<String> parseJSONStructure(String jsonFile) throws Exception {
        List<String> fields = new ArrayList<>();
        
        // Read JSON file
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }
        
        JSONObject json = new JSONObject(content.toString());
        extractJSONFields(json, "", fields);
        
        return fields;
    }
    
    /**
     * Recursively extract JSON field paths
     */
    private static void extractJSONFields(Object obj, String parentPath, List<String> fields) {
        if (obj instanceof JSONObject) {
            JSONObject jsonObj = (JSONObject) obj;
            Iterator<String> keys = jsonObj.keys();
            
            while (keys.hasNext()) {
                String key = keys.next();
                String currentPath = parentPath.isEmpty() ? key : parentPath + "." + key;
                Object value = jsonObj.get(key);
                
                if (value instanceof JSONObject) {
                    extractJSONFields(value, currentPath, fields);
                } else if (value instanceof JSONArray) {
                    // For arrays, just note the array field
                    fields.add(currentPath);
                } else {
                    // Leaf node (actual field)
                    fields.add(currentPath);
                }
            }
        }
    }
    
    /**
     * Read sample data from XML file for testing
     */
    public static String readXMLSample(String xmlFile) throws Exception {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(xmlFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
    
    /**
     * Read sample data from JSON file for testing
     */
    public static String readJSONSample(String jsonFile) throws Exception {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
    
    /**
     * Test the parser with sample files
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java SampleFileParser <xml-file> <json-file>");
            return;
        }
        
        try {
            System.out.println("Parsing XML structure from: " + args[0]);
            List<String> xmlFields = parseXMLStructure(args[0]);
            System.out.println("\nXML Fields:");
            for (String field : xmlFields) {
                System.out.println("  " + field);
            }
            
            System.out.println("\nParsing JSON structure from: " + args[1]);
            List<String> jsonFields = parseJSONStructure(args[1]);
            System.out.println("\nJSON Fields:");
            for (String field : jsonFields) {
                System.out.println("  " + field);
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

// Made with Bob
