# Quick Start: Field Mapping Feature

Get started with FlowSmith's automatic ESQL generation in 5 minutes!

## Step 1: Create Your Mapping Document

Create an Excel file (e.g., `customer-mapping.xlsx`) with two columns:

| Source Field (XML) | Target Field (JSON) |
|-------------------|---------------------|
| customer/id       | customer.customerId |
| customer/name     | customer.fullName   |
| customer/email    | customer.email      |
| order/orderId     | order.id            |
| order/amount      | order.total         |

**Tips:**
- Column A = XML field paths (use `/` or `.` as separator)
- Column B = JSON field paths (use `.` as separator)
- First row can be a header (will be auto-detected)
- Save as `.xlsx` format

## Step 2: Run FlowSmith with Mapping

```bash
java -jar flowsmith.jar generate \
  --pattern ptp_file \
  --subsys XAJ \
  --app CUST \
  --func TRANSFORM \
  --mapping customer-mapping.xlsx
```

**What happens:**
1. FlowSmith loads your mapping document
2. Selects the appropriate pattern template
3. Generates the ACE application structure
4. **Automatically injects ESQL mapping code** into Adapter_Compute.esql
5. Outputs ready-to-import project

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

**Mapping:**
```
customer/name → customer.name
customer/age → customer.age
```

**Command:**
```bash
java -jar flowsmith.jar generate --pattern ptp_file \
  --subsys XAJ --app CONV --func XMLJSON \
  --mapping simple-mapping.xlsx
```

### Use Case 2: Publisher with Transformation

**Mapping:**
```
order/header/orderId → orderId
order/header/date → orderDate
order/items/item → items
```

**Command:**
```bash
java -jar flowsmith.jar generate --pattern pub_file \
  --subsys XAJ --app ORDER --func PUBLISH \
  --mapping order-mapping.xlsx
```

### Use Case 3: AI-Recommended Pattern with Mapping

**Command:**
```bash
java -jar flowsmith.jar generate \
  --requirement "transform XML customer data to JSON and publish to queue" \
  --subsys XAJ --app CUST --func TRANSFORM \
  --mapping customer-mapping.xlsx
```

FlowSmith will:
1. Use AI to select the best pattern (likely `pub_file`)
2. Apply your field mappings
3. Generate complete solution

## Troubleshooting

### "Mapping file not found"
- Check file path is correct
- Use absolute path: `/full/path/to/mapping.xlsx`
- Or relative to current directory

### "No mappings loaded"
- Verify Excel file has data in columns A and B
- Check file is `.xlsx` format (not `.xls`)
- Ensure at least one valid mapping row exists

### Generated ESQL has compilation errors
- Review field paths in mapping document
- Ensure XML structure matches your input data
- Check for typos in field names
- Manually adjust ESQL if needed

### Need Apache POI libraries
- See [DEPENDENCIES.md](DEPENDENCIES.md) for setup
- Download Apache POI from https://poi.apache.org/
- Add JARs to classpath when running FlowSmith

## Next Steps

- Read [MAPPING_FEATURE.md](MAPPING_FEATURE.md) for complete documentation
- See [example-mapping.csv](example-mapping.csv) for a sample mapping file
- Check [DEPENDENCIES.md](DEPENDENCIES.md) for Apache POI setup
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
- Use complex transformations in mapping (do those manually)
- Forget to verify field paths match your data structure
- Skip the review step - always check generated code
- Use old .xls format (use .xlsx)

## Example Workflow

```bash
# 1. Create mapping document (Excel)
# 2. Generate with FlowSmith
java -jar flowsmith.jar generate \
  --pattern ptp_file \
  --subsys XAJ --app TEST --func DEMO \
  --mapping my-mapping.xlsx

# 3. Output shows:
#    [AI] Loading mapping document: my-mapping.xlsx
#    [AI] Loaded 5 field mappings
#    [AI] Generating standardized ACE application...
#    [AI] Done - developer review required.
#    field mappings : 5 mappings injected into ESQL

# 4. Import into ACE Toolkit
# 5. Review and test
# 6. Deploy!
```

Happy mapping! 🚀