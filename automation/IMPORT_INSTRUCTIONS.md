# How to Import into ACE Toolkit / Eclipse

## Problem: "Run As → Java Application" Not Showing?

This happens when Eclipse doesn't recognize the project as a Java project. Follow these steps:

## Solution: Proper Import Steps

### Step 1: Close ACE Toolkit/Eclipse
- Close the application completely
- This ensures clean import

### Step 2: Import the Project

1. **Open ACE Toolkit or Eclipse**

2. **Import as Existing Project:**
   - File → Import...
   - Select "General" → "Existing Projects into Workspace"
   - Click "Next"

3. **Select the automation folder:**
   - Click "Browse..." next to "Select root directory"
   - Navigate to: `<your-path>/SmartACEers-Salini-206218/automation`
   - You should see "ACEFlowSmithAutomation" checked in the Projects list
   - **IMPORTANT:** Make sure "Copy projects into workspace" is UNCHECKED
   - Click "Finish"

### Step 3: Verify Java Nature

After import, you should see:
```
ACEFlowSmithAutomation
├── src
│   └── com.flowsmith.automation
│       ├── ACEAutomation.java
│       └── ACEDeployer.java
├── JRE System Library [JavaSE-1.8]
└── bin (empty initially)
```

If you see a folder icon (not a Java project icon), continue to Step 4.

### Step 4: Add Java Nature (If Needed)

If the project doesn't show as a Java project:

1. **Right-click on the project** → Configure → Convert to Java Project
   
   OR

2. **Manually add Java nature:**
   - Right-click project → Properties
   - Select "Project Natures"
   - Click "Add..."
   - Select "Java"
   - Click "OK"

### Step 5: Configure Build Path (If Needed)

1. Right-click project → Properties
2. Select "Java Build Path"
3. In "Source" tab, ensure `src` folder is listed
4. In "Libraries" tab, ensure JRE System Library is present
5. Click "Apply and Close"

### Step 6: Clean and Build

1. Project → Clean...

## Step 8: Add JRE System Library (If Missing)

If you don't see "JRE System Library" in the project:

**Quick Fix:**
1. Right-click project → **Build Path** → **Configure Build Path...**
2. Go to **Libraries** tab
3. Click **"Add Library..."**
4. Select **"JRE System Library"**
5. Click **"Next"** → **"Finish"** → **"Apply and Close"**

👉 **See [ADD_JRE_LIBRARY.md](ADD_JRE_LIBRARY.md) for detailed JRE setup instructions**

2. Select "ACEFlowSmithAutomation"
3. Click "Clean"
4. Eclipse will rebuild the project

### Step 7: Run the Application

Now you should be able to:

1. **Navigate to the Java file:**
   - Expand: src → com.flowsmith.automation
   - Open: ACEAutomation.java

2. **Right-click on ACEAutomation.java**
   - You should now see "Run As" → "Java Application"
   - Click it!

3. **Enter project name when prompted:**
   ```
   Enter project name (e.g., XAJ_PUB_DEMO_TEST_FIL): XAJ_PUB_DEMO_TEST_FIL
   ```

4. **Watch the automation run!**

## Alternative: Create New Java Project

If import still doesn't work:

1. **Create new Java Project:**
   - File → New → Java Project
   - Name: ACEFlowSmithAutomation
   - Click "Finish"

2. **Copy source files:**
   - Right-click on `src` folder → New → Package
   - Name: `com.flowsmith.automation`
   - Copy contents of `ACEAutomation.java` and `ACEDeployer.java` into this package

3. **Run:**
   - Right-click ACEAutomation.java → Run As → Java Application

## Troubleshooting

### Issue: "Run As → Java Application" still not showing

**Solution 1: Check file extension**
- Ensure file is named `ACEAutomation.java` (not .txt or other)
- Right-click → Properties → check file name

**Solution 2: Refresh project**
- Right-click project → Refresh (F5)
- Project → Clean → Clean all projects

**Solution 3: Check Java perspective**
- Window → Perspective → Open Perspective → Java
- This ensures Java-specific menus are available

**Solution 4: Verify JDK (not just JRE)**
- Window → Preferences → Java → Installed JREs
- Ensure a JDK is listed (not just JRE)
- If only JRE, click "Add..." and add JDK

### Issue: Compilation errors

**Check package declaration:**
```java
package com.flowsmith.automation;  // Must match folder structure
```

**Check imports:**
```java
import java.io.*;
import java.util.*;
```

### Issue: "Main class not found"

**Ensure main method exists:**
```java
public static void main(String[] args) {
    // ...
}
```

## Quick Test

After successful import, test with:

1. Right-click `ACEAutomation.java`
2. Run As → Java Application
3. Enter a test project name
4. Should see:
   ```
   ╔════════════════════════════════════════════════════════════╗
   ║        ACE FlowSmith AI - Automated Deployment            ║
   ╚════════════════════════════════════════════════════════════╝
   
   ✓ Detected ACE Toolkit: C:\Program Files\IBM\ACE\12.0
   ```

## Project Structure

Correct structure after import:
```
ACEFlowSmithAutomation/
├── .classpath              (Eclipse classpath config)
├── .project                (Eclipse project config)
├── .settings/              (Java compiler settings)
│   └── org.eclipse.jdt.core.prefs
├── src/                    (Source folder)
│   └── com/
│       └── flowsmith/
│           └── automation/
│               ├── ACEAutomation.java    (Main automation)
│               └── ACEDeployer.java      (Alternative deployer)
├── bin/                    (Compiled .class files - auto-generated)
└── README.md, etc.         (Documentation)
```

## Success Indicators

✅ Project shows Java icon (not folder icon)
✅ `src` folder shows package icon
✅ `com.flowsmith.automation` shows package icon
✅ `.java` files show Java file icon
✅ Right-click menu shows "Run As → Java Application"
✅ Console shows output when run

## Need Help?

If still having issues:
1. Check Eclipse error log: Window → Show View → Error Log
2. Verify Java version: Help → About → Installation Details
3. Try creating a simple "Hello World" Java project to verify Eclipse Java setup

---

**Once imported successfully, you can run the automation with a single right-click!**