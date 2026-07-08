# Testing Guide - Field Mapping Feature

## Overview

This guide shows you how to test your deployed ACE flow with sample XML documents that match your mapping configuration.

## Complete Testing Workflow

### Step 1: Create Mapping Document

**example-mapping.csv:**
```csv
Source Field (XML),Target Field (JSON)
customer/id,customer.customerId
customer/name,customer.fullName
customer/email,customer.email
order/orderId,order.id
order/date,order.orderDate
order/amount,order.totalAmount
```

### Step 2: Generate ACE Flow

```bash
java -jar flowsmith.jar generate \
  --pattern ptp_file \
  --subsys XAJ --app CUST --func TRANSFORM \
  --mapping example-mapping.csv
```

### Step 3: Create Sample XML Input

Based on your mapping document, create a sample XML file:

**sample-input.xml:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<root>
    <customer>
        <id>12345</id>
        <name>John Doe</name>
        <email>john.doe@example.com</email>
    </customer>
    <order>
        <orderId>ORD-2024-001</orderId>
        <date>2024-07-08</date>
        <amount>1500.00</amount>
    </order>
</root>
```

### Step 4: Deploy to Integration Server

1. **Import to ACE Toolkit:**
   - File → Import → Existing Projects
   - Select generated project

2. **Build BAR File:**
   - Right-click project → New → BAR file
   - Add message flows and ESQL
   - Build BAR

3. **Deploy to Integration Server:**
   - Use `mqsideploy` command or ACE Web UI
   ```bash
   mqsideploy -i default -a XAJ_PTP_CUST_TRANSFORM_FIL.bar
   ```

### Step 5: Test the Flow

#### Option A: File-Based Testing (PTP Pattern)

1. **Place input file:**
   ```bash
   copy sample-input.xml C:\ACE\input\
   ```

2. **Check output:**
   ```bash
   type C:\ACE\output\sample-input.xml
   ```

3. **Expected JSON output:**
   ```json
   {
       "customer": {
           "customerId": "12345",
           "fullName": "John Doe",
           "email": "john.doe@example.com"
       },
       "order": {
           "id": "ORD-2024-001",
           "orderDate": "2024-07-08",
           "totalAmount": "1500.00"
       }
   }
   ```

#### Option B: MQ Testing (PUB/SUB Patterns)

1. **Put message to input queue:**
   ```bash
   # Using amqsput
   amqsput INPUT.QUEUE QM1 < sample-input.xml
   ```

2. **Get message from output queue:**
   ```bash
   # Using amqsget
   amqsget OUTPUT.QUEUE QM1 > output.json
   ```

3. **Verify JSON output**

#### Option C: HTTP Testing (REST API Pattern)

1. **Send HTTP POST:**
   ```bash
   curl -X POST http://localhost:7800/transform \
     -H "Content-Type: application/xml" \
     -d @sample-input.xml
   ```

2. **Verify JSON response**

### Step 6: Verify Mapping

**Checklist:**
- [ ] All XML fields mapped to correct JSON fields
- [ ] Field names match mapping document
- [ ] Data values preserved correctly
- [ ] JSON structure is valid
- [ ] No missing fields
- [ ] No extra fields

---

## Automated Test Sample Generator

### Create Test Data Generator Script

**generate-test-xml.bat:**
```batch
@echo off
REM Generate sample XML from mapping CSV

set MAPPING_FILE=%1
set OUTPUT_FILE=%2

if "%MAPPING_FILE%"=="" (
    echo Usage: generate-test-xml.bat mapping.csv output.xml
    exit /b 1
)

if "%OUTPUT_FILE%"=="" set OUTPUT_FILE=sample-input.xml

echo ^<?xml version="1.0" encoding="UTF-8"?^> > %OUTPUT_FILE%
echo ^<root^> >> %OUTPUT_FILE%

REM Parse CSV and generate XML structure
REM This is a simple example - enhance as needed
echo     ^<customer^> >> %OUTPUT_FILE%
echo         ^<id^>12345^</id^> >> %OUTPUT_FILE%
echo         ^<name^>John Doe^</name^> >> %OUTPUT_FILE%
echo         ^<email^>john.doe@example.com^</email^> >> %OUTPUT_FILE%
echo     ^</customer^> >> %OUTPUT_FILE%
echo     ^<order^> >> %OUTPUT_FILE%
echo         ^<orderId^>ORD-2024-001^</orderId^> >> %OUTPUT_FILE%
echo         ^<date^>2024-07-08^</date^> >> %OUTPUT_FILE%
echo         ^<amount^>1500.00^</amount^> >> %OUTPUT_FILE%
echo     ^</order^> >> %OUTPUT_FILE%
echo ^</root^> >> %OUTPUT_FILE%

