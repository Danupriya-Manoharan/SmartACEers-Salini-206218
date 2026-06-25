# How to Use the ACE FlowSmith MVP — Step by Step

This guide walks you from zero to a generated, ready-to-import IBM ACE
application. No prior setup beyond **Python 3** (already on macOS/Linux; on
Windows install from python.org).

> TL;DR
> ```bash
> cd SmartACEers-Salini-206218/flowsmith
> python3 flowsmith.py list
> python3 flowsmith.py recommend "consume messages from a queue and write files"
> python3 flowsmith.py generate --pattern ptp_file --subsys XAJ --app TLMTF --func FINANCING
> ```
> Output appears in `SmartACEers-Salini-206218/Generated/`.

> **Java build for the IBM/ACE-Toolkit demo:** a Java implementation (no Python /
> `cmd` / `.bat`, runs in the approved JVM) lives in [`../flowsmith-java/`](../flowsmith-java/README.md),
> with the AI framing wired in. Use that on locked-down Toolkit machines.

## Two ways to run FlowSmith

1. **Command line** (this Python guide, Steps 0–6) — full control of every option.
2. **Inside the ACE Toolkit** — use the **Java build**
   ([`../flowsmith-java/`](../flowsmith-java/README.md)), which runs via an
   *External Tools* `java` button in the approved JVM (no Python / `cmd` / `.bat`).
   That is the path for the IBM/ACE-Toolkit demo.

---

## Step 0 — Prerequisites

- **Python 3** — check with `python3 --version` (any 3.x works; no pip installs needed).
- **IBM ACE Toolkit** — only needed at the end, to import and build the result.
- The **`Existing_Templates/`** folder must be present next to `flowsmith/`
  (it ships with this repo). FlowSmith reads its templates from there.

Open a terminal and move into the tool folder:

```bash
cd SmartACEers-Salini-206218/flowsmith
```

---

## Step 1 — See the available patterns (the "map")

```bash
python3 flowsmith.py list
```

You'll see the 4 reusable patterns and the tokens that get substituted:

| Pattern id           | Type | Use it for…                       |
|----------------------|------|-----------------------------------|
| `ptp_file`           | PTP  | File → file point-to-point        |
| `pub_file`           | PUB  | File → MQ publish                 |
| `sub_file_pubonline` | SUB  | Queue → file, online publishing   |
| `sub_file_pubbatch`  | SUB  | Queue → file, batch publishing    |

---

## Step 2 — (Optional) Let FlowSmith recommend a pattern

If you're not sure which pattern fits, describe the requirement in plain English:

```bash
python3 flowsmith.py recommend "publish a file onto an MQ queue"
```

It prints a ranked list and a top pick, plus the exact `generate` command to run.
Skip this step if you already know the pattern id.

---

## Step 3 — Decide your token values

