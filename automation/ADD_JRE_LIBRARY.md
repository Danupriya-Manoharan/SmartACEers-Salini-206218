# How to Add JRE System Library

If the JRE System Library is not showing after import, follow these steps:

## Method 1: Automatic (Recommended)

1. **Right-click on the project** `ACEFlowSmithAutomation`
2. **Select "Build Path"** → **"Configure Build Path..."**
3. **Go to "Libraries" tab**
4. **Click "Add Library..."**
5. **Select "JRE System Library"**
6. **Click "Next"**
7. **Select "Workspace default JRE"** (or choose a specific JRE/JDK)
8. **Click "Finish"**
9. **Click "Apply and Close"**

## Method 2: Using Project Properties

1. **Right-click on the project** `ACEFlowSmithAutomation`
2. **Select "Properties"**
3. **Go to "Java Build Path"**
4. **Click "Libraries" tab**
5. **If JRE System Library is missing:**
   - Click "Add Library..."
   - Select "JRE System Library"
   - Click "Next"
   - Choose "Workspace default JRE" or a specific JRE
   - Click "Finish"
6. **Click "Apply and Close"**

## Method 3: Fix Project Setup

If JRE is still not showing:

1. **Right-click on the project**
2. **Select "Properties"**
3. **Go to "Project Facets"**
4. **Check "Java"** (if not checked)
5. **Set Java version to 17 or higher**
6. **Click "Apply and Close"**

## Method 4: Clean and Rebuild

After adding JRE:

1. **Project** → **Clean...**
2. **Select "ACEFlowSmithAutomation"**
3. **Click "Clean"**
4. **Eclipse will rebuild the project**

## Verify JRE is Added

After adding, you should see in Project Explorer:

```
ACEFlowSmithAutomation
├── src
│   └── com.flowsmith.automation
│       ├── ACEAutomation.java
│       └── ACEDeployer.java
├── JRE System Library [JavaSE-17]  ← Should appear here
│   ├── rt.jar
│   ├── jce.jar
│   └── ... (other JRE libraries)
└── bin
```

## Troubleshooting

### Issue: "JRE System Library" option not available

**Solution:** Install JDK (not just JRE)

1. Download and install Java JDK 17 or higher
2. In Eclipse: **Window** → **Preferences** → **Java** → **Installed JREs**
3. Click **"Add..."**
4. Select **"Standard VM"**
5. Click **"Next"**
6. Click **"Directory..."** and browse to JDK installation (e.g., `C:\Program Files\Java\jdk-17`)
7. Click **"Finish"**
8. **Check the box** next to the newly added JDK to make it default
9. Click **"Apply and Close"**

### Issue: Multiple JRE entries showing

**Solution:** Remove duplicates

1. Right-click project → **Build Path** → **Configure Build Path...**
2. Go to **Libraries** tab
3. Select duplicate JRE entries
4. Click **"Remove"**
5. Keep only one JRE System Library
6. Click **"Apply and Close"**

### Issue: Compilation errors after adding JRE

**Solution:** Update compiler compliance

1. Right-click project → **Properties**
2. Go to **Java Compiler**
3. Uncheck **"Enable project specific settings"** (to use workspace settings)
   OR
4. Set **Compiler compliance level** to **17** or higher
5. Click **"Apply and Close"**
6. When prompted, click **"Yes"** to rebuild

## Quick Check

After adding JRE, test if it works:

1. Open `ACEAutomation.java`
2. Look for any red error markers - should be none
3. Right-click on the file
4. You should see **"Run As"** → **"Java Application"**
5. Click it to test

## Expected Console Output

```
╔════════════════════════════════════════════════════════════╗
║        ACE FlowSmith AI - Automated Deployment            ║
╚════════════════════════════════════════════════════════════╝

✓ Detected ACE Toolkit: C:\Program Files\IBM\ACE\12.0
✓ Project: XAJ_PUB_DEMO_TEST_FIL
```

## Alternative: Re-import Project

If nothing works, try re-importing:

1. **Delete the project from workspace** (don't delete files on disk)
   - Right-click project → Delete
   - **UNCHECK** "Delete project contents on disk"
   - Click "OK"

2. **Re-import:**
   - File → Import → General → Existing Projects into Workspace
   - Browse to automation folder
   - Click Finish

3. **Add JRE using Method 1 above**

## Still Having Issues?

Check Eclipse installation:
- **Help** → **About Eclipse** → **Installation Details**
- Ensure "Eclipse IDE for Java Developers" or "Eclipse IDE for Enterprise Java Developers" is installed
- If not, download and install the correct Eclipse package

---

**Once JRE System Library is added, you're ready to run the automation!**