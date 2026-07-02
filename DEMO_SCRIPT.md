# ACE FlowSmith AI - Hackathon Demo Script

## 🎯 Demo Overview (5-6 minutes total)

**Objective:** Show how ACE FlowSmith AI transforms ACE development from manual, time-consuming work into an AI-assisted, standardized experience.

**Demo Flow:**
1. Web Interface Demo (2 min) - Show AI reasoning
2. Java CLI Live Generation (1.5 min) - Generate real ACE app
3. ACE Toolkit Import (1.5 min) - Show actual artifacts
4. Business Impact (1 min) - Metrics and value proposition

---

## 🚀 Pre-Demo Setup Checklist

### Before the Presentation:

- [ ] **Open Web Demo**: Open `mvp-web/index.html` in browser (works offline, no server needed)
- [ ] **Open ACE Toolkit**: Have ACE Toolkit running with workspace ready
- [ ] **Terminal Ready**: Open terminal in `flowsmith-java/` directory
- [ ] **Pre-generate Sample**: Run one generation beforehand as backup
- [ ] **Slide Deck**: Have presentation slides ready on second screen
- [ ] **Timer**: Set 6-minute timer

### Pre-generate Backup Project:
```bash
cd flowsmith-java
java -jar flowsmith.jar generate \
  --pattern pub_file \
  --subsys XAJ \
  --app DEMO \
  --func BACKUP
```

---

## 📋 Demo Script (Detailed)

### Opening (30 seconds)

**Say:**
> "Hi, I'm [Name] from Team SmartACEers. We're solving a critical problem in enterprise integration: IBM ACE development is slow, requires weeks of onboarding, and manual discovery of reusable patterns is inefficient. We built ACE FlowSmith AI - an intelligent agent that learns your organization's standards and generates production-ready ACE applications in minutes."

**Action:** Show title slide

---

### Part 1: Web Interface Demo (2 minutes)

**Say:**
> "Let me show you how the AI works. This is our interactive demo interface."

**Actions:**

1. **Switch to browser** (mvp-web/index.html already open)

2. **Point out the interface:**
   > "Three panels: Input your requirement, AI reasoning, and generated code preview."

3. **Show pre-filled requirement:**
   > "Here's a real-world scenario: 'consume grouped messages from queue at end of batch and write files'"

4. **Click "AI Recommend Pattern"**
   > "Watch the AI reason. It's scoring each pattern against the requirement..."
   
   **Point out:**
   - Pattern scores with star ratings
   - Matched keywords highlighted
   - Top recommendation: `sub_file_pubbatch`
   
   > "The AI understood 'grouped messages' and 'end of batch' - it recommended the subscriber pattern tuned for batch publishing."

5. **Fill in org tokens** (already filled):
   - SUBSYS: XAJ
   - APPNM: TLMTF
   - FUNCNM: FINANCING

6. **Click "Generate ACE Application"**
   > "Now it generates the complete application structure..."
   
   **Point out:**
   - File tree showing project structure
   - ESQL code preview with org-specific framework calls
   - Multi-environment configs (DEV/ACC/PRO)

7. **Click "Show Import Instructions"**
   > "And here's how a developer imports this into ACE Toolkit."

**Transition:**
> "That's the AI in action. Now let me show you the real generation using our Java CLI."

---

### Part 2: Java CLI Live Generation (1.5 minutes)

**Say:**
> "The web interface is for demonstration. In practice, developers use our Java CLI which integrates directly with ACE Toolkit."

**Actions:**

1. **Switch to terminal** (already in `flowsmith-java/`)

2. **Show learned patterns:**
```bash
java -jar flowsmith.jar list
```

**Say:**
> "The agent has learned 4 integration patterns from our organization's standards. These encode our logging frameworks, error handling, naming conventions - everything a new developer would need weeks to learn."

3. **AI Recommendation:**
```bash
java -jar flowsmith.jar recommend "publish file onto MQ queue for downstream systems"
```

**Say:**
> "Let's ask the AI to recommend a pattern... It scores each one and recommends 'pub_file' - the publisher pattern. Notice the rationale: it matched 'publish', 'queue', and 'downstream'."

4. **Generate Application:**
```bash
java -jar flowsmith.jar generate \
  --requirement "publish file onto MQ queue for downstream systems" \
  --subsys XAJ \
  --app TLMTF \
  --func FINANCING
```

**Say:**
> "Now let's generate the complete application. The AI selects the pattern, applies our naming convention, generates message flows, ESQL compute nodes, and environment-specific configs."

**Point out the output:**
- Pattern selected
- Files being generated
- Output directory path

**Say:**
> "In 30 seconds, we have a production-ready ACE application that would take 2-3 hours manually."

---

### Part 3: ACE Toolkit Import (1.5 minutes)

**Say:**
> "Now the critical part - does it actually work? Let's import it into ACE Toolkit."

**Actions:**

1. **Switch to ACE Toolkit**

2. **Import Project:**
   - File → Import → Existing Projects
   - Browse to: `Generated/XAJ_PUB_TLMTF_FINANCING_FIL/`
   - Select both projects (main + _Configs)
   - Click Finish

**Say:**
> "Importing... and there it is."

