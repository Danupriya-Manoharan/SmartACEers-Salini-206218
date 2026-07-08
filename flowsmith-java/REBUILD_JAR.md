# Rebuilding flowsmith.jar

## ⚠️ Important: JAR Needs Rebuild

The `flowsmith.jar` file needs to be rebuilt to include the new mapping feature classes:
- `MappingDocument.java`
- `ESQLMappingGenerator.java`

## Prerequisites

- Java JDK 8 or higher installed
- `javac` compiler available in PATH
- Apache POI libraries (for Excel support)

## Option 1: Rebuild Without Apache POI (CSV Only)

If you only need CSV support (not Excel), you can rebuild without Apache POI:

```bash
cd flowsmith-java

# Create bin directory
mkdir -p bin

# Compile (will fail on MappingDocument.java due to missing Apache POI)
# Comment out Apache POI imports in MappingDocument.java first
javac -d bin src/com/flowsmith/*.java

# Create JAR
jar cfm flowsmith.jar manifest.mf -C bin .
```

## Option 2: Rebuild With Apache POI (Full Excel Support)

### Step 1: Download Apache POI

Download Apache POI from: https://poi.apache.org/download.html

Extract and note the location of these JARs:
- `poi-5.2.3.jar`
- `poi-ooxml-5.2.3.jar`
- `poi-ooxml-schemas-4.1.2.jar`
- `xmlbeans-5.1.1.jar`
- `commons-compress-1.21.jar`
- `commons-collections4-4.4.jar`

### Step 2: Compile with Dependencies

```bash
cd flowsmith-java

# Create bin directory
mkdir -p bin

# Set classpath to Apache POI JARs (adjust paths as needed)
export CLASSPATH="lib/poi-5.2.3.jar:lib/poi-ooxml-5.2.3.jar:lib/poi-ooxml-schemas-4.1.2.jar:lib/xmlbeans-5.1.1.jar:lib/commons-compress-1.21.jar:lib/commons-collections4-4.4.jar"

# Compile all Java files
javac -cp "$CLASSPATH" -d bin src/com/flowsmith/*.java

# Create JAR with dependencies
jar cfm flowsmith.jar manifest.mf -C bin .
```

### Step 3: Run with Dependencies

When running the JAR, include Apache POI in classpath:

```bash
java -cp "flowsmith.jar:lib/*" com.flowsmith.FlowSmith generate \
  --pattern ptp_file \
  --subsys XAJ --app TEST --func DEMO \
  --mapping mappings.xlsx
```

## Option 3: Use Eclipse/ACE Toolkit

Since you're using ACE Toolkit (which is Eclipse):

1. **Import Project:**
   - File → Import → Existing Projects into Workspace
   - Select `flowsmith-java` directory

2. **Add Apache POI to Build Path:**
   - Right-click project → Build Path → Configure Build Path
   - Add External JARs → Select Apache POI JARs

3. **Build:**
   - Project → Clean → Build
   - Eclipse will compile automatically

4. **Export JAR:**
   - Right-click project → Export → Java → JAR file
   - Select all source files
   - Choose destination: `flowsmith.jar`
   - Click Finish

## Option 4: Maven Build (Recommended)

Create a `pom.xml` file:

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.flowsmith</groupId>
    <artifactId>flowsmith</artifactId>
    <version>1.0.0</version>
    
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
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>com.flowsmith.FlowSmith</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

Then build:
```bash
mvn clean package
```

## Verification

After rebuilding, verify the JAR contains the new classes:

```bash
jar tf flowsmith.jar | grep -E "(MappingDocument|ESQLMappingGenerator)"
```

Should show:
```
com/flowsmith/MappingDocument.class
com/flowsmith/MappingDocument$FieldMapping.class
com/flowsmith/ESQLMappingGenerator.class
```

## Testing

Test the rebuilt JAR:

```bash
# Without mapping (should work)
java -jar flowsmith.jar list

# With mapping (requires Apache POI in classpath)
java -cp "flowsmith.jar:lib/*" com.flowsmith.FlowSmith generate \
  --pattern ptp_file \
  --subsys XAJ --app TEST --func DEMO \
  --mapping example-mapping.csv
```

## Troubleshooting

### "ClassNotFoundException: org.apache.poi..."
- Apache POI JARs not in classpath
- Add `-cp "flowsmith.jar:lib/*"` when running

### "NoClassDefFoundError: MappingDocument"
- JAR not rebuilt with new classes
- Follow rebuild steps above

### Compilation errors
- Check Java version (need JDK 8+)
- Verify all source files present
- Check Apache POI JARs downloaded

## Quick Rebuild Script

Save as `rebuild.sh`:

```bash
#!/bin/bash
cd flowsmith-java
mkdir -p bin
javac -cp "lib/*" -d bin src/com/flowsmith/*.java
jar cfm flowsmith.jar manifest.mf -C bin .
echo "✅ flowsmith.jar rebuilt successfully!"
```

Make executable and run:
```bash
chmod +x rebuild.sh
./rebuild.sh
```

## Notes

- The current `flowsmith.jar` in the repository does NOT include the mapping feature
- You must rebuild it to use the --mapping parameter
- For production use, include Apache POI JARs in the distribution
- Consider creating a fat JAR with all dependencies included