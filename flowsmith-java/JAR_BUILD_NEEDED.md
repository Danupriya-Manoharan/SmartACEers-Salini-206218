# ⚠️ IMPORTANT: JAR Rebuild Required

## Current situation

The mapping feature source code is implemented, but the committed
`flowsmith.jar` is **stale** - it was built before the mapping feature and does
NOT contain the new classes:
- `MappingDocument.java`
- `ESQLMappingGenerator.java`

Until you rebuild, the `--mapping` option has no effect. Rebuilding is required
for the feature to exist in the running jar.

## Good news: no external dependencies

Mapping documents are read as **CSV** by a pure-Java parser, so there is
**nothing to download** - no Apache POI, no `lib\` folder. You only need a
**JDK 17+** with `javac` on PATH.

## How to rebuild

Pick whichever route works on your machine. Full details in
[REBUILD_JAR.md](REBUILD_JAR.md) and [DEPENDENCIES.md](DEPENDENCIES.md).

### Option 1: build.bat (Windows, fastest)

```cmd
cd C:\Users\...\SmartACEers-Salini-206218\flowsmith-java
build.bat
```

### Option 2: Manual commands (if scripts are blocked by policy)

Paste into a Command Prompt opened in `flowsmith-java`:

```cmd
rmdir /s /q bin 2>nul
mkdir bin
javac -d bin src\com\flowsmith\*.java
jar cfe flowsmith.jar com.flowsmith.FlowSmith -C bin .
rmdir /s /q bin
```

### Option 3: Eclipse / ACE Toolkit (no command line)

1. Import the `flowsmith-java` project (File → Import → Existing Projects).
2. File → Export → Java → **Runnable JAR file**.
3. Launch configuration: `FlowSmith Generate with Mapping (java)`.
4. Export destination: `flowsmith-java\flowsmith.jar`.
5. Finish. No libraries need to be added to the build path.

## Verify the rebuild

```cmd
jar tf flowsmith.jar | findstr /i "MappingDocument ESQLMappingGenerator"
```

Should show:
```
com/flowsmith/MappingDocument.class
com/flowsmith/MappingDocument$FieldMapping.class
com/flowsmith/ESQLMappingGenerator.class
```

## Run with the mapping feature

```cmd
java -jar flowsmith.jar generate ^
  --subsys XAJ --app CUST --func TRANSFORM ^
  --mapping example-mapping.csv
```

No classpath flags are needed - there are no external libraries.

## Summary

**Status:** source is complete; the committed jar is stale
**Action:** rebuild `flowsmith.jar` with a JDK (no dependencies)
**Time:** ~2 minutes
**Difficulty:** easy