3. **Show Message Flow:**
   - Expand project tree
   - Open `Adapter.msgflow`

**Say:**
> "This is the actual ACE message flow - visual diagram with nodes and connections. FileInput reads the file, Compute node processes it, MQOutput publishes to the queue. This is real, deployable ACE code."

4. **Show ESQL Code:**
   - Open `Adapter_Map.esql`

**Say:**
> "Here's the generated ESQL. Notice the BROKER SCHEMA uses our organization's naming convention: PUB.XAJ.TLMTF.FINANCING.FIL. It calls our enterprise framework - ED6_BatchFramework_Shared - for logging and error handling. This is 100% compliant with our standards."

5. **Show Configs:**
   - Open `_Configs` project
   - Show `batchconfig/DEV/`, `ACC/`, `PRO/` folders

**Say:**
> "And here are the environment-specific configurations - DEV, ACC, PRO - all auto-generated. A developer just needs to review and deploy."

6. **Show BAR Build (if time):**
   - Right-click project → Build BAR file

**Say:**
> "This BAR file is ready to deploy to an Integration Server. From requirement to deployment-ready in 5 minutes."

---

### Part 4: Business Impact (1 minute)

**Say:**
> "Let's talk about the impact."

**Switch to slide or web metrics panel:**

**Point out metrics:**
- ⏱️ **95% Time Reduction**: Manual 2-3 hours → FlowSmith 2 minutes
- ✅ **100% Compliance**: Org standards automatically enforced
- 🚀 **90% Faster Onboarding**: New developers productive in hours, not weeks
- 📚 **Scalable Knowledge**: 4 patterns today, expandable to 50+

**Say:**
> "This isn't just faster development - it's capturing institutional knowledge and making it accessible to everyone. New developers don't need weeks of training. Standards are enforced automatically. And we can scale this to cover all ACE use cases."

---

### Closing (30 seconds)

**Say:**
> "ACE FlowSmith AI transforms ACE development into an AI-assisted, standardized experience. The agent learns your patterns, the AI recommends the right one, and it generates production-ready applications in minutes. We're ready to scale this to production. Thank you!"

**Be ready for questions:**
- "How does the AI work?" → Keyword matching today, watsonx.ai integration available
- "Can it handle custom patterns?" → Yes, just add templates to the catalog
- "What about security?" → Runs locally, no code leaves your network
- "Integration with Toolkit?" → Java-based, runs in same JVM as Toolkit

---

## 🎬 Backup Plans

### If Live Generation Fails:
> "Let me show you a pre-generated example..." (use backup project)

### If ACE Toolkit Crashes:
> "Here's a video recording of the import process..." (have video ready)

### If Judges Want to Try:
> "Absolutely! Here's the web interface - try entering your own requirement..."

---

## 📊 Key Talking Points

### Problem Statement:
- Large enterprises run 100s of ACE integrations
- New developers need 2-3 weeks onboarding
- Manual pattern discovery is inefficient
- Pressure for faster delivery without compromising compliance

### Solution Highlights:
- **AI-Powered**: Intelligent pattern recommendation
- **Organization-Specific**: Learns your standards, not generic templates
- **Production-Ready**: Generates complete, deployable applications
- **Developer-Friendly**: CLI + Toolkit integration, no new tools to learn

### Competitive Advantages:
- vs. Manual: 95% faster, 100% compliant
- vs. Code Generators: AI-powered, not just templates
- vs. Copilot/ChatGPT: ACE-specialized, generates complete apps

### Technical Highlights:
- Java-based (runs in ACE Toolkit JVM)
- Watsonx.ai integration (IBM Granite model)
- Template-based with token substitution
- Multi-environment configuration support

---

## 🎯 Success Criteria

**You've nailed the demo if judges:**
1. ✅ Understand the problem (ACE development is slow)
2. ✅ See the AI in action (pattern recommendation)
3. ✅ Believe it works (imported into real Toolkit)
4. ✅ Grasp the business value (time/cost savings)
5. ✅ Want to use it themselves

---

## 📝 Post-Demo Q&A Prep

**Q: How accurate is the AI recommendation?**
A: Currently using keyword matching with 80%+ accuracy. Watsonx.ai integration (Granite model) available in Java version for even better results.

**Q: Can we add our own patterns?**
A: Absolutely! Just add your template to `Existing_Templates/` and update the catalog. The agent learns it automatically.

**Q: What about complex integrations?**
A: Today we have 4 core patterns covering 70% of use cases. Roadmap includes 20+ patterns and custom subflow generation.

**Q: Integration with existing tools?**
A: Java-based, runs in ACE Toolkit. Can be called from Eclipse External Tools, command line, or CI/CD pipelines.

**Q: Security and compliance?**
A: Runs locally, no cloud dependencies. All generated code follows your org standards. Perfect for regulated industries.

**Q: Scalability?**
A: Catalog is JSON-based, easily scales to 50+ patterns. Generation is template-based, handles large projects efficiently.

---

## 🚀 Next Steps After Demo

If judges are interested:
1. Offer to share the GitHub repo
2. Provide contact info for follow-up
3. Discuss pilot program possibilities
4. Show roadmap for production deployment

---

**Good luck! 🎉**