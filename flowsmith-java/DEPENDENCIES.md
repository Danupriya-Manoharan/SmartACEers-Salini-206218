# FlowSmith Dependencies

## Required Libraries

### Apache POI (for Excel mapping support)

The mapping feature requires Apache POI to read Excel (.xlsx) files.

**Required JARs:**
- `poi-5.2.3.jar` - Core Apache POI library
- `poi-ooxml-5.2.3.jar` - OOXML support for .xlsx files
- `poi-ooxml-schemas-4.1.2.jar` - OOXML schemas
- `xmlbeans-5.1.1.jar` - XML beans dependency
- `commons-compress-1.21.jar` - Compression support
- `commons-collections4-4.4.jar` - Collections utilities

## Installation Options

### Option 1: Maven (Recommended)

Add to your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi</artifactId>
        <version>5.2.3</version>
    </dependency>
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
        <version>5.2.3</version>
    </dependency>
</dependencies>
```

### Option 2: Manual Download

1. Download Apache POI from: https://poi.apache.org/download.html
2. Extract the archive
3. Add all JARs from the `lib` directory to your classpath

### Option 3: Gradle

Add to your `build.gradle`:

```gradle
dependencies {
    implementation 'org.apache.poi:poi:5.2.3'
    implementation 'org.apache.poi:poi-ooxml:5.2.3'
}
```

## Compilation

### With Dependencies in lib/ folder:

```bash
# Compile
javac -cp "lib/*" -d bin src/com/flowsmith/*.java

# Create JAR
jar cvfm flowsmith.jar MANIFEST.MF -C bin .

# Run
java -cp "flowsmith.jar:lib/*" com.flowsmith.FlowSmith
```

### With Maven:

```bash
mvn clean compile
mvn package
java -jar target/flowsmith.jar
```

## Runtime Classpath

When running FlowSmith with mapping support, ensure Apache POI JARs are in the classpath:

```bash
java -cp "flowsmith.jar:lib/poi-5.2.3.jar:lib/poi-ooxml-5.2.3.jar:lib/poi-ooxml-schemas-4.1.2.jar:lib/xmlbeans-5.1.1.jar:lib/commons-compress-1.21.jar:lib/commons-collections4-4.4.jar" \
  com.flowsmith.FlowSmith generate --pattern ptp_file \
  --subsys XAJ --app TEST --func DEMO \
  --mapping mappings.xlsx
```

## Troubleshooting

### ClassNotFoundException: org.apache.poi.ss.usermodel.Workbook

**Solution:** Add Apache POI JARs to classpath

### NoClassDefFoundError: org/apache/xmlbeans/XmlException

**Solution:** Add xmlbeans JAR to classpath

### UnsupportedFileFormatException

**Solution:** Ensure you're using .xlsx format (not .xls)

## Alternative: CSV Support

If you cannot use Apache POI, you can:
1. Save your Excel file as CSV
2. Modify `MappingDocument.java` to parse CSV instead
3. No external dependencies required for CSV parsing

## License Compatibility

Apache POI is licensed under Apache License 2.0, which is compatible with most open-source and commercial projects.