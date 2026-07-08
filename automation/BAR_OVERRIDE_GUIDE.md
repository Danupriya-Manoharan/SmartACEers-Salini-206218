# BAR Override Feature Guide

## Overview

The BAR Override feature automatically configures test directories and file paths before deploying ACE flows, enabling easy end-to-end testing without manual configuration.

## What It Does

When deploying a flow with BAR override enabled, ACEDeployer will:

1. **Create test directories** automatically:
   - Input directory: `C:\temp\test\<appName>\in`
   - Output directory: `C:\temp\test\<appName>\out`

2. **Apply BAR overrides** to configure FileInput and FileOutput nodes:
   - Sets input directory path
   - Sets output directory path
   - Creates a new BAR file with overrides applied

3. **Deploy the configured BAR** to the integration server

## Configuration

### Required Parameters

Add these to your `ACEDeployer` configuration:

```java
deployer.setTestApp("MyApp");  // Application name for test directories
deployer.setFileInNodeLabel("File Input");  // Label of FileInput node
deployer.setFileOutNodeLabel("File Output");  // Label of FileOutput node
```

### Optional Parameters

```java
deployer.setTestBaseDir("D:\\testing");  // Default: C:\temp\test
```

## Usage Example

### 1. Basic Deployment with BAR Override

```java
ACEDeployer deployer = new ACEDeployer();
deployer.setIntegrationNodeName("TESTNODE");
deployer.setIntegrationServerName("default");
deployer.setBarFilePath("C:\\bars\\MyFlow.bar");
deployer.setApplicationName("MyApp");

// Enable BAR override
deployer.setTestApp("CustomerFlow");
deployer.setFileInNodeLabel("File Input");
deployer.setFileOutNodeLabel("File Output");

deployer.deploy();
```

### 2. Complete Workflow with Mapping

```java
// 1. Generate flow with mapping
FlowSmith.main(new String[]{
    "--requirement", "Convert XML customer data to JSON",
    "--mapping", "customer-mapping.csv",
    "--output", "CustomerFlow"
});

// 2. Deploy with BAR override
ACEDeployer deployer = new ACEDeployer();
deployer.setBarFilePath("CustomerFlow.bar");
deployer.setTestApp("CustomerFlow");
deployer.setFileInNodeLabel("File Input");
deployer.setFileOutNodeLabel("File Output");
deployer.deploy();

// 3. Test the flow
// - Place sample-input.xml in C:\temp\test\CustomerFlow\in
// - Check output in C:\temp\test\CustomerFlow\out
```

## How It Works

### 1. Directory Creation

```
C:\temp\test\
└── CustomerFlow\
    ├── in\      (input files go here)
    └── out\     (output files appear here)
```

### 2. BAR Override Command

The tool executes:
```cmd
mqsiapplybaroverride -b CustomerFlow.bar -o CustomerFlow_test.bar 
  -k "FlowName#File Input.inputDirectory" -v "C:\temp\test\CustomerFlow\in"
  -k "FlowName#File Output.outputDirectory" -v "C:\temp\test\CustomerFlow\out"
```

### 3. Deployment

The modified BAR file (`CustomerFlow_test.bar`) is deployed to the integration server.

## Testing Your Flow

### Step 1: Prepare Test Data

Create a sample XML file matching your mapping document:

```xml
<!-- sample-input.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<customer>
    <id>12345</id>
    <name>John Doe</name>
    <email>john@example.com</email>
</customer>
```

### Step 2: Place Input File

Copy the file to the input directory:
```cmd
copy sample-input.xml C:\temp\test\CustomerFlow\in\
```

### Step 3: Monitor Output

The flow will process the file and create output in:
```
C:\temp\test\CustomerFlow\out\sample-input.json
```

### Step 4: Verify Output

Check the JSON output matches your expected format:

```json
{
  "customer": {
    "customerId": "12345",
    "customerName": "John Doe",
    "emailAddress": "john@example.com"
  }
}
```

## Troubleshooting

### Issue: Directories Not Created

**Symptom**: Test directories don't exist after deployment

**Solution**: 
- Check Windows permissions for `C:\temp\test`
- Verify `testApp` is set correctly
- Check deployment logs for errors

### Issue: BAR Override Fails

**Symptom**: `mqsiapplybaroverride` command fails

**Solution**:
- Verify ACE Toolkit is installed and in PATH
- Check node labels match exactly (case-sensitive)
- Ensure original BAR file exists and is valid

### Issue: Files Not Processed

**Symptom**: Input files remain in `in` directory

**Solution**:
- Check integration server is running: `mqsilist`
- Verify flow is deployed: `mqsilist TESTNODE -d 2`
- Check flow logs for errors
- Ensure FileInput node pattern matches filename

### Issue: Wrong Output Format

**Symptom**: Output doesn't match expected JSON

**Solution**:
- Verify mapping document is correct
- Check ESQL code was generated properly
- Review flow logs for transformation errors
- Test mapping logic in isolation

## Advanced Configuration

### Custom Test Base Directory

```java
deployer.setTestBaseDir("D:\\integration-tests");
// Creates: D:\integration-tests\CustomerFlow\in and \out
```

### Multiple Flows

```java
// Flow 1
deployer.setTestApp("CustomerFlow");
deployer.deploy();

// Flow 2
deployer.setTestApp("OrderFlow");
deployer.deploy();

// Result:
// C:\temp\test\CustomerFlow\in, \out
// C:\temp\test\OrderFlow\in, \out
```

### Disabling BAR Override

Simply don't set `testApp`:

```java
// No BAR override - deploy as-is
deployer.setBarFilePath("MyFlow.bar");
deployer.deploy();
```

## Integration with CI/CD

### Jenkins Pipeline Example

```groovy
stage('Deploy and Test') {
    steps {
        // Deploy with BAR override
        bat """
            java -cp automation.jar com.flowsmith.automation.ACEDeployer ^
                --node TESTNODE ^
                --server default ^
                --bar CustomerFlow.bar ^
                --app CustomerFlow ^
                --testApp CustomerFlow ^
                --fileIn "File Input" ^
                --fileOut "File Output"
        """
        
        // Copy test data
        bat "copy test-data\\*.xml C:\\temp\\test\\CustomerFlow\\in\\"
        
        // Wait for processing
        sleep 10
        
        // Verify output
        bat "dir C:\\temp\\test\\CustomerFlow\\out"
    }
}
```

## Best Practices

1. **Use descriptive test app names**: `CustomerXMLtoJSON` instead of `Test1`

2. **Clean test directories** between runs:
   ```cmd
   del /Q C:\temp\test\CustomerFlow\in\*
   del /Q C:\temp\test\CustomerFlow\out\*
   ```

3. **Version your mapping documents**: Keep them in source control alongside flows

4. **Automate testing**: Create scripts to copy test data and verify output

5. **Use consistent node labels**: Standardize FileInput/FileOutput node names across flows

## See Also

- [TESTING_GUIDE.md](../flowsmith-java/TESTING_GUIDE.md) - Complete testing workflow
- [MAPPING_FEATURE.md](../flowsmith-java/MAPPING_FEATURE.md) - Field mapping documentation
- [QUICK_START_MAPPING.md](../flowsmith-java/QUICK_START_MAPPING.md) - Quick start guide