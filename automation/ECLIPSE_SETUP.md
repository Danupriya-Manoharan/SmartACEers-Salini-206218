# Eclipse Setup Guide for ACE FlowSmith Automation

## Quick Import into Eclipse/ACE Toolkit

### Method 1: Import Existing Project (Recommended)

1. **Open Eclipse or ACE Toolkit**

2. **Import the Project:**
   - File → Import → General → Existing Projects into Workspace
   - Click "Next"
   - Browse to: `<your-path>/SmartACEers-Salini-206218/automation`
   - Check "ACEFlowSmithAutomation" project
   - Click "Finish"

3. **Project Structure:**
   ```
   ACEFlowSmithAutomation/
   ├── src/
   │   └── com/
   │       └── flowsmith/
   │           └── automation/
   │               └── ACEAutomation.java
   ├── bin/ (compiled classes)
   ├── .project
   └── .classpath
   ```

4. **Run the Application:**
   - Right-click on `ACEAutomation.java`
   - Select "Run As" → "Java Application"
   - Enter project name when prompted (e.g., `XAJ_PUB_DEMO_TEST_FIL`)
   - Watch the automated deployment!

### Method 2: Create Run Configuration

1. **Create a Saved Run Configuration:**
   - Run → Run Configurations...
   - Right-click "Java Application" → New
   - Name: `ACE Deploy - Demo Project`
   - Project: `ACEFlowSmithAutomation`
   - Main class: `com.flowsmith.automation.ACEAutomation`
   - Arguments tab → Program arguments: `XAJ_PUB_DEMO_TEST_FIL`
   - Click "Apply" then "Run"

2. **Create Multiple Configurations:**
   - Duplicate the configuration for different projects
   - Name: `ACE Deploy - PUB_FILE`
   - Arguments: `XAJ_PUB_DEMO_TEST_FIL`
   
   - Name: `ACE Deploy - SUB_FILE`
   - Arguments: `XAJ_SUB_DEMO_TEST_FIL`

3. **Quick Run:**
   - Use toolbar dropdown to select configuration
   - Click Run button (green play icon)

## Features

### ✅ Auto-Detection
- **Automatically detects ACE Toolkit version** (12.0, 13.0, 14.0)
- Searches `C:\Program Files\IBM\ACE\` for any installed version
- No manual configuration needed!

### ✅ Interactive Mode
- When run without arguments, prompts for project name
- Perfect for "Run As → Java Application" in Eclipse
- User-friendly console interface

### ✅ Batch Mode
- Pass project name as argument for automation
- Can be called from scripts or other Java programs
- Supports CI/CD integration

## What It Does

```
╔════════════════════════════════════════════════════════════╗
║        ACE FlowSmith AI - Automated Deployment            ║
╚════════════════════════════════════════════════════════════╝

✓ Detected ACE Toolkit: C:\Program Files\IBM\ACE\12.0
✓ Project: XAJ_PUB_DEMO_TEST_FIL

📋 Step 1/6: Validating environment...
   ✓ Environment validated

📦 Step 2/6: Building BAR file...
   BIP8071I: Successful command completion.
   ✓ BAR file created: XAJ_PUB_DEMO_TEST_FIL.bar

🔧 Step 3/6: Starting Queue Manager...
   ✓ Queue Manager started

🚀 Step 4/6: Starting Integration Node...
   ✓ Integration Node started

📤 Step 5/6: Deploying BAR file...
   BIP8071I: Successful command completion.
   ✓ BAR file deployed

✅ Step 6/6: Verifying deployment...
   ✓ Deployment verified

✅ Deployment completed successfully!

Press Enter to exit...
```

## Troubleshooting

### Project Not Showing in Import
- Ensure `.project` and `.classpath` files exist in automation folder
- Try "Refresh" (F5) in Eclipse
- Check folder permissions

### Compilation Errors
- Ensure Java JDK is configured in Eclipse
- Window → Preferences → Java → Installed JREs
- Add JDK if only JRE is listed

### ACE Toolkit Not Detected
- Verify ACE is installed at `C:\Program Files\IBM\ACE\`
- Check version folder exists (12.0, 13.0, etc.)
- Ensure `server\bin` subdirectory exists

### Queue Manager Issues
- Install IBM MQ if not present
- Create Queue Manager: `crtmqm QM1`
- Start manually: `strmqm QM1`

## Configuration

Default values in `ACEAutomation.java`:
```java
QUEUE_MANAGER = "QM1"
INTEGRATION_NODE = "ACENODE"
INTEGRATION_SERVER = "default"
```

To customize:
1. Edit `src/com/flowsmith/automation/ACEAutomation.java`
2. Modify the constants at the top
3. Save (Eclipse auto-compiles)
4. Run again

## Complete Workflow

### 1. Generate ACE Project
```cmd
java -jar flowsmith.jar generate --pattern pub_file --subsys XAJ --app DEMO --func TEST
```

### 2. Deploy Using Eclipse
- Right-click `ACEAutomation.java`
- Run As → Java Application
- Enter: `XAJ_PUB_DEMO_TEST_FIL`
- Done in 30 seconds!

## Time Savings

| Task | Manual | Automated | Savings |
|------|--------|-----------|---------|
| BAR Creation | 15 min | 10 sec | 98% |
| Service Startup | 10 min | 15 sec | 97% |
| Deployment | 20 min | 5 sec | 99% |
| Verification | 10 min | 5 sec | 99% |
| **Total** | **55 min** | **35 sec** | **98%** |

## Benefits for Hackathon Demo

✅ **No batch scripts** - Pure Java, Eclipse-friendly
✅ **Auto-detection** - Works with ACE 12.0, 13.0, 14.0
✅ **Interactive mode** - Just right-click and run
✅ **Professional output** - Clean console with progress indicators
✅ **Error handling** - Clear messages for troubleshooting
✅ **Logging** - Creates `automation_log.txt` for audit trail

## Demo Tips

1. **Pre-create run configurations** for different patterns
2. **Keep Eclipse console visible** during demo
3. **Show the auto-detection** of ACE version
4. **Highlight the time savings** (55 min → 35 sec)
5. **Demonstrate error handling** if needed

---

**ACE FlowSmith AI** - Transforming ACE development into a faster, standardized, AI-assisted experience.