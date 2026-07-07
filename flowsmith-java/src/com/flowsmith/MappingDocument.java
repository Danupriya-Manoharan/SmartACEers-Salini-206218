package com.flowsmith;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Parses an Excel mapping document (.xlsx) that defines field mappings
 * between source and target formats (e.g., XML to JSON).
 * 
 * Expected format:
 * Column A: Source field (e.g., XML tag like "customer/name")
 * Column B: Target field (e.g., JSON field like "customer.name")
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
     * Load mapping document from Excel file.
     * Reads first sheet, expects Column A = source, Column B = target.
     * Skips header row if present.
     */
    public static MappingDocument load(Path excelFile) throws IOException {
        MappingDocument doc = new MappingDocument();
        
        try (FileInputStream fis = new FileInputStream(excelFile.toFile());
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            boolean firstRow = true;
            
            for (Row row : sheet) {
                // Skip header row if it looks like a header
                if (firstRow) {
                    Cell firstCell = row.getCell(0);
                    if (firstCell != null) {
                        String val = getCellValue(firstCell).toLowerCase();
                        if (val.contains("source") || val.contains("xml") || 
                            val.contains("input") || val.contains("from")) {
                            firstRow = false;
                            continue;
                        }
                    }
                    firstRow = false;
                }
                
                Cell sourceCell = row.getCell(0);
                Cell targetCell = row.getCell(1);
                
                if (sourceCell == null || targetCell == null) continue;
                
                String source = getCellValue(sourceCell).trim();
                String target = getCellValue(targetCell).trim();
                
                if (!source.isEmpty() && !target.isEmpty()) {
                    doc.mappings.add(new FieldMapping(source, target));
                }
            }
        }
        
        return doc;
    }
    
    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
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
