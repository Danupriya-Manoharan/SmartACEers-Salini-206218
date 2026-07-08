# FlowSmith Dependencies

## No external libraries required

FlowSmith builds and runs with **just the JDK (Java 8+)**. There are no
third-party dependencies to download.

The field-mapping feature reads mapping documents as **CSV** using a small
pure-Java parser (`MappingDocument.java`), so **Apache POI is not needed**.

## Building

You only need a JDK on the PATH. Pick whichever route works on your machine.

### Option 1: build.bat (Windows)

From the `flowsmith-java` directory:

```bat
build.bat
```

Produces `flowsmith.jar` (runnable, pure JDK).

### Option 2: Manual commands (if scripts are blocked by policy)

Paste into a Command Prompt opened in `flowsmith-java`:

```bat
rmdir /s /q bin 2>nul
mkdir bin
javac -d bin src\com\flowsmith\*.java
jar cfe flowsmith.jar com.flowsmith.FlowSmith -C bin .
rmdir /s /q bin
```

### Option 3: Eclipse / ACE Toolkit

`File > Export > Java > Runnable JAR file`, choose the
`FlowSmith Generate with Mapping (java)` launch configuration, and export to
`flowsmith-java/flowsmith.jar`. No libraries need to be added to the build path.

### macOS / Linux

```bash
mkdir -p bin
javac -d bin src/com/flowsmith/*.java
jar cfe flowsmith.jar com.flowsmith.FlowSmith -C bin .
rm -rf bin
```

## Verifying the build

Confirm the mapping classes are present in the jar:

```bat
jar tf flowsmith.jar | findstr /i "MappingDocument ESQLMappingGenerator"
```

## Mapping document format

Mapping documents are **CSV** files (`.csv`):

```csv
Source Field (XML),Target Field (JSON)
customer/id,customer.customerId
customer/name,customer.fullName
order/amount,order.totalAmount
```

- Column A = source field (XML path), Column B = target field (JSON path)
- The first row is treated as a header when it looks like column titles
- Blank lines are ignored

Editing in Excel is fine - just use **File > Save As > CSV (Comma delimited)**.
See `example-mapping.csv` for a complete example.

## Runtime

```bash
java -jar flowsmith.jar generate \
  --subsys XAJ --app TLMTF --func FINANCING \
  --mapping example-mapping.csv
```

No classpath flags are required, since there are no external libraries.

## Troubleshooting

### "Mapping file not found"

Ensure the path is correct and points to a `.csv` file (absolute path, or
relative to the `flowsmith-java` directory).

### "No mappings loaded"

Check the CSV has at least one data row with both columns filled in
(`source,target`).

### Want Excel (.xlsx) support instead?

The `.xlsx` binary format requires Apache POI. This build intentionally avoids
that dependency for simplicity and locked-down environments. If you later need
native `.xlsx` reading, add the Apache POI jars and restore a POI-based reader
in `MappingDocument.java`.
