# Eclipse Launch Configuration Instructions

## Using the Mapping Feature with Eclipse Launch

You now have **two launch configurations** available:

### 1. Original Launch (No Mapping)
**File:** `FlowSmith Generate (java).launch`

Use this for standard flow generation without field mappings.

**How to use:**
1. Run → External Tools → External Tools Configurations...
2. Select "FlowSmith Generate (java)"
3. Click Run
4. Enter prompts:
   - Requirement description
   - SUBSYS code
   - APPNM code
   - FUNCNM code
   - NDM name (or NONE)

### 2. New Launch with Mapping Support
**File:** `FlowSmith Generate with Mapping (java).launch`

Use this to generate flows with automatic ESQL field mappings.

**How to use:**
1. Run → External Tools → External Tools Configurations...
2. Select "FlowSmith Generate with Mapping (java)"
3. Click Run
4. Enter prompts:
   - Requirement description (e.g., "convert XML to JSON with field mappings")
   - SUBSYS code (e.g., XAJ)
   - APPNM code (e.g., TLMTF)
   - FUNCNM code (e.g., FINANCING)
   - NDM name (or NONE)
   - **Mapping file path** (e.g., `C:\path\to\your-mapping.csv` or type `NONE` to skip)

### Example Workflow with Mapping

1. **Create your mapping CSV file:**
   ```csv
   Source Field (XML),Target Field (JSON)
   customer/id,customer.customerId
   customer/name,customer.fullName
   ```
   Save as `customer-mapping.csv` (in Excel: File > Save As > CSV)

2. **Run the launch configuration:**
   - Select "FlowSmith Generate with Mapping (java)"
   - Click Run

3. **Enter values when prompted:**
   ```
   Requirement: convert XML customer data to JSON
   SUBSYS: XAJ
   APPNM: CUST
   FUNCNM: TRANSFORM
   NDM: NONE
   Mapping file: C:\Users\YourName\Documents\customer-mapping.csv
   ```

4. **FlowSmith will:**
   - Load your mapping file
   - Generate the ACE application
   - Inject ESQL mapping code automatically
   - Output to `FlowSmith_Generated` directory

5. **Import and review:**
   - File → Import → Existing Projects
   - Browse to output directory
   - Review generated ESQL with your mappings

### Tips

- **Default mapping file:** The launch config suggests `example-mapping.csv` by default
- **Skip mapping:** Type `NONE` in the mapping file prompt to generate without mappings
- **Absolute paths:** Use full paths like `C:\path\to\file.csv`
- **Relative paths:** Or use paths relative to `flowsmith-java` directory

### Troubleshooting

**"Mapping file not found"**
- Ensure you typed the full path correctly
- Use backslashes on Windows: `C:\Users\...`
- Or type `NONE` to skip mapping feature

**"No mappings loaded"**
- Check your CSV file has data in both columns (`source,target`)
- Verify file is `.csv` format
- Ensure at least one mapping row exists

**Launch config not appearing**
- Refresh Eclipse workspace (F5)
- Check External Tools Configurations menu
- Ensure `.launch` files are in project root

### Customizing Launch Configs

To modify the launch configurations:

1. Run → External Tools → External Tools Configurations...
2. Select the configuration to edit
3. Modify the "Arguments" field
4. Click Apply

The arguments field contains:
```
-jar "path\to\flowsmith.jar" generate 
  --requirement "${string_prompt:...}" 
  --subsys ${string_prompt:...}
  --app ${string_prompt:...}
  --func ${string_prompt:...}
  --ndm ${string_prompt:...}
  --mapping "${string_prompt:...}"
```

Each `${string_prompt:...}` creates an input dialog when you run the launch.

### Next Steps

- See [QUICK_START_MAPPING.md](flowsmith-java/QUICK_START_MAPPING.md) for mapping file format
- See [MAPPING_FEATURE.md](flowsmith-java/MAPPING_FEATURE.md) for complete documentation
- See [DEPENDENCIES.md](flowsmith-java/DEPENDENCIES.md) for build instructions (JDK only)