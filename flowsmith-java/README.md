# ACE FlowSmith AI — Java MVP (IBM competition build)

An **AI-ready agent** that accelerates IBM App Connect Enterprise (ACE)
development. It learns an organisation's reusable integration patterns and
standards, interprets a plain-English requirement, recommends the right pattern,
and auto-generates a standardized, ready-to-import ACE application — leaving the
developer as the reviewer.

Built in **Java** so it runs inside the approved ACE Toolkit / JVM (no Python,
no `cmd`, no `.bat` — runs where corporate group policy allows `java`).

## 🆕 NEW: Field Mapping Feature

FlowSmith now supports **automatic ESQL generation from mapping documents**!
Provide a CSV file with XML-to-JSON field mappings, and FlowSmith will
automatically inject the transformation code into your generated ESQL files.
No external libraries required (pure JDK - no Apache POI).

**Quick Example:**
```bash
java -jar flowsmith.jar generate \
  --subsys XAJ --app TLMTF --func FINANCING \
  --mapping my-mappings.csv
```

See [MAPPING_FEATURE.md](MAPPING_FEATURE.md) for complete documentation.

---

## How the AI is integrated (read this before the demo)

The architecture is **already agentic** — `perceive → reason → act → human review`:

```
   plain-English requirement
            │
   ┌────────▼─────────┐   PERCEIVE  learns org patterns/standards (patterns.txt)
   │   Catalog        │
   └────────┬─────────┘
   ┌────────▼─────────┐   REASON    picks the pattern   ◀── AI INTEGRATION POINT
   │   Recommender    │             (Recommender interface)
   │   (pluggable)    │
   └────────┬─────────┘
   ┌────────▼─────────┐   ACT       generates standardized ACE app + env configs
   │   Generator      │
   └────────┬─────────┘
            ▼
   developer reviews → BAR → deploy   HUMAN-IN-THE-LOOP
```

**The reasoning engine is a swappable component — and the LLM is now wired in.**

Two implementations of `Recommender` ship today:

- **`WatsonxRecommender`** — calls **IBM watsonx.ai** (a **Granite** model) to
  interpret the requirement and choose the pattern. Live REST integration
  (IAM token + text generation), dependency-free.
  See [`src/com/flowsmith/WatsonxRecommender.java`](src/com/flowsmith/WatsonxRecommender.java).
- **`KeywordRecommender`** — a deterministic rule-based engine, used when
  watsonx is not configured **and** as an automatic safety-net if a live call
  fails (so a demo never breaks).

The agent selects watsonx automatically when credentials are present; force it
with `--engine watsonx` or `--engine keyword`. Either way the rest of the agent
(perceive / act / review) is identical — the model sits behind one interface.

> Framing for judges: *"The agent is AI-integrated: the reasoning step calls IBM
> watsonx.ai (Granite) over REST. If watsonx is unconfigured or unreachable it
> falls back to a rule-based engine, so the demo is robust either way."*

### Turn on watsonx.ai
1. Copy `watsonx.properties.example` → `watsonx.properties` (git-ignored) and fill
   in `url`, `apikey`, `projectId`, `modelId` — or set `WATSONX_URL`,
   `WATSONX_APIKEY`, `WATSONX_PROJECT_ID`, `WATSONX_MODEL_ID` as environment vars.
2. Run any `recommend` / `generate --requirement` command. The `[AI]` trace will
   show `IBM watsonx.ai (...)` and the model's chosen pattern.

Credentials are **never committed** (`watsonx.properties` is git-ignored).

---

## 60-second demo script

1. **Show the learned knowledge base**
   ```
   java -jar flowsmith.jar list
   ```
   → "the agent has learned 4 reusable patterns from the org standards."

2. **Show the agent reasoning over a plain-English need**
   ```
   java -jar flowsmith.jar recommend "consume grouped messages from a queue at end of batch and write files"
   ```
   → it scores patterns and recommends `sub_file_pubbatch`, with the rationale.

3. **Let the agent generate the integration end-to-end**
   ```
   java -jar flowsmith.jar generate --requirement "publish a file onto an MQ queue for downstream systems" --subsys XAJ --app TLMTF --func FINANCING
   ```
   → AI selects `pub_file`, applies the org naming convention, injects the ED6
     enterprise framework references, generates DEV/ACC/PRO configs, and prints
     the artifacts for developer review.

4. **Point at the seam:** open `Recommender.java`, show the `AI INTEGRATION POINT`
   banner — "swap this for watsonx.ai and the recommendation becomes a real LLM."

---

## Run it from the ACE Toolkit (no Python / cmd / bat)

Use **`FlowSmith Generate (java).launch`** (at the repo root):

1. **Run → External Tools → External Tools Configurations… → FlowSmith Generate (java) → Run**
2. Type a plain-English requirement, then SUBSYS / APPNM / FUNCNM (NDM optional).
3. The Console shows the AI trace; generated projects land in
   `C:\Users\jf49313\git\FlowSmith_Generated` → *File → Import → Existing Projects*.

The launch's `ATTR_LOCATION` is pre-wired to this machine's Java 17 (bundled with ACE):
`C:\Program Files\IBM\ACE\13.0.5.0\common\java17\bin\java.exe`
(Eclipse External Tools needs the absolute `java.exe` path — it does not search
PATH). On a different machine, find the path via *Window → Preferences → Java →
Installed JREs* (`<JRE home>\bin\java.exe`) and update that field.

---

## Build (only if you change the source)

The repo ships a prebuilt **`flowsmith.jar`** (Java 17 bytecode — runs on ACE 11/12
JVMs), so you normally don't need to compile. To rebuild:

```
javac -d bin src/com/flowsmith/*.java
jar cfm flowsmith.jar manifest.mf -C bin .
```

Or just drop `src/` into a Java project in the Toolkit (it's Eclipse) and let it
compile.

---

## Layout

```
flowsmith-java/
    patterns.txt          # org knowledge base (the "map" the agent learns)
    flowsmith.jar         # prebuilt, portable (Java 17)
    manifest.mf
    example-mapping.csv   # example field mapping document
    MAPPING_FEATURE.md    # complete mapping feature documentation
    DEPENDENCIES.md       # build instructions (JDK only, no dependencies)
    src/com/flowsmith/
        FlowSmith.java              # main + AI reasoning trace + CLI
        Catalog.java                # PERCEIVE - loads patterns.txt
        Pattern.java                # data
        Recommender.java            # REASON - the AI integration point (interface)
        KeywordRecommender.java     # current rule-based stand-in for the LLM
        WatsonxRecommender.java     # IBM watsonx.ai integration
        Generator.java              # ACT - template copy + token substitution
        MappingDocument.java        # NEW - parses CSV mapping files (pure Java)
        ESQLMappingGenerator.java   # NEW - generates ESQL from mappings
```

The Python version (`../flowsmith/`) implements the same idea; this Java build is
the one wired for the ACE Toolkit / IBM demo.

## Dependencies

None. FlowSmith builds and runs with just the **JDK (Java 17+)** - mapping
documents are read as CSV by a pure-Java parser (no Apache POI). See
[DEPENDENCIES.md](DEPENDENCIES.md) for build instructions.
