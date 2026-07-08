# Sample-Based Mapping Feature

## Overview

FlowSmith can now automatically infer field mappings from sample input and output files, eliminating the need to manually create mapping documents. Simply provide an XML input sample and a JSON output sample, and FlowSmith will generate the ESQL transformation code automatically.

## How It Works

### 1. Automatic Mapping Inference

FlowSmith uses a **1:1 position-based mapping strategy**:

1. **Parse XML structure** - Extracts all leaf fields from the XML sample
2. **Parse JSON structure** - Extracts all leaf fields from the JSON sample  
3. **Match by position** - Maps the 1st XML field to 1st JSON field, 2nd to 2nd, etc.
4. **Generate ESQL** - Creates transformation code: `SET OutputRoot.JSON.Data.field = InputRoot.XMLNSC.field;`

### 2. Benefits

- ✅ **No manual mapping document needed** - Just provide samples
- ✅ **Same files used for testing** - Input sample becomes your test data
- ✅ **Faster development** - Skip CSV creation step
- ✅ **Visual validation** - See exactly what input produces what output
- ✅ **Self-documenting** - Sample files show the transformation clearly

## Usage

### Basic Command

```bash
java -jar flowsmith.jar generate \
  --requirement "Convert XML customer data to JSON" \
  --subsys XAJ --app CUST --func TRANSFORM \
  --sample-input customer-input.xml \
  --sample-output customer-output.json
```

### Sample Files

#### Input: customer-input.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<customer>
    <id>12345</id>
    <name>John Doe</name>
    <email>john@example.com</email>
    <address>
        <street>123 Main St</street>
        <city>Boston</city>
        <zip>02101</zip>
    </address>
</customer>
```

#### Output: customer-output.json
```json
{
  "customer": {
    "customerId": "12345",
    "fullName": "John Doe",
    "emailAddress": "john@example.com",
    "location": {
      "streetAddress": "123 Main St",
      "cityName": "Boston",
      "postalCode": "02101"
    }
  }
}
```

### Generated Mappings

FlowSmith will infer these mappings (1:1 by position):

```
customer.id           -> customer.customerId
customer.name         -> customer.fullName
customer.email        -> customer.emailAddress
customer.address.street -> customer.location.streetAddress
customer.address.city   -> customer.location.cityName
customer.address.zip    -> customer.location.postalCode
```

### Generated ESQL

```esql
-- Auto-generated field mappings from sample files
SET OutputRoot.JSON.Data.customer.customerId = InputRoot.XMLNSC.customer.id;
SET OutputRoot.JSON.Data.customer.fullName = InputRoot.XMLNSC.customer.name;
SET OutputRoot.JSON.Data.customer.emailAddress = InputRoot.XMLNSC.customer.email;
SET OutputRoot.JSON.Data.customer.location.streetAddress = InputRoot.XMLNSC.customer.address.street;
SET OutputRoot.JSON.Data.customer.location.cityName = InputRoot.XMLNSC.customer.address.city;
SET OutputRoot.JSON.Data.customer.location.postalCode = InputRoot.XMLNSC.customer.address.zip;
```

## Complete Workflow

### Step 1: Create Sample Files

Create your input XML and expected output JSON:

```bash
# Create input sample
cat > order-input.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<order>
    <orderId>ORD-001</orderId>
    <customerId>CUST-123</customerId>
    <amount>99.99</amount>
    <status>pending</status>
</order>
EOF

# Create output sample
cat > order-output.json << 'EOF'
{
  "order": {
    "id": "ORD-001",
    "customer": "CUST-123",
    "totalAmount": "99.99",
    "orderStatus": "pending"
  }
}
EOF
```

### Step 2: Generate Flow with Mappings

```bash
java -jar flowsmith.jar generate \
  --requirement "Process order XML to JSON" \
  --subsys ORD --app PROCESS --func TRANSFORM \
  --sample-input order-input.xml \
  --sample-output order-output.json
