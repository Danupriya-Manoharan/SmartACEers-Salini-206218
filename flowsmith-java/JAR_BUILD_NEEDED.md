# ⚠️ IMPORTANT: JAR Rebuild Required

## Current Situation

The mapping feature source code has been implemented, but **flowsmith.jar needs to be rebuilt** to include the new classes:
- `MappingDocument.java`
- `ESQLMappingGenerator.java`

## Why Can't I Rebuild It Here?

The current Linux environment does not have:
- Java JDK (javac compiler)
- Apache POI libraries

## How to Get a Rebuilt JAR

### Option 1: Build on Windows with ACE Toolkit (RECOMMENDED)

Since you have ACE Toolkit installed on Windows, use Eclipse:

1. **Open ACE Toolkit (Eclipse)**

2. **Import the Project:**
   - File → Import → General → Existing Projects into Workspace
   - Browse to: `C:\Users\...\SmartACEers-Salini-206218\flowsmith-java`
   - Click Finish

3. **Download Apache POI:**
   - Go to: https://poi.apache.org/download.html
   - Download "Binary Distribution" (poi-bin-5.2.3.zip)
   - Extract to a folder (e.g., `C:\poi-5.2.3`)

4. **Add Apache POI to Build Path:**
   - Right-click project → Build Path → Configure Build Path
   - Click "Add External JARs"
   - Navigate to extracted POI folder
   - Select these JARs:
     - `poi-5.2.3.jar`
     - `poi-ooxml-5.2.3.jar`
     - `poi-ooxml-lite-5.2.3.jar`
     - `lib/commons-compress-1.21.jar`
     - `lib/commons-collections4-4.4.jar`
     - `ooxml-lib/xmlbeans-5.1.1.jar`
   - Click Apply and Close

5. **Build the Project:**
   - Project → Clean → Select flowsmith-java → Clean
   - Eclipse will automatically rebuild

6. **Export as JAR:**
   - Right-click project → Export
   - Java → JAR file
   - Select:
     - ✅ Export generated class files and resources
     - ✅ Export Java source files and resources
     - ✅ src folder
   - Destination: `C:\Users\...\SmartACEers-Salini-206218\flowsmith-java\flowsmith.jar`
   - Click "Next"
   - Click "Next" again
   - JAR Manifest Specification:
     - Select "Use existing manifest from workspace"
     - Browse to: `/flowsmith-java/manifest.mf`
   - Click Finish

7. **Copy Apache POI JARs:**
   ```cmd
   cd C:\Users\...\SmartACEers-Salini-206218\flowsmith-java
   mkdir lib
   copy C:\poi-5.2.3\poi-5.2.3.jar lib\
   copy C:\poi-5.2.3\poi-ooxml-5.2.3.jar lib\
   copy C:\poi-5.2.3\poi-ooxml-lite-5.2.3.jar lib\
   copy C:\poi-5.2.3\lib\commons-compress-1.21.jar lib\
   copy C:\poi-5.2.3\lib\commons-collections4-4.4.jar lib\
   copy C:\poi-5.2.3\ooxml-lib\xmlbeans-5.1.1.jar lib\
   ```

8. **Test the JAR:**
   ```cmd
   cd C:\Users\...\SmartACEers-Salini-206218\flowsmith-java
   java -cp "flowsmith.jar;lib\*" com.flowsmith.FlowSmith list
   ```

9. **Commit and Push:**
   ```cmd
   git add flowsmith.jar lib/
   git commit -m "Rebuild JAR with mapping feature and add Apache POI dependencies"
   git push
   ```

### Option 2: Build with Command Line (if you have JDK)

If you have Java JDK installed on Windows:

```cmd
cd C:\Users\...\SmartACEers-Salini-206218\flowsmith-java

REM Create bin directory
mkdir bin

REM Compile (with Apache POI in classpath)
javac -cp "lib\*" -d bin src\com\flowsmith\*.java

REM Create JAR
jar cfm flowsmith.jar manifest.mf -C bin .

REM Test
java -cp "flowsmith.jar;lib\*" com.flowsmith.FlowSmith list
```

### Option 3: Request Pre-built JAR

If you cannot build it yourself, you can:
1. Ask a team member with Java JDK to build it
2. Use a CI/CD service (GitHub Actions, Jenkins)
3. Build on a different machine with Java installed

## What's in the New JAR?

Once rebuilt, the JAR will include:
- ✅ All existing FlowSmith classes
- ✅ `MappingDocument.class` - Excel/CSV parser
- ✅ `MappingDocument$FieldMapping.class` - Inner class
- ✅ `ESQLMappingGenerator.class` - ESQL generator
- ✅ Updated `FlowSmith.class` - with --mapping support
- ✅ Updated `Generator.class` - with injection logic

## Verification

After rebuilding, verify the JAR contains new classes:

```cmd
jar tf flowsmith.jar | findstr /C:"MappingDocument" /C:"ESQLMapping"
```

Should show:
```
com/flowsmith/MappingDocument.class
com/flowsmith/MappingDocument$FieldMapping.class
com/flowsmith/ESQLMappingGenerator.class
```

## Running with Mapping Feature

Once rebuilt:

```cmd
java -cp "flowsmith.jar;lib\*" com.flowsmith.FlowSmith generate ^
  --pattern ptp_file ^
  --subsys XAJ --app CUST --func TRANSFORM ^
  --mapping customer-mapping.xlsx
```

## Alternative: Run from Source (Without JAR)

If you can't rebuild the JAR, you can run directly from source:

```cmd
cd flowsmith-java

REM Compile
javac -cp "lib\*" -d bin src\com\flowsmith\*.java

REM Run
java -cp "bin;lib\*" com.flowsmith.FlowSmith generate ^
  --pattern ptp_file ^
  --subsys XAJ --app CUST --func TRANSFORM ^
  --mapping customer-mapping.xlsx
```

## Need Help?

If you're stuck:
1. Check if Java JDK is installed: `javac -version`
2. Check if you're in the right directory
3. Verify Apache POI JARs are downloaded
4. Try building in Eclipse (ACE Toolkit)
5. Ask a team member with Java development environment

## Summary

**Current Status**: Source code is complete, JAR needs rebuild  
**Action Required**: Build JAR using Eclipse (ACE Toolkit) on Windows  
**Estimated Time**: 10-15 minutes  
**Difficulty**: Easy (follow steps above)

Once the JAR is rebuilt and pushed to Git, the mapping feature will be fully functional!