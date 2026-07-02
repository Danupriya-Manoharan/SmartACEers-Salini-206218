## ⚠️ IMPORTANT: Batch Scripts Not Allowed

**If batch scripts (.bat files) are not allowed in your environment**, use the pure Java solution instead:

👉 **See [JAVA_USAGE.md](JAVA_USAGE.md) for the recommended pure Java approach**

The Java solution (`ACEAutomation.java`) provides the same automation without batch scripts:
```cmd
cd automation
javac ACEAutomation.java
java com.flowsmith.automation.ACEAutomation <PROJECT_NAME>
```

## 🎯 Eclipse/ACE Toolkit Users (Recommended)

**For easy "Right-click → Run As → Java Application" experience:**

👉 **See [ECLIPSE_SETUP.md](ECLIPSE_SETUP.md) for complete Eclipse setup guide**

Quick steps:
1. File → Import → Existing Projects into Workspace
2. Browse to `automation` folder
3. Right-click `ACEAutomation.java` → Run As → Java Application
4. Enter project name when prompted
5. Done in 30 seconds!

**Features:**
- ✅ Auto-detects ACE Toolkit version (12.0, 13.0, 14.0)
- ✅ Interactive mode - prompts for project name
- ✅ No batch scripts required
- ✅ Professional console output

---

---

# ACE FlowSmith AI - Automated Deployment

## Overview

This automation suite provides end-to-end deployment automation for ACE FlowSmith AI generated applications:

1. **Build BAR file** from generated project
2. **Start Queue Manager** (if not running)
3. **Start Integration Node** (if not running)
4. **Deploy BAR file** to Integration Server
5. **Verify deployment** status

---

## 🚀 Quick Start

### **Option 1: Java Deployer (Recommended)**

```cmd
cd automation
javac ACEDeployer.java
java com.flowsmith.automation.ACEDeployer XAJ_PUB_TLMTF_FINANCING_FIL MB8QMGR Test_node_test default
```

### **Option 2: Batch Script**

```cmd
cd automation
deploy.bat
```

---

## 📋 Prerequisites

### **Required:**
- IBM ACE (App Connect Enterprise) installed
- ACE commands in PATH:
  - `mqsicreatebar` - Build BAR files
  - `mqsideploy` - Deploy BAR files
  - `mqsistart` - Start Integration Node
  - `mqsilist` - List Integration Nodes
  - `strmqm` - Start Queue Manager
  - `dspmq` - Display Queue Manager status

### **Verify Installation:**

```cmd
REM Check ACE commands
mqsicreatebar -?
mqsideploy -?

REM Check MQ commands
strmqm -?
dspmq
```

---

## 🔧 Configuration

### **Default Values:**

| Parameter | Default Value | Description |
|-----------|---------------|-------------|
| Queue Manager | `MB8QMGR` | IBM MQ Queue Manager name |
| Integration Node | `Test_node_test` | ACE Integration Node name |
| Integration Server | `default` | Integration Server within the node |
| Workspace | `%USERPROFILE%\git\FlowSmith_Generated` | Generated projects location |

### **Customize:**

You can override defaults by:
1. **Command line arguments** (Java)
2. **Interactive prompts** (Batch script)
3. **Edit the scripts** directly

---

## 📖 Usage

### **Java Deployer**

#### **Compile Once:**
```cmd
cd automation
javac ACEDeployer.java
```

#### **Interactive Mode:**
```cmd
java com.flowsmith.automation.ACEDeployer
```
Then enter values when prompted.

#### **Command Line Mode:**
```cmd
java com.flowsmith.automation.ACEDeployer <projectName> [queueManager] [integrationNode] [integrationServer]
```

**Examples:**

```cmd
REM Use all defaults
java com.flowsmith.automation.ACEDeployer XAJ_PUB_TLMTF_FINANCING_FIL

REM Custom Queue Manager
java com.flowsmith.automation.ACEDeployer XAJ_PUB_TLMTF_FINANCING_FIL MY_QMGR

REM Full custom configuration
java com.flowsmith.automation.ACEDeployer XAJ_PUB_TLMTF_FINANCING_FIL MB8QMGR MyNode MyServer
```