echo Sample XML generated: %OUTPUT_FILE%
```

---

## Testing Scenarios

### Scenario 1: Simple Field Mapping

**Mapping:**
```csv
customer/name,customer.fullName
```

**Input XML:**
```xml
<root>
    <customer>
        <name>John Doe</name>
    </customer>
</root>
```

**Expected JSON:**
```json
{
    "customer": {
        "fullName": "John Doe"
    }
}
```

### Scenario 2: Nested Structure

**Mapping:**
```csv
customer/address/street,customer.address.streetAddress
customer/address/city,customer.address.city
```

**Input XML:**
```xml
<root>
    <customer>
        <address>
            <street>123 Main St</street>
            <city>New York</city>
        </address>
    </customer>
</root>
```

**Expected JSON:**
```json
{
    "customer": {
        "address": {
            "streetAddress": "123 Main St",
            "city": "New York"
        }
    }
}
```

### Scenario 3: Multiple Records

**Mapping:**
```csv
item/id,items.itemId
item/name,items.itemName
```

**Input XML:**
```xml
<root>
    <item>
        <id>001</id>
        <name>Product A</name>
    </item>
    <item>
        <id>002</id>
        <name>Product B</name>
    </item>
</root>
```

**Expected JSON:**
```json
{
    "items": [
        {
            "itemId": "001",
            "itemName": "Product A"
        },
        {
            "itemId": "002",
            "itemName": "Product B"
        }
    ]
}
```

---

## Debugging Tips

### Check Generated ESQL

1. **Open Adapter_Compute.esql** in ACE Toolkit
2. **Verify mapping statements:**
   ```esql
   SET OutputRoot.JSON.Data.customer.customerId = InputRoot.XMLNSC.customer.id;
   ```
3. **Check field paths match your XML structure**

### Enable Flow Monitoring

1. **In ACE Toolkit:**
   - Right-click flow → Start Recording
   - Process test message
   - View message tree at each node

2. **Check logs:**
   ```bash
   # View integration server logs
   type C:\ProgramData\IBM\MQSI\components\default\stdout
   ```

### Common Issues

**Issue: Field not mapped**
- Check XML path matches mapping document
- Verify ESQL syntax is correct
- Check for typos in field names

**Issue: JSON structure incorrect**
- Review generated ESQL
- Check OutputRoot.JSON.Data structure
- Verify CREATE FIELD statements

**Issue: Data type mismatch**
- XML values are strings by default
- Add CAST if needed in ESQL
- Check for numeric/date conversions

---

## Performance Testing

### Load Testing

1. **Create multiple test files:**
   ```bash
   for /L %i in (1,1,100) do copy sample-input.xml input\test-%i.xml
   ```

2. **Monitor processing:**
   - Check integration server CPU/memory
   - Measure throughput (messages/second)
   - Monitor queue depths

3. **Verify all outputs:**
   ```bash
   dir output\*.json /b | find /c ".json"
   ```

---

## Validation Checklist

### Pre-Deployment
- [ ] Mapping document reviewed
- [ ] ESQL code generated correctly
- [ ] Sample XML created
- [ ] Expected JSON documented

### Post-Deployment
- [ ] Flow deployed successfully
- [ ] Test message processed
- [ ] Output matches expected JSON
- [ ] All fields mapped correctly
- [ ] No errors in logs

### Production Readiness
- [ ] Load testing completed
- [ ] Error handling tested
- [ ] Monitoring configured
- [ ] Documentation updated

---

## Sample Test Suite

### test-suite.bat

```batch
@echo off
echo ========================================
echo ACE Flow Mapping Test Suite
echo ========================================

REM Test 1: Simple mapping
echo Test 1: Simple field mapping...
copy test-data\simple-input.xml input\
timeout /t 2 /nobreak > nul
if exist output\simple-input.json (
    echo [PASS] Simple mapping
) else (
    echo [FAIL] Simple mapping
)

REM Test 2: Nested structure
echo Test 2: Nested structure...
copy test-data\nested-input.xml input\
timeout /t 2 /nobreak > nul
if exist output\nested-input.json (
    echo [PASS] Nested structure
) else (
    echo [FAIL] Nested structure
)

REM Test 3: Multiple records
echo Test 3: Multiple records...
copy test-data\multiple-input.xml input\
timeout /t 2 /nobreak > nul
if exist output\multiple-input.json (
    echo [PASS] Multiple records
) else (
    echo [FAIL] Multiple records
)

echo ========================================
echo Test suite completed
echo ========================================
```

---

## Next Steps

1. **Generate your flow** with mapping document
2. **Create sample XML** based on your mappings
3. **Deploy to integration server**
4. **Run test** with sample XML
5. **Verify JSON output** matches expectations
6. **Iterate** if needed

---

## Support

For issues or questions:
- Check ESQL in Adapter_Compute.esql
- Review integration server logs
- Verify mapping document format
- Test with simple examples first
- Gradually add complexity

**Happy Testing!** 🚀