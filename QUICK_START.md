# ACE FlowSmith AI - Quick Start Guide

## 🎯 For IBM Hackathon Demo

This guide helps you set up and run the ACE FlowSmith AI demo on your Windows machine with ACE Toolkit.

---

## 📋 Prerequisites

✅ **Required:**
- IBM ACE Toolkit (version 11 or 12)
- Java 17+ (comes with ACE Toolkit)
- Web browser (Chrome, Firefox, Edge)

✅ **Optional:**
- IBM watsonx.ai credentials (for AI integration)

---

## 🚀 Setup Instructions (5 minutes)

### Step 1: Verify Java Installation

Open Command Prompt and verify Java is available:

```cmd
java -version
```

Expected output: `java version "17.x.x"` or higher

If not found, use the Java that comes with ACE Toolkit:
```cmd
"C:\Program Files\IBM\ACE\13.0.5.0\common\java17\bin\java.exe" -version
```

### Step 2: Test FlowSmith CLI

Navigate to the project directory:

```cmd
cd C:\path\to\SmartACEers-Salini-206218\flowsmith-java
```

List available patterns:

```cmd
java -jar flowsmith.jar list
```

Expected output:
```
[AI] Learned 4 reusable integration patterns from the org knowledge base:

  ID                   TYPE   CONNECT   TITLE
  ----------------------------------------------------------------------
  ptp_file             PTP    FILE      PTP File-to-File
  pub_file             PUB    FILE      PUB Publisher (File to MQ)
  sub_file_pubonline   SUB    FILE      SUB Subscriber (Online publishing)
  sub_file_pubbatch    SUB    FILE      SUB Subscriber (Batch publishing)
```

### Step 3: Generate Sample Projects

Generate one project for each pattern (for demo backup):

```cmd
REM Pattern 1: Point-to-Point
java -jar flowsmith.jar generate --pattern ptp_file --subsys XAJ --app DEMO --func TRANSFER

REM Pattern 2: Publisher
java -jar flowsmith.jar generate --pattern pub_file --subsys XAJ --app DEMO --func PUBLISH

REM Pattern 3: Subscriber (Online)
java -jar flowsmith.jar generate --pattern sub_file_pubonline --subsys XAJ --app DEMO --func SUBON

REM Pattern 4: Subscriber (Batch)
java -jar flowsmith.jar generate --pattern sub_file_pubbatch --subsys XAJ --app DEMO --func SUBBATCH
```

All projects will be generated in: `C:\Users\<username>\git\FlowSmith_Generated\`

### Step 4: Import Sample Project into ACE Toolkit

1. Open ACE Toolkit
2. **File → Import → Existing Projects into Workspace**
3. Browse to: `C:\Users\<username>\git\FlowSmith_Generated\XAJ_PUB_DEMO_PUBLISH_FIL\`
4. Select both projects:
   - `XAJ_PUB_DEMO_PUBLISH_FIL`
   - `XAJ_PUB_DEMO_PUBLISH_FIL_Configs`
5. Click **Finish**

### Step 5: Verify Generated Artifacts

In ACE Toolkit, expand the project and verify:

✅ **Message Flow:**
- Navigate to: `PUB/XAJ/DEMO/PUBLISH/FIL/`
- Open `Adapter.msgflow` → Should show visual flow diagram
- Open `FileConnector.msgflow` → Should show file connector flow

✅ **ESQL Code:**
- Open `Adapter_Map.esql` → Should show compute node logic
- Verify BROKER SCHEMA: `PUB.XAJ.DEMO.PUBLISH.FIL`

✅ **Configurations:**
- Open `XAJ_PUB_DEMO_PUBLISH_FIL_Configs` project
- Check `batchconfig/DEV/`, `ACC/`, `PRO/` folders
- Verify property files exist

### Step 6: Open Web Demo Interface

Simply open the HTML file in your browser:

```cmd
cd ..\mvp-web
start index.html
```

Or double-click: `mvp-web\index.html`

The web interface works completely offline - no server needed!

---

## 🎬 Demo Execution (Follow DEMO_SCRIPT.md)

### Quick Demo Commands

**1. Show Learned Patterns:**
```cmd
java -jar flowsmith.jar list
```

**2. AI Recommendation:**
```cmd
java -jar flowsmith.jar recommend "publish file onto MQ queue for downstream systems"
```

**3. Generate Application (Live):**
```cmd
java -jar flowsmith.jar generate ^
  --requirement "publish file onto MQ queue for downstream systems" ^
  --subsys XAJ ^
  --app TLMTF ^
  --func FINANCING