### **Batch Script**

```cmd
cd automation
deploy.bat
```

Follow the interactive prompts to enter:
1. Project Name
2. Queue Manager Name (or press Enter for default)
3. Integration Node Name (or press Enter for default)
4. Integration Server Name (or press Enter for default)

---

## 🔄 Deployment Workflow

### **Step 1: Build BAR File**

```
mqsicreatebar -data <workspace> -b <barFile> -a <projectName> -o
```

- Reads the generated ACE project
- Compiles message flows and ESQL
- Creates deployable BAR file
- Output: `<workspace>\BAR_Files\<projectName>.bar`

### **Step 2: Start Queue Manager**

```
dspmq -m <queueManager>  # Check status
strmqm <queueManager>    # Start if not running
```

- Checks if Queue Manager is already running
- Starts it if needed
- Waits 5 seconds for startup

### **Step 3: Start Integration Node**

```
mqsilist                 # Check status
mqsistart <integrationNode>  # Start if not running
```

- Checks if Integration Node is already running
- Starts it if needed
- Waits 10 seconds for startup

### **Step 4: Deploy BAR File**

```
mqsideploy <integrationNode> -e <integrationServer> -a <barFile>
```

- Deploys BAR file to specified Integration Server
- Waits 5 seconds for deployment to complete

### **Step 5: Verify Deployment**

```
mqsilist <integrationNode> -e <integrationServer> -d 2
```

- Lists deployed applications
- Shows message flow status
- Displays deployment details

---

## 📁 File Structure

```
automation/
├── README.md              # This file
├── ACEDeployer.java       # Java deployment automation
├── deploy.bat             # Windows batch script
└── (generated)
    └── BAR_Files/         # Created automatically
        └── *.bar          # Generated BAR files
```

---

## 🎯 Integration with FlowSmith

### **Complete End-to-End Workflow:**

```cmd
REM Step 1: Generate ACE application
cd flowsmith-java
java -jar flowsmith.jar generate ^
  --pattern pub_file ^
  --subsys XAJ ^
  --app TLMTF ^
  --func FINANCING

REM Step 2: Deploy automatically
cd ..\automation
java com.flowsmith.automation.ACEDeployer XAJ_PUB_TLMTF_FINANCING_FIL
```

### **One-Line Deployment:**

```cmd
java -jar flowsmith.jar generate --pattern pub_file --subsys XAJ --app TLMTF --func FINANCING && cd automation && java com.flowsmith.automation.ACEDeployer XAJ_PUB_TLMTF_FINANCING_FIL
```

---

## 🐛 Troubleshooting

### **Error: "mqsicreatebar: command not found"**

**Solution:** Add ACE to PATH:
```cmd
set PATH=%PATH%;C:\Program Files\IBM\ACE\12.0\server\bin
```

Or run from ACE Command Console.

### **Error: "Failed to create BAR file"**

**Possible causes:**
1. Project doesn't exist in workspace
2. Project name is incorrect
3. Workspace path is wrong

**Solution:** Verify project exists:
```cmd
dir %USERPROFILE%\git\FlowSmith_Generated\<projectName>
```

### **Error: "Failed to start Queue Manager"**

**Possible causes:**
1. Queue Manager doesn't exist
2. Insufficient permissions
3. MQ not installed

**Solution:** Create Queue Manager:
```cmd
crtmqm MB8QMGR
```

### **Error: "Failed to start Integration Node"**

**Possible causes:**
1. Integration Node doesn't exist
2. Queue Manager not running
3. Port conflict

**Solution:** Create Integration Node:
```cmd
mqsicreatebroker Test_node_test -q MB8QMGR
```

### **Error: "Failed to deploy BAR file"**

**Possible causes:**
1. Integration Server doesn't exist
2. BAR file is corrupted
3. Deployment conflicts

**Solution:** Check Integration Server:
```cmd
mqsilist Test_node_test
```

---

## 📊 Output Example

