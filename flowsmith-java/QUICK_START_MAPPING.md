# Quick Start: Field Mapping Feature

Get started with FlowSmith's automatic ESQL generation in 5 minutes!

> No external libraries needed - mapping documents are plain **CSV** files,
> parsed by pure Java. No Apache POI, no Excel runtime.

## Step 1: Create Your Mapping Document

Create a CSV file (e.g., `customer-mapping.csv`) with two columns:

```csv
Source Field (XML),Target Field (JSON)
customer/id,customer.customerId
customer/name,customer.fullName
customer/email,customer.email
order/orderId,order.id
order/amount,order.total
```

**Tips:**
- Column A = XML field paths (use `/` or `.` as separator)
- Column B = JSON field paths (use `.` as separator)
- First row can be a header (auto-detected and skipped)
- Save as `.csv` format. In Excel: **File > Save As > CSV (Comma delimited)**

## Step 2: Run FlowSmith with Mapping

```bash
java -jar flowsmith.jar generate \
  --subsys XAJ \
  --app CUST \
  --func TRANSFORM \
  --mapping customer-mapping.csv
```

**What happens:**
1. FlowSmith loads your mapping document
2. Generates the PTP file-to-file ACE application structure
3. **Automatically injects ESQL mapping code** into Adapter_Compute.esql
4. Outputs a ready-to-import project

> Note: FlowSmith always generates the PTP file-to-file pattern (which supports
> XML-to-JSON mappings). Any `--pattern` / `--requirement` you pass is accepted
> but does not change the pattern.

## Step 3: Review Generated ESQL

FlowSmith creates code like this in your `Adapter_Compute.esql`:

```esql
CREATE COMPUTE MODULE Adapter_Compute
    CREATE FUNCTION Main() RETURNS BOOLEAN
    BEGIN
        CALL CopyMessageHeaders();
        
        -- Auto-generated field mappings from mapping document
        -- Source: XMLNSC -> Target: JSON
        
        -- Initialize output message
        CREATE FIELD OutputRoot.JSON;
        CREATE FIELD OutputRoot.JSON.Data;
        
        -- Field mappings
        SET OutputRoot.JSON.Data.customer.customerId = InputRoot.XMLNSC.customer.id;
        SET OutputRoot.JSON.Data.customer.fullName = InputRoot.XMLNSC.customer.name;
        SET OutputRoot.JSON.Data.customer.email = InputRoot.XMLNSC.customer.email;
        SET OutputRoot.JSON.Data.order.id = InputRoot.XMLNSC.order.orderId;
        SET OutputRoot.JSON.Data.order.total = InputRoot.XMLNSC.order.amount;
        
        RETURN TRUE;
    END;
    
    -- Helper procedures...
END MODULE;
```

## Step 4: Import and Test

1. **Import into ACE Toolkit:**
   - File → Import → Existing Projects
   - Browse to generated output directory
   - Select your project

2. **Review the ESQL:**
   - Open `Adapter_Compute.esql`
   - Verify field mappings match your requirements
   - Add any custom logic if needed

3. **Test with sample data:**
   - Create test XML input
   - Run flow in debug mode
   - Verify JSON output

4. **Build and Deploy:**
   - Create BAR file
   - Deploy to integration server

## Common Use Cases

### Use Case 1: Simple XML to JSON Conversion

**Mapping (`simple-mapping.csv`):**
```csv
customer/name,customer.name
customer/age,customer.age
```

**Command:**
```bash
java -jar flowsmith.jar generate \
  --subsys XAJ --app CONV --func XMLJSON \
  --mapping simple-mapping.csv
```

### Use Case 2: Order Transformation

**Mapping (`order-mapping.csv`):**
```csv
order/header/orderId,orderId
order/header/date,orderDate
order/items/item,items
```

**Command:**
```bash
java -jar flowsmith.jar generate \
  --subsys XAJ --app ORDER --func PUBLISH \
  --mapping order-mapping.csv
```

### Use Case 3: With a Plain-English Requirement

**Command:**
```bash
java -jar flowsmith.jar generate \
  --requirement "transform XML customer data to JSON" \
  --subsys XAJ --app CUST --func TRANSFORM \
  --mapping customer-mapping.csv
```

The requirement text is echoed for context; FlowSmith still generates the PTP
file-to-file flow and applies your field mappings.

## Troubleshooting

### "Mapping file not found"
- Check the file path is correct
- Use an absolute path: `/full/path/to/mapping.csv`
- Or a path relative to the current directory

### "No mappings loaded"
- Verify the CSV has data in both columns (`source,target`)
- Ensure at least one valid mapping row exists below the header

### Generated ESQL has compilation errors
- Review field paths in the mapping document
- Ensure the XML structure matches your input data
- Check for typos in field names
- Manually adjust ESQL if needed

## Next Steps

- Read [MAPPING_FEATURE.md](MAPPING_FEATURE.md) for complete documentation
- See [example-mapping.csv](example-mapping.csv) for a sample mapping file
- Check [DEPENDENCIES.md](DEPENDENCIES.md) for build instructions (JDK only)
- Explore advanced features (coming soon):
  - Data type conversions
  - Conditional mappings
  - Array handling

## Tips for Success

✅ **DO:**
- Start with simple 1:1 mappings
- Test with sample data before production
- Review generated ESQL code
- Keep mapping documents in version control
- Use descriptive field names

❌ **DON'T:**
- Use complex transformations in the mapping (do those manually)
- Forget to verify field paths match your data structure
- Skip the review step - always check generated code
- Put commas inside field paths (CSV uses comma as the column separator)

## Example Workflow

```bash
# 1. Create mapping document (CSV)
# 2. Generate with FlowSmith
java -jar flowsmith.jar generate \
  --subsys XAJ --app TEST --func DEMO \
  --mapping my-mapping.csv

# 3. Output shows:
#    [AI] Loading mapping document: my-mapping.csv
#    [AI] Loaded 5 field mappings
#    [AI] Generating standardized ACE application...
#    [AI] Done - developer review required.
#    field mappings : 5 mappings injected into ESQL

# 4. Import into ACE Toolkit
# 5. Review and test
# 6. Deploy!
```

Happy mapping! 🚀