```

**Output:**
```
=========================================================
  ACE FlowSmith AI  -  Intelligent Integration Generator
=========================================================

[AI] Inferring mappings from sample files:
       - Input  : order-input.xml
       - Output : order-output.json

Parsing XML structure from: order-input.xml

XML Fields:
  order.orderId
  order.customerId
  order.amount
  order.status

Parsing JSON structure from: order-output.json

JSON Fields:
  order.id
  order.customer
  order.totalAmount
  order.orderStatus

Inferring mappings...

Inferred 4 field mappings:
  order.orderId -> order.id
  order.customerId -> order.customer
  order.amount -> order.totalAmount
  order.status -> order.orderStatus

[AI] Inferred 4 field mappings (1:1 position-based)
[AI] Pattern          : ptp_file  (Point-to-Point File Integration)
[AI] Generating standardized ACE application...
       - org naming convention : ORD_PTP_PROCESS_TRANSFORM_FIL
       - enterprise subflows    : ED6 logging / error-handling framework
       - env params             : DEV / ACC / PRO
       - field mappings         : XML to JSON transformation

[AI] Done - developer review required.
```

### Step 3: Deploy with BAR Override

```bash
cd automation
java ACEDeployer ORD_PTP_PROCESS_TRANSFORM_FIL.bar \
  --testApp OrderTransform \
  --fileIn "File Input" \
  --fileOut "File Output"
```

### Step 4: Test with Sample File

```bash
# Copy your input sample to test directory
copy order-input.xml C:\temp\test\OrderTransform\in\

# Check output
type C:\temp\test\OrderTransform\out\order-input.json
```

## Advanced Usage

### Test Mapping Inference

Test the inference without generating a full flow:

```bash
# Test parser
java -cp flowsmith.jar com.flowsmith.SampleFileParser \
  customer-input.xml customer-output.json

# Test inferencer (shows generated ESQL)
java -cp flowsmith.jar com.flowsmith.MappingInferencer \
  customer-input.xml customer-output.json
```

### Verify Mappings

The inferencer shows you exactly what mappings it will create:

```
========================================
  Mapping Inference from Sample Files
========================================

Parsing XML structure from: customer-input.xml
Parsing JSON structure from: customer-output.json

Inferring mappings...

Inferred 6 field mappings:
  customer.id -> customer.customerId
  customer.name -> customer.fullName
  customer.email -> customer.emailAddress
  customer.address.street -> customer.location.streetAddress
  customer.address.city -> customer.location.cityName
  customer.address.zip -> customer.location.postalCode

========================================
  Generated ESQL Code
========================================

-- Auto-generated field mappings
SET OutputRoot.JSON.Data.customer.customerId = InputRoot.XMLNSC.customer.id;
SET OutputRoot.JSON.Data.customer.fullName = InputRoot.XMLNSC.customer.name;
...
```

## Mapping Strategy: 1:1 Position-Based

### How Fields Are Matched

Fields are matched **by their position** in the structure:

```
XML Fields (in order):        JSON Fields (in order):
1. customer.id           →    1. customer.customerId
2. customer.name         →    2. customer.fullName
3. customer.email        →    3. customer.emailAddress
```

### Important Notes

1. **Field count must match** - XML and JSON should have the same number of leaf fields
2. **Order matters** - Fields are matched by position, not by name
3. **Structure your samples carefully** - Arrange fields in the order you want them mapped

### Example: Correct vs Incorrect

**✅ Correct - Same field count:**
```xml
<customer>
  <id>123</id>
  <name>John</name>
</customer>
```
```json
{
  "customer": {
    "customerId": "123",
    "fullName": "John"
  }
}
```
Result: `id → customerId`, `name → fullName`

**⚠️ Warning - Different field counts:**
```xml
<customer>
  <id>123</id>
  <name>John</name>
  <email>john@example.com</email>
