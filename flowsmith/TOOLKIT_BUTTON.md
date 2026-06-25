# Run FlowSmith from inside the ACE Toolkit (Option B)

This turns FlowSmith into a **one-click "External Tools" button** in the IBM ACE
Toolkit — no plugin install, same mechanism your original `CreatePatternAppl.pl.launch`
used. Click it, answer a few prompts, and the generated ACE projects appear in
your workspace folder.

## What's included

```
SmartACEers-Salini-206218/
    FlowSmith Generate (mac-linux).launch   <- External Tools config (macOS/Linux)
    FlowSmith Generate (windows).launch     <- External Tools config (Windows)
    flowsmith/tools/
        flowsmith-run.sh                     <- launcher invoked by the .launch (sh)
        flowsmith-run.bat                    <- launcher invoked by the .launch (cmd)
```

The `.launch` files prompt for **pattern id, SUBSYS, APPNM, FUNCNM, NDM (optional)**,
then run the generator with output going to your **workspace root**, and finally
**auto-refresh the workspace** so the new projects show up.

## One-time setup

1. **Import this project into the Toolkit** so the launch + scripts live in the
   workspace: *File → Import → Existing Projects into Workspace* → select
   `SmartACEers-Salini-206218`. (The launch configs reference that project name;
   if your project has a different name, edit the two `.launch` files and replace
   `SmartACEers-Salini-206218` accordingly.)
2. **Ensure Python 3 is on PATH** (`python3 --version`). The Toolkit/Eclipse uses
   your system PATH; on Windows the launcher calls `python`.
3. **(macOS/Linux) make the launcher executable** once:
   `chmod +x flowsmith/tools/flowsmith-run.sh` (already set in this repo).

## Run it

1. **Run → External Tools → External Tools Configurations…**
2. Under **Program**, pick **FlowSmith Generate (mac-linux)** or
   **(windows)** to match your OS. (If it isn't listed, the shared `.launch`
   files are at the project root — they appear once the project is imported; you
   can also just double-click the matching `.launch` once.)
3. Click **Run**. Answer the prompt dialogs:
   - Pattern id — e.g. `ptp_file`
   - SUBSYS — e.g. `XAJ`
   - APPNM — e.g. `TLMTF`
   - FUNCNM — e.g. `FINANCING`
   - NDM — optional; leave the default `NONE` if not used
4. The Console shows the generation log. The new projects land under the
   **workspace root** (e.g. `XAJ_PTP_TLMTF_FINANCING_FIL/`).

## Make the generated projects appear as ACE projects

The launch **refreshes** the workspace, but Eclipse doesn't auto-add new folders
as projects. After the run:

- *File → Import → Existing Projects into Workspace* → **root directory = your
  workspace folder** → tick the generated application project **and** its
  `_Configs` project → **Finish**.

(They import in seconds and only need doing once per generated app.)

## Tips / customisation

- **Change defaults:** edit the `${string_prompt:...:DEFAULT}` text in the
  `.launch` files to set your own default values.
- **Output elsewhere:** the launch passes `${workspace_loc}` as the output dir.
  To send output to a fixed folder instead, edit the last argument in the
  `.launch` `ATTR_TOOL_ARGUMENTS`.
- **Different project name:** if you rename the containing project, update the
  `${workspace_loc:/SmartACEers-Salini-206218/...}` paths in both `.launch` files.
- **Reminder — shared framework:** generated apps still reference the ED6 shared
  libraries; have `ED6_BatchFramework_Shared`, `ED6_CommonFunctions_Shared`,
  `ED6_MessageLoggingTracking_Shared` in the workspace to build cleanly.

## Locked-down machines ("This program is blocked by group policy")

Corporate AppLocker / Software Restriction Policy often blocks `cmd.exe` and
`.bat`/`.cmd` scripts (especially from `C:\Users`). If the windows button fails
with **"This program is blocked by group policy"**, the *blocked program* is
`cmd`/the `.bat` — not FlowSmith.

Use **`FlowSmith Generate (python-direct).launch`** instead: it launches
`python` directly with no shell, so there is no `cmd`/`.bat` to block.

This launch is pre-wired to the absolute checkout path
`C:\Users\jf49313\git\SmartACEers-Salini-206218` and writes generated projects to
`C:\Users\jf49313\git\FlowSmith_Generated` (a sibling folder, easy to import).
If your checkout moves, update the `ATTR_WORKING_DIRECTORY` and the `--out` path
in that `.launch`.

- It requires **`python` to be on PATH** (and Python 3 installed). If Eclipse
  can't find it, edit the launch's `ATTR_LOCATION` (currently `python`) to the
  full path — find it with `where python` in an allowed shell.
- If **Python itself** is also blocked by policy (e.g. a per-user install under
  `C:\Users\...\AppData`), no external launcher can work. Then either:
  1. Run `python flowsmith.py generate ...` in any shell/VM where it's allowed
     (or on another machine) and **import the generated projects** — importing
     is not blocked; or
  2. Move to an **in-Toolkit** approach that runs inside the already-approved
     Toolkit JVM and isn't subject to program-execution policy: a **native IBM
     ACE Pattern**, or an **Eclipse plugin**. (See `README.md` → vision.)

## Troubleshooting

**"The filename, directory name, or volume syntax is incorrect"** (Windows)
This is a `cmd.exe` error. Common causes, in order:

1. **Nested quoting in the `.launch`** (fixed). Older versions used
   `/C ""..." "..."" ` which Eclipse's argument parser mangled. The current
   config uses single-level quoting and is the fix for the local-drive case —
   make sure you have the latest `.launch` files.
2. **Wrong project name in the `.launch` path.** The configs reference
   `${workspace_loc:/SmartACEers-Salini-206218/...}`. If your project is imported
   under a different name, that variable resolves to an invalid path. Open both
   `.launch` files and replace `SmartACEers-Salini-206218` with your actual
   project name (Project Explorer shows it).
3. **Spaces in the path** (e.g. `C:\Users\First Last\...`). If your Windows
   user/profile folder contains a space, run `flowsmith-interactive.bat` instead
   (below), which avoids the Eclipse/`cmd` quoting entirely.
4. **Workspace on a UNC / network path** (`\\server\share\...`). The launcher
   uses `pushd`, which maps a temporary drive and handles UNC.

**Quickest way to isolate the problem:** run
`flowsmith\tools\flowsmith-interactive.bat` directly (double-click or from a
Command Prompt). It prompts for inputs and uses no Eclipse variables, so:
- if it works → the issue is the `.launch` path/variable resolution (cause 2);
- if it also fails → it's Python/PATH or a path issue the message will name.

**"python is not recognized"** — install Python 3 and ensure it's on PATH, or
edit the `set "PY=python"` line in the launcher to a full path.

## How this maps to the bigger picture

This is the lightest way to put FlowSmith "in the Toolkit". It runs the same
`flowsmith.py generate` you use on the command line. The AI `recommend` step and
a richer native UI would come with a true Eclipse plugin (Option C) — but this
button gives you an integrated generate experience today with near-zero overhead.
