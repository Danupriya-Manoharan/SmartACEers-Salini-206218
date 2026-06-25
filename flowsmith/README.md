# ACE FlowSmith — MVP

A minimal, working slice of **ACE FlowSmith AI**: an agent that accelerates IBM
App Connect Enterprise (ACE) development by *discovering* the right reusable
integration pattern for a requirement and *auto-generating* a standardized,
ready-to-import ACE application from your organisation's existing templates.

The developer stays the **reviewer** — FlowSmith generates, the human validates
and fine-tunes before deployment.

This MVP is the cross-platform, offline successor to `CreatePatternAppl.pl`:
no Perl, no Windows, no internal git clone required.

---

## The "map" — pattern catalog

FlowSmith's discovery layer is `catalog.json`: a registry of the reusable
templates under `Existing_Templates/`. This is the "small map" that turns
*manual discovery* into *automated selection*.

```
 Functional requirement (plain English)
              │
              ▼
   ┌──────────────────────┐
   │  recommend (the map) │   keyword / intent match → ranked patterns
   └──────────┬───────────┘     (LLM plugs in here in the full product)
              ▼
   ┌──────────────────────┐
   │   generate           │   copy template → substitute org tokens
   │                      │   (folders + filenames + file contents)
   └──────────┬───────────┘
              ▼
   Ready-to-import ACE app  ───►  Developer reviews ──► BAR build ──► deploy
   + env configs (DEV/ACC/PRO)
```

| Pattern id           | Type | Connectivity | What it does                                   |
|----------------------|------|--------------|------------------------------------------------|
| `ptp_file`           | PTP  | FILE         | Point-to-point file → file batch flow          |
| `pub_file`           | PUB  | FILE         | Publisher: file → MQ publish (+ embedded MQ)   |
| `sub_file_pubonline` | SUB  | FILE         | Subscriber tuned for **online** publishing     |
| `sub_file_pubbatch`  | SUB  | FILE         | Subscriber tuned for **batch** publishing      |

**Organisation tokens** substituted on generation:

| Token    | Meaning                                  | Example     |
|----------|------------------------------------------|-------------|
| `SUBSYS` | Subsystem code (uppercased)              | `XAJ`       |
| `APPNM`  | Application code                         | `TLMTF`     |
| `FUNCNM` | Functionality name                      | `FINANCING` |
| `NDMNM`  | NDM dataset name (some PUB/SUB patterns) | `REFPGN`    |

---

## Usage

Requires only Python 3 (standard library — no installs).

```bash
cd flowsmith

# 1. Show the pattern map
python3 flowsmith.py list

# 2. Recommend a pattern from a plain-English requirement
python3 flowsmith.py recommend "consume messages from a queue and write files"

# 3a. Generate from explicit inputs
python3 flowsmith.py generate \
    --pattern ptp_file --subsys XAJ --app TLMTF --func FINANCING

# 3b. Generate from a requirements file (pattern can auto-select from text)
python3 flowsmith.py generate --requirements requirements.sample.json
```

Output lands in `../Generated/<APP_PROJECT>/`, containing the ACE **application
project** and its **`_Configs` project** (env-specific `batchconfig/{DEV,ACC,PRO}`
properties, embedded MQ definitions). Import it into ACE Toolkit via
*File → Import → Existing project*.

---

## What it generates (verified)

For `--pattern ptp_file --subsys XAJ --app TLMTF --func FINANCING`:

```
XAJ_PTP_TLMTF_FINANCING_FIL/
    XAJ_PTP_TLMTF_FINANCING_FIL/
        .project                              ← project name tokenized
        application.descriptor
        PTP/XAJ/TLMTF/FINANCING/FIL/
            Adapter.msgflow                   ← node config + esql routine ref tokenized
            Adapter_Compute.esql              ← BROKER SCHEMA PTP.XAJ.TLMTF.FINANCING.FIL
    XAJ_PTP_TLMTF_FINANCING_FIL_Configs/
        .project
        batchconfig/PTP_XAJ_TLMTF_FINANCING..prop
```

Tokens are replaced in **folder names, file names, and file contents**, while
shared-framework references (`ED6_BatchFramework_Shared`, ACE builders, etc.)
are left untouched. A post-run check confirms **zero placeholder tokens remain**.

---

## How this maps to the full FlowSmith vision

| FlowSmith capability (opportunity note)          | MVP today                                   | Path to full product                          |
|--------------------------------------------------|---------------------------------------------|-----------------------------------------------|
| Learns org standards / reusable subflows         | `catalog.json` encodes patterns + intent    | Auto-index template repos; learn naming rules |
| Auto-generates flows from requirements           | `generate` instantiates templates           | LLM composes/edits flows beyond templates     |
| Discovery & reuse of subflows                    | `recommend` keyword matcher                  | Swap scorer for an LLM classifier (same API)  |
| Runtime artifacts (configs, policies)            | `_Configs` project generated per env        | Generate BAR + server.conf + policies         |
| Developer as reviewer                            | Prints summary + tree for review            | Diff view / approval workflow in Toolkit      |

The `recommend` command is the deliberate **seam for AI**: its contract is
*requirement text → ranked pattern ids*. Replacing the offline keyword scorer
with a Claude classification call upgrades intelligence without changing the
generator or the catalog.

---

## Files

```
flowsmith/
    catalog.json               # the "map": reusable pattern registry
    flowsmith.py               # list / recommend / generate CLI (stdlib only)
    requirements.sample.json   # example requirement input
    README.md                  # this file
```
