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
   - NDM — optional, leave blank to skip
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

## How this maps to the bigger picture

This is the lightest way to put FlowSmith "in the Toolkit". It runs the same
`flowsmith.py generate` you use on the command line. The AI `recommend` step and
a richer native UI would come with a true Eclipse plugin (Option C) — but this
button gives you an integrated generate experience today with near-zero overhead.