Every generated app needs these identifiers (your org's naming convention):

| Flag       | Token    | Meaning                         | Example     |
|------------|----------|---------------------------------|-------------|
| `--subsys` | `SUBSYS` | Subsystem code (auto-uppercased)| `XAJ`       |
| `--app`    | `APPNM`  | Application code                | `TLMTF`     |
| `--func`   | `FUNCNM` | Functionality name              | `FINANCING` |
| `--ndm`    | `NDMNM`  | NDM dataset (PUB/SUB only)      | `REFPGN`    |

`--subsys`, `--app`, `--func` are required. `--ndm` is optional.

---

## Step 4 — Generate the application

### Option A — straight from the command line

```bash
python3 flowsmith.py generate \
    --pattern ptp_file \
    --subsys XAJ --app TLMTF --func FINANCING
```

### Option B — from a requirements file

Edit `requirements.sample.json` (or copy it), then:

```bash
python3 flowsmith.py generate --requirements requirements.sample.json
```

In a requirements file you can either set `"pattern"` explicitly, or leave it
`null` and provide a `"requirement"` sentence — FlowSmith will auto-select the
pattern for you.

**Useful flags**
- `--out <dir>` — write somewhere other than the default `../Generated`.
- `--force` — overwrite an existing output project of the same name.

---

## Step 5 — Read the generation summary

FlowSmith prints what it did, e.g.:

```
=== ACE FlowSmith :: generation complete ===
Pattern        : ptp_file  (PTP File-to-File)
Tokens applied : SUBSYS=XAJ, APPNM=TLMTF, FUNCNM=FINANCING
Output project : .../Generated/XAJ_PTP_TLMTF_FINANCING_FIL
Substitutions  : 44 token hits across 3 files; 6 paths renamed
```

Tokens are replaced in **folder names, file names, and file contents**. The
result lands in:

```
Generated/XAJ_PTP_TLMTF_FINANCING_FIL/
    XAJ_PTP_TLMTF_FINANCING_FIL/          ← the ACE application project
    XAJ_PTP_TLMTF_FINANCING_FIL_Configs/  ← BAR/config project (DEV/ACC/PRO)
```

---

## Step 6 — Import into ACE Toolkit (developer as reviewer)

1. Open **IBM ACE Toolkit**.
2. **File → Import → IBM App Connect Enterprise → Existing project** (or
   *General → Existing Projects into Workspace*).
3. Select the generated folder under `Generated/` and import **both** the
   application project and its `_Configs` project.
4. **Review** the `.msgflow` and `.esql`, and fine-tune the business logic
   (the compute/map nodes are skeletons by design).
5. Validate the environment configs under
   `*_Configs/batchconfig/{DEV,ACC,PRO}`.
6. Build the **BAR** file and deploy.

> **Important — shared framework dependency.** The generated app references your
> ED6 shared libraries (`ED6_BatchFramework_Shared`, `ED6_CommonFunctions_Shared`,
> `ED6_MessageLoggingTracking_Shared`) and connector subflows. These must already
> be in the same ACE workspace, or the project will import but show
> unresolved-reference build errors until they're added. (This is the same
> requirement as the original template/Perl process — not specific to FlowSmith.)

---

## Run inside the ACE Toolkit

Use the **Java build** — it runs via an *External Tools* `java` button in the
Toolkit's approved JVM (no Python, `cmd`, or `.bat`). Setup, the one-click
launch, and the demo script are in
[`../flowsmith-java/README.md`](../flowsmith-java/README.md).

This Python guide is for command-line use and for understanding the generator;
the Java build is the one wired for the IBM/ACE-Toolkit demo.

---

## Quick troubleshooting

| Symptom                                   | Fix                                                          |
|-------------------------------------------|-------------------------------------------------------------|
| `command not found: python3`              | Install Python 3, or try `python` instead of `python3`.     |
| `ERROR: no pattern chosen`                | Add `--pattern <id>` or run `recommend` first.              |
| `ERROR: pattern '…' requires: …`          | Provide the missing `--subsys` / `--app` / `--func`.        |
| `ERROR: … already exists`                 | Add `--force`, or pick a different `--out` directory.       |
| `ERROR: template not found`               | Run from inside `flowsmith/`; keep `Existing_Templates/` intact. |
| Build errors after import in Toolkit      | Add the ED6 shared projects to the workspace (see Step 6).  |

---

## Command reference

```bash
python3 flowsmith.py list
python3 flowsmith.py recommend "<plain-english requirement>"
python3 flowsmith.py generate --pattern <id> --subsys <X> --app <Y> --func <Z> [--ndm <N>] [--out <dir>] [--force]
python3 flowsmith.py generate --requirements <file.json> [--out <dir>] [--force]
```

See `README.md` for the architecture and how this MVP maps to the full
ACE FlowSmith AI vision, and [`../flowsmith-java/README.md`](../flowsmith-java/README.md)
for running it from inside the ACE Toolkit.
