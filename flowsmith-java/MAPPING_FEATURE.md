# Field Mapping Feature - XML to JSON Transformation

## Overview

FlowSmith supports **automatic ESQL generation** from mapping documents. You provide a CSV file that specifies field mappings between XML and JSON formats, and FlowSmith automatically injects the mapping code into your generated ESQL files.

> The mapping document is a plain **CSV** file, parsed by pure Java. There are
> **no external dependencies** (no Apache POI, no Excel runtime) - FlowSmith
> builds and runs with just the JDK.

## How It Works

1. **Create a mapping document** (CSV) with two columns:
   - Column A: Source field (XML path)
   - Column B: Target field (JSON path)

2. **Run FlowSmith with the --mapping parameter**:
   ```bash
   java -jar flowsmith.jar generate \
     --subsys XAJ --app TLMTF --func FINANCING \
     --mapping my-mappings.csv
   ```

3. **FlowSmith automatically**:
   - Parses your mapping document
   - Generates ESQL mapping code
   - Injects it into Adapter_Compute.esql or Adapter_Map.esql files
   - Creates proper InputRoot.XMLNSC → OutputRoot.JSON.Data mappings

> Pattern note: FlowSmith always generates the PTP file-to-file pattern (which
> supports XML-to-JSON mappings). A `--pattern` or `--requirement` argument is
> accepted but does not change the generated pattern.

## Mapping Document Format

### CSV Format (.csv)

```csv
Source Field (XML),Target Field (JSON)
customer/id,customer.customerId
customer/name,customer.fullName
order/orderId,order.id
order/amount,order.totalAmount
```

**Notes:**
- First row is treated as a header when it looks like column titles (optional)
- Use `/` or `.` as path separators in source fields
- Use `.` as path separator in target fields
- Empty lines are ignored
- Don't put commas inside field paths (comma is the CSV column separator)
- Editing in Excel is fine - use **File > Save As > CSV (Comma delimited)**

## Generated ESQL Code

Given the mapping above, FlowSmith generates:

```esql
BROKER SCHEMA PTP.XAJ.TLMTF.FINANCING.FIL

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
        SET OutputRoot.JSON.Data.order.id = InputRoot.XMLNSC.order.orderId;
        SET OutputRoot.JSON.Data.order.totalAmount = InputRoot.XMLNSC.order.amount;
        
        RETURN TRUE;
    END;

    CREATE PROCEDURE CopyMessageHeaders() BEGIN
        DECLARE I INTEGER 1;
        DECLARE J INTEGER;
        SET J = CARDINALITY(InputRoot.*[]);
        WHILE I < J DO
            SET OutputRoot.*[I] = InputRoot.*[I];
            SET I = I + 1;
        END WHILE;
    END;
END MODULE;
```

## Usage Examples

### Example 1: Basic XML to JSON Conversion

```bash
# Create your mapping file (example-mapping.csv)
# Then generate the flow:

java -jar flowsmith.jar generate \
  --subsys XAJ \
  --app TLMTF \
  --func FINANCING \
  --mapping example-mapping.csv
```

### Example 2: With a Plain-English Requirement

```bash
java -jar flowsmith.jar generate \
  --requirement "Convert XML customer data to JSON format" \
  --subsys XAJ \
  --app CUST \
  --func TRANSFORM \
  --mapping customer-mappings.csv
```

### Example 3: Another Application with Mappings

```bash
java -jar flowsmith.jar generate \
  --subsys XAJ \
  --app ORDER \
  --func PUBLISH \
  --mapping order-mappings.csv
```

## Field Path Syntax

### XML Source Paths
- Simple: `customer` → `InputRoot.XMLNSC.customer`
- Nested: `customer/name` → `InputRoot.XMLNSC.customer.name`
- Deep: `order/items/item` → `InputRoot.XMLNSC.order.items.item`

### JSON Target Paths
- Simple: `customer` → `OutputRoot.JSON.Data.customer`
- Nested: `customer.name` → `OutputRoot.JSON.Data.customer.name`
- Deep: `order.items.item` → `OutputRoot.JSON.Data.order.items.item`

## Best Practices

1. **Use descriptive field names** in your mapping document
2. **Test with sample data** before deploying
3. **Review generated ESQL** - FlowSmith adds comments for clarity
4. **Keep mappings simple** - complex transformations should be done manually
5. **Version control** your mapping documents alongside your flows

## Generated Pattern

FlowSmith always generates the **`ptp_file`** pattern (PTP File-to-File), which
supports XML-to-JSON field mappings. A `--pattern` or `--requirement` argument
is accepted but does not change the generated pattern.

## Limitations

Current version supports:
- ✅ 1:1 field mappings (one source → one target)
- ✅ Simple path navigation
- ✅ XML to JSON transformations
- ❌ Complex transformations (concatenation, calculations)
- ❌ Conditional mappings
- ❌ Data type conversions (coming soon)
- ❌ Array/list handling (coming soon)

For complex transformations, manually edit the generated ESQL after generation.

## Troubleshooting

### "Mapping file not found"
- Ensure the file path is correct
- Use absolute path or path relative to current directory

### "No mappings loaded"
- Check the CSV file has data in both columns (`source,target`)
- Ensure the first row is a header or a valid data row
- Verify the file is `.csv` format

### "Generated ESQL has errors"
- Review field paths in mapping document
- Ensure XML/JSON structure matches your data
- Check for typos in field names

## Example Mapping Document

See `example-mapping.csv` in the flowsmith-java directory for a complete example.

## Future Enhancements

Planned features:
- Data type conversions (string to number, date formatting)
- Conditional mappings (if-then-else logic)
- Array/list iteration
- Custom transformation functions
- Validation rules
- Mapping document templates

## Support

For issues or questions:
1. Check this documentation
2. Review generated ESQL code
3. Consult ACE ESQL documentation
4. Contact the FlowSmith team