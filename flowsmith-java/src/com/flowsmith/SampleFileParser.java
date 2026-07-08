package com.flowsmith;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

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
        
        Object json = MiniJson.parse(content.toString());
        extractJSONFields(json, "", fields);

        return fields;
    }

    /**
     * Recursively extract JSON field paths (in document order).
     * Nested objects recurse; arrays and scalars are treated as leaf fields.
     */
    @SuppressWarnings("unchecked")
    private static void extractJSONFields(Object obj, String parentPath, List<String> fields) {
        if (obj instanceof Map) {
            for (Map.Entry<String, Object> e : ((Map<String, Object>) obj).entrySet()) {
                String currentPath = parentPath.isEmpty()
                        ? e.getKey() : parentPath + "." + e.getKey();
                Object value = e.getValue();
                if (value instanceof Map) {
                    extractJSONFields(value, currentPath, fields);
                } else {
                    // Arrays and scalars are leaf fields.
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

    /**
     * Minimal pure-JDK JSON parser (no external libraries). Parses objects,
     * arrays, strings, numbers, booleans and null. Objects preserve key order
     * (LinkedHashMap) so 1:1 position-based mapping stays deterministic.
     * Numbers/booleans are returned as-is; only structure/order matters here.
     */
    private static final class MiniJson {
        private final String s;
        private int i;

        private MiniJson(String s) { this.s = s; }

        static Object parse(String s) {
            MiniJson p = new MiniJson(s);
            p.ws();
            Object v = p.value();
            p.ws();
            return v;
        }

        private Object value() {
            ws();
            char c = peek();
            switch (c) {
                case '{': return object();
                case '[': return array();
                case '"': return string();
                case 't': case 'f': return bool();
                case 'n': expect("null"); return null;
                default:  return number();
            }
        }

        private Map<String, Object> object() {
            Map<String, Object> m = new LinkedHashMap<>();
            expect('{'); ws();
            if (peek() == '}') { i++; return m; }
            while (true) {
                ws();
                String key = string();
                ws(); expect(':');
                m.put(key, value());
                ws();
                char c = next();
                if (c == ',') continue;
                if (c == '}') break;
                throw err("expected ',' or '}'");
            }
            return m;
        }

        private List<Object> array() {
            List<Object> a = new ArrayList<>();
            expect('['); ws();
            if (peek() == ']') { i++; return a; }
            while (true) {
                a.add(value());
                ws();
                char c = next();
                if (c == ',') continue;
                if (c == ']') break;
                throw err("expected ',' or ']'");
            }
            return a;
        }

        private String string() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') break;
                if (c == '\\') {
                    char e = next();
                    switch (e) {
                        case '"':  sb.append('"');  break;
                        case '\\': sb.append('\\'); break;
                        case '/':  sb.append('/');  break;
                        case 'b':  sb.append('\b'); break;
                        case 'f':  sb.append('\f'); break;
                        case 'n':  sb.append('\n'); break;
                        case 'r':  sb.append('\r'); break;
                        case 't':  sb.append('\t'); break;
                        case 'u':
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                            break;
                        default: throw err("bad escape \\" + e);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private String number() {
            int start = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (i == start) throw err("unexpected character '" + peek() + "'");
            return s.substring(start, i);
        }

        private Boolean bool() {
            if (peek() == 't') { expect("true"); return Boolean.TRUE; }
            expect("false"); return Boolean.FALSE;
        }

        private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        private char peek() { if (i >= s.length()) throw err("unexpected end of input"); return s.charAt(i); }
        private char next() { if (i >= s.length()) throw err("unexpected end of input"); return s.charAt(i++); }
        private void expect(char c) { char g = next(); if (g != c) throw err("expected '" + c + "' but got '" + g + "'"); }
        private void expect(String tok) {
            for (int k = 0; k < tok.length(); k++) {
                if (next() != tok.charAt(k)) throw err("expected '" + tok + "'");
            }
        }
        private RuntimeException err(String m) { return new RuntimeException("JSON parse error at " + i + ": " + m); }
    }
}

// Made with Bob