```

**4. Import to Toolkit:**
- File → Import → Existing Projects
- Browse to generated project
- Show message flows and ESQL

---

## 🔧 Optional: Configure watsonx.ai Integration

If you have IBM watsonx.ai credentials:

1. Copy `watsonx.properties.example` to `watsonx.properties`:
```cmd
copy watsonx.properties.example watsonx.properties
```

2. Edit `watsonx.properties` with your credentials:
```properties
url=https://us-south.ml.cloud.ibm.com
apikey=YOUR_IBM_CLOUD_API_KEY
projectId=YOUR_WATSONX_PROJECT_ID
modelId=ibm/granite-13b-chat-v2
```

3. Test watsonx.ai integration:
```cmd
java -jar flowsmith.jar recommend "consume messages from queue" --engine watsonx
```

---

## 📁 Project Structure

```
SmartACEers-Salini-206218/
├── flowsmith-java/              # Java CLI (main demo tool)
│   ├── flowsmith.jar           # Prebuilt executable
│   ├── patterns.txt            # Pattern catalog
│   └── watsonx.properties      # AI credentials (optional)
├── mvp-web/
│   └── index.html              # Interactive web demo (offline)
├── Existing_Templates/          # Pattern templates
│   ├── subsys_ptp_appnm_funcnm_file/
│   ├── subsys_pub_appnm_funcnm_file/
│   └── ...
├── Generated/                   # Generated projects (created on first run)
├── DEMO_SCRIPT.md              # Detailed demo walkthrough
└── QUICK_START.md              # This file
```

---

## 🎯 Demo Checklist

Before the hackathon presentation:

- [ ] Java working: `java -jar flowsmith.jar list`
- [ ] Sample projects generated (4 patterns)
- [ ] At least one project imported into ACE Toolkit
- [ ] Web demo opened in browser (`mvp-web/index.html`)
- [ ] ACE Toolkit running with workspace ready
- [ ] DEMO_SCRIPT.md printed or on second screen
- [ ] Timer set for 6 minutes
- [ ] Backup project ready in case live generation fails

---

## 🐛 Troubleshooting

### Issue: "java: command not found"

**Solution:** Use full path to Java from ACE Toolkit:
```cmd
"C:\Program Files\IBM\ACE\13.0.5.0\common\java17\bin\java.exe" -jar flowsmith.jar list
```

Or add to PATH temporarily:
```cmd
set PATH=%PATH%;C:\Program Files\IBM\ACE\13.0.5.0\common\java17\bin
```

### Issue: "Pattern not found"

**Solution:** Verify you're in the correct directory:
```cmd
cd C:\path\to\SmartACEers-Salini-206218\flowsmith-java
dir flowsmith.jar
dir patterns.txt
```

### Issue: Generated project won't import

**Solution:** 
1. Check the output directory: `C:\Users\<username>\git\FlowSmith_Generated\`
2. Verify both projects exist (main + _Configs)
3. Try importing each project separately

### Issue: Message flow won't open

**Solution:**
1. Verify ACE Toolkit version (11 or 12)
2. Check for errors in Error Log view
3. Try opening a different generated project

### Issue: Web demo not working

**Solution:**
1. Use a modern browser (Chrome, Firefox, Edge)
2. Check browser console for errors (F12)
3. Try opening in incognito/private mode

---

## 📞 Support

For issues during setup:
1. Check `flowsmith-java/README.md` for detailed documentation
2. Review `DEMO_SCRIPT.md` for demo walkthrough
3. Verify all prerequisites are installed

---

## 🎉 You're Ready!

Once you've completed the setup:
1. ✅ Java CLI working
2. ✅ Sample projects generated
3. ✅ One project imported into Toolkit
4. ✅ Web demo accessible

**You're ready for the hackathon demo!**

Follow `DEMO_SCRIPT.md` for the detailed presentation walkthrough.

Good luck! 🚀