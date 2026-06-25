# ACE FlowSmith AI — Java MVP (IBM competition build)

An **AI-ready agent** that accelerates IBM App Connect Enterprise (ACE)
development. It learns an organisation's reusable integration patterns and
standards, interprets a plain-English requirement, recommends the right pattern,
and auto-generates a standardized, ready-to-import ACE application — leaving the
developer as the reviewer.

Built in **Java** so it runs inside the approved ACE Toolkit / JVM (no Python,
no `cmd`, no `.bat` — runs where corporate group policy allows `java`).

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

**The reasoning engine is the only swappable part.**

- **Today:** `KeywordRecommender` — a deterministic intent matcher. A faithful
  *stand-in* for the model, at the exact spot the model will sit. No model call,
  fully explainable.
- **Later:** implement `Recommender` with **IBM watsonx.ai (Granite)** (or Claude)
  to interpret requirements and even compose new subflows. **One class changes —
  nothing else.** See the big `AI INTEGRATION POINT` banner in
  [`src/com/flowsmith/Recommender.java`](src/com/flowsmith/Recommender.java).

> Honest framing for judges: *"This is a working AI-ready agent. The agent loop
> is complete; the reasoning step currently uses a rule-based stand-in and is
> designed to be replaced by an LLM (watsonx.ai / Granite) behind one interface —
> no rearchitecting."* (It does **not** call a model yet — and says so.)

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

Requires `java` on PATH (it is, per your environment). If Eclipse can't find it,
set the launch's `ATTR_LOCATION` to the full `java.exe` path.

---

## Build (only if you change the source)

The repo ships a prebuilt **`flowsmith.jar`** (Java 8 bytecode — runs on ACE 11/12
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
    flowsmith.jar         # prebuilt, portable (Java 8)
    manifest.mf
    src/com/flowsmith/
        FlowSmith.java        # main + AI reasoning trace + CLI
        Catalog.java          # PERCEIVE - loads patterns.txt
        Pattern.java          # data
        Recommender.java      # REASON - the AI integration point (interface)
        KeywordRecommender.java   # current rule-based stand-in for the LLM
        Generator.java        # ACT - template copy + token substitution
```

The Python version (`../flowsmith/`) implements the same idea; this Java build is
the one wired for the ACE Toolkit / IBM demo.