```
========================================================================
  ACE FlowSmith AI - Automated Deployment
========================================================================

Configuration:
  Project Name      : XAJ_PUB_TLMTF_FINANCING_FIL
  Queue Manager     : MB8QMGR
  Integration Node  : Test_node_test
  Integration Server: default
  Workspace         : C:\Users\username\git\FlowSmith_Generated
  BAR File          : C:\Users\username\git\FlowSmith_Generated\BAR_Files\XAJ_PUB_TLMTF_FINANCING_FIL.bar

[Step 1/4] Building BAR file...
========================================================================
Executing: mqsicreatebar -data "C:\Users\username\git\FlowSmith_Generated" -b "C:\Users\username\git\FlowSmith_Generated\BAR_Files\XAJ_PUB_TLMTF_FINANCING_FIL.bar" -a "XAJ_PUB_TLMTF_FINANCING_FIL" -o
SUCCESS: BAR file created

[Step 2/4] Starting Queue Manager...
========================================================================
Queue Manager MB8QMGR is already running

[Step 3/4] Starting Integration Node...
========================================================================
Integration Node Test_node_test is already running

[Step 4/4] Deploying BAR file to Integration Server...
========================================================================
Deploying to Test_node_test:default...
SUCCESS: BAR file deployed

[Verification] Checking deployment status...
========================================================================
BIP1286I: Integration server 'default' has successfully deployed application 'XAJ_PUB_TLMTF_FINANCING_FIL'.

========================================================================
  Deployment Complete!
========================================================================

  Project: XAJ_PUB_TLMTF_FINANCING_FIL
  BAR File: C:\Users\username\git\FlowSmith_Generated\BAR_Files\XAJ_PUB_TLMTF_FINANCING_FIL.bar
  Deployed to: Test_node_test:default

  Next Steps:
  1. Test the deployed flow
  2. Monitor logs: mqsireadlog Test_node_test
  3. Check message flow status in ACE Toolkit

========================================================================
```

---

## 🎬 Demo Integration

### **For Hackathon Demo:**

1. **Generate application** using FlowSmith
2. **Show automation** deploying it
3. **Verify in Toolkit** that it's running

**Demo Script Addition:**

```
After showing generated code in Toolkit:

"Now watch as we deploy this to a running Integration Server with one command..."

[Run: java com.flowsmith.automation.ACEDeployer XAJ_PUB_TLMTF_FINANCING_FIL]

"In 30 seconds, the application is built, deployed, and running. 
From requirement to production-ready in under 5 minutes total."
```

---

## 🔒 Security Notes

- Scripts run with current user permissions
- No credentials are stored
- Queue Manager and Integration Node must be pre-configured
- BAR files are stored locally in workspace

---

## 📞 Support

For issues or questions:
1. Check ACE installation: `mqsiversion`
2. Verify MQ installation: `dspmqver`
3. Review ACE logs: `mqsireadlog <integrationNode>`
4. Check deployment status: `mqsilist <integrationNode> -e <integrationServer> -d 2`

---

## 🚀 Advanced Usage

### **Deploy Multiple Projects:**

```cmd
for %%P in (PROJECT1 PROJECT2 PROJECT3) do (
    java com.flowsmith.automation.ACEDeployer %%P
)
```

### **Deploy to Different Environments:**

```cmd
REM DEV
java com.flowsmith.automation.ACEDeployer MyProject QMGR_DEV NODE_DEV SERVER_DEV

REM ACC
java com.flowsmith.automation.ACEDeployer MyProject QMGR_ACC NODE_ACC SERVER_ACC

REM PRO
java com.flowsmith.automation.ACEDeployer MyProject QMGR_PRO NODE_PRO SERVER_PRO
```

### **CI/CD Integration:**

```cmd
REM Jenkins/GitLab CI pipeline
java -jar flowsmith.jar generate --pattern %PATTERN% --subsys %SUBSYS% --app %APP% --func %FUNC%
java com.flowsmith.automation.ACEDeployer %PROJECT_NAME% %QMGR% %NODE% %SERVER%
```

---

**Automated deployment makes FlowSmith even more powerful! 🚀**