</customer>
```
```json
{
  "customer": {
    "customerId": "123",
    "fullName": "John"
  }
}
```
Result: Only first 2 fields mapped, `email` ignored with warning

## Comparison: Sample-Based vs CSV Mapping

### Sample-Based Approach (Recommended)

**Pros:**
- ✅ No manual CSV creation
- ✅ Visual - see actual data transformation
- ✅ Same files used for testing
- ✅ Faster development
- ✅ Self-documenting

**Cons:**
- ⚠️ Requires same number of fields in XML and JSON
- ⚠️ Position-based matching (not name-based)

**Best for:**
- Quick prototyping
- Simple transformations
- When you have sample data available

### CSV Mapping Approach

**Pros:**
- ✅ Explicit field-to-field mapping
- ✅ Can map different field counts
- ✅ Name-based matching
- ✅ More control over mappings

**Cons:**
- ⚠️ Manual CSV creation required
- ⚠️ Extra step in workflow

**Best for:**
- Complex transformations
- Different field counts
- When explicit control is needed

## Troubleshooting

### Issue: "WARNING: XML has X fields but JSON has Y fields"

**Cause:** Field count mismatch between XML and JSON samples

**Solution:** 
1. Check your sample files - count the leaf fields
2. Ensure both have the same number of fields
3. Adjust samples to match, or use CSV mapping instead

### Issue: Wrong field mappings

**Cause:** Fields matched by position, not by name

**Solution:**
1. Rearrange fields in your sample files to match desired mapping order
2. Or use CSV mapping for explicit control

### Issue: Nested structures not mapping correctly

**Cause:** Parser extracts only leaf fields (fields with actual values)

**Solution:**
1. Ensure your samples have values in all leaf fields
2. Check the inference output to see what fields were detected
3. Adjust sample structure if needed

## Examples

### Example 1: Simple Customer Transform

**Input:** `customer.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<customer>
    <id>C001</id>
    <name>Alice Smith</name>
    <email>alice@example.com</email>
</customer>
```

**Output:** `customer.json`
```json
{
  "customer": {
    "customerId": "C001",
    "fullName": "Alice Smith",
    "emailAddress": "alice@example.com"
  }
}
```

**Command:**
```bash
java -jar flowsmith.jar generate \
  --subsys CRM --app CUST --func SYNC \
  --sample-input customer.xml \
  --sample-output customer.json
```

### Example 2: Order with Nested Address

**Input:** `order.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<order>
    <orderId>ORD-123</orderId>
    <customer>
        <customerId>C001</customerId>
        <name>Bob Jones</name>
    </customer>
    <shipping>
        <street>456 Oak Ave</street>
        <city>Seattle</city>
        <zip>98101</zip>
    </shipping>
</order>
```

**Output:** `order.json`
```json
{
  "order": {
    "id": "ORD-123",
    "customerInfo": {
      "id": "C001",
      "name": "Bob Jones"
    },
    "shippingAddress": {
      "streetAddress": "456 Oak Ave",
      "cityName": "Seattle",
      "postalCode": "98101"
    }
  }
}
```

**Command:**
```bash
java -jar flowsmith.jar generate \
  --subsys ORD --app SHIP --func PROCESS \
  --sample-input order.xml \
  --sample-output order.json
```

## See Also

- [TESTING_GUIDE.md](TESTING_GUIDE.md) - Complete testing workflow
- [BAR_OVERRIDE_GUIDE.md](../automation/BAR_OVERRIDE_GUIDE.md) - Automated test configuration
- [MAPPING_FEATURE.md](MAPPING_FEATURE.md) - CSV-based mapping (alternative approach)
- [QUICK_START_MAPPING.md](QUICK_START_MAPPING.md) - Quick start guide

## Summary

The sample-based mapping feature provides the fastest path from requirement to working flow:

1. **Create samples** - XML input and JSON output
2. **Generate flow** - FlowSmith infers mappings automatically
3. **Deploy with BAR override** - Auto-configures test directories
4. **Test immediately** - Use your input sample as test data

No CSV creation, no manual mapping - just provide samples and go! 🚀