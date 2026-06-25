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

## Two ways to run FlowSmith

1. **Command line** (this guide, Steps 0–6) — full control of every option.
2. **One click inside the ACE Toolkit** — an *External Tools* button that prompts
   for the inputs and generates into your workspace. See
   [`TOOLKIT_BUTTON.md`](TOOLKIT_BUTTON.md), summarised under
   *"Run inside the ACE Toolkit"* below.

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

## Run inside the ACE Toolkit (one click) — Option B

Instead of the command line, you can run FlowSmith from a Toolkit **External
Tools** button (the same mechanism as the original `CreatePatternAppl.pl.launch`).

1. Import this project into the Toolkit so the launch configs are in the workspace.
2. **Run → External Tools → External Tools Configurations…** → under **Program**
   pick **FlowSmith Generate (mac-linux)** or **(windows)** → **Run**.
3. Answer the prompt dialogs (pattern id, SUBSYS, APPNM, FUNCNM, NDM optional).
4. The Console shows the log; projects generate into the **workspace root** and
   the workspace auto-refreshes.
5. Register them once via *File → Import → Existing Projects into Workspace*.

Full setup, customisation, and troubleshooting for this path are in
[`TOOLKIT_BUTTON.md`](TOOLKIT_BUTTON.md).

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
| Toolkit button does nothing / path error  | Check Python is on PATH; fix the project-name paths in the `.launch` files (see `TOOLKIT_BUTTON.md`). |
| Windows: "filename, directory name or volume syntax is incorrect" | UNC/network workspace or wrong project name in the `.launch`. See `TOOLKIT_BUTTON.md` → Troubleshooting; or run `flowsmith\tools\flowsmith-interactive.bat` directly. |

---

## Command reference

```bash
python3 flowsmith.py list
python3 flowsmith.py recommend "<plain-english requirement>"
python3 flowsmith.py generate --pattern <id> --subsys <X> --app <Y> --func <Z> [--ndm <N>] [--out <dir>] [--force]
python3 flowsmith.py generate --requirements <file.json> [--out <dir>] [--force]
```

See `README.md` for the architecture and how this MVP maps to the full
ACE FlowSmith AI vision, and `TOOLKIT_BUTTON.md` for running it from inside the
ACE Toolkit.
