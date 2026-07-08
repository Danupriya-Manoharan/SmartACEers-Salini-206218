# Rebuilding flowsmith.jar

The `flowsmith.jar` must be rebuilt to include the mapping feature classes
(`MappingDocument`, `ESQLMappingGenerator`) and any later source changes.

## Prerequisites

- **Java JDK 17 or higher**, with `javac` and `jar` on the PATH.

That's it. Mapping documents are read as **CSV** by a pure-Java parser, so there
are **no external dependencies** (no Apache POI, no `lib\` folder).

> Build the jar into the `flowsmith-java` directory (next to `patterns.txt`).
> The tool locates `patterns.txt` and the templates relative to the jar, so it
> must live here - not in a `target/` or `bin/` subfolder.

## Option 1: build.bat (Windows)

From the `flowsmith-java` directory:

```cmd
build.bat
```

Produces `flowsmith.jar` and cleans up intermediates.

## Option 2: Manual commands

### Windows (Command Prompt)

```cmd
rmdir /s /q bin 2>nul
mkdir bin
javac -d bin src\com\flowsmith\*.java
jar cfe flowsmith.jar com.flowsmith.FlowSmith -C bin .
rmdir /s /q bin
```

### macOS / Linux

```bash
rm -rf bin
mkdir -p bin
javac -d bin src/com/flowsmith/*.java
jar cfe flowsmith.jar com.flowsmith.FlowSmith -C bin .
rm -rf bin
```

`jar cfe` sets the Main-Class (`com.flowsmith.FlowSmith`) directly, so no
separate manifest file is required.

## Option 3: Eclipse / ACE Toolkit

Since the ACE Toolkit is Eclipse-based and needs no command line:

1. **Import:** File → Import → General → Existing Projects into Workspace →
   select `flowsmith-java`.
2. **Export:** File → Export → Java → **Runnable JAR file**.
3. **Launch configuration:** `FlowSmith Generate with Mapping (java)`.
4. **Export destination:** `flowsmith-java/flowsmith.jar`.
5. Finish. No JARs need to be added to the build path.

## Verification

```bash
jar tf flowsmith.jar | grep -E "(MappingDocument|ESQLMappingGenerator)"
```

(Windows: `jar tf flowsmith.jar | findstr /i "MappingDocument ESQLMappingGenerator"`)

Should show:
```
com/flowsmith/MappingDocument.class
com/flowsmith/MappingDocument$FieldMapping.class
com/flowsmith/ESQLMappingGenerator.class
```

## Testing

```bash
# Basic - no mapping
java -jar flowsmith.jar list

# With mapping (CSV)
java -jar flowsmith.jar generate \
  --subsys XAJ --app TEST --func DEMO \
  --mapping example-mapping.csv
```

No classpath flags are needed, since there are no external libraries.

## Troubleshooting

### "NoClassDefFoundError: com/flowsmith/MappingDocument"
The jar wasn't rebuilt with the new classes. Rebuild using one of the options
above and re-run the verification step.

### "Mapping file not found"
Point `--mapping` at a real `.csv` file (absolute path, or relative to the
`flowsmith-java` directory).

### Compilation errors
- Check the Java version: `javac -version` (need JDK 17+).
- Verify all source files are present under `src/com/flowsmith/`.

### `patterns.txt` / template not found at runtime
The jar must sit in the `flowsmith-java` directory (next to `patterns.txt`),
not in a build subfolder. Move `flowsmith.jar` up if you built into `bin/`.

## Notes

- The committed `flowsmith.jar` does NOT include the mapping feature until you
  rebuild it.
- The build is dependency-free - a plain `jar` is all that's needed; no fat jar
  or bundled libraries.
