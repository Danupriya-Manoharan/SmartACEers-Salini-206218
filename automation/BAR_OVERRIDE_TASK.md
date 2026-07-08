# Task: Add BAR override step to ACEDeployer.java (re-point file dirs for local testing)

## Goal

Before the BAR is deployed, apply a **BAR override** that re-points the message
flow's **FileInput input directory** and **FileOutput directory** to a local
test location:

```
C:\temp\test\<app>\in      (FileInput polls here)
C:\temp\test\<app>\out     (FileOutput writes here)
```

This lets us drop a test XML into the `in` folder and read the transformed JSON
from the `out` folder, without editing the `.msgflow` or rebuilding the BAR.

`<app>` = the application/project identifier (use `projectName`, or a value
derived from it — see "Open choice" below).

## Where this lives

File: `automation/src/com/flowsmith/automation/ACEDeployer.java`

Relevant existing code:

- Fields: `projectName`, `barFile`, `integrationNode`, `integrationServer`,
  and the static `ACE_TOOLKIT_PATH`.
- `deployBarFile()` (around line 236) currently runs, after sourcing the ACE
  profile:
  ```
  "<ACE>\server\bin\mqsideploy" <node> -e <server> -a "<barFile>"
  ```
- Helpers already available in the class:
  - `int executeCommand(String command)` — runs a command, returns exit code.
  - `String executeCommandWithOutput(String command)` — runs a command, returns stdout.
- Every ACE command is run inside a shell that first sources the profile:
  ```
  call "<ACE>\server\bin\mqsiprofile.cmd" && <actual command>
  ```

## What to implement

Add a new method `applyBarOverrides()` and call it **immediately before**
`deployBarFile()` in the deploy sequence (`deploy()` method). It must:

1. Build the two override key/value pairs (see "Override command" below), using
   `C:\temp\test\<app>\in` and `C:\temp\test\<app>\out`.
2. Run the override command (profile-sourced, like the other steps), producing a
   new BAR file, e.g. `<barFile without .bar>-test.bar`.
3. Update the `barFile` field to point at the new `-test.bar`, so the existing
   `deployBarFile()` deploys the overridden BAR (no change needed inside
   `deployBarFile()`).
4. Print clear progress lines consistent with the existing style
   (`[Step x/y] ...`, `SUCCESS: ...`), and throw an `Exception` on non-zero exit
   code (same pattern as `deployBarFile()`).

Keep the coding style identical to the existing class: `String.format(...)`,
`call "...\mqsiprofile.cmd" && ...`, `executeCommand(...)`, `throw new Exception(...)`.

## Override command (ACE 13 — `ibmint`)

`ibmint apply overrides` takes a **file** of overrides, then writes a new BAR:

```
ibmint apply overrides "<overridesFile>" --input-bar-file "<inBar>" --output-bar-file "<outBar>"
```

Steps inside `applyBarOverrides()`:

1. Write a temp overrides properties file (e.g. `%TEMP%\flowsmith-overrides.properties`
   or next to the BAR) containing:
   ```
   <FLOW>#<FILEIN_NODE>.inputDirectory=C:\temp\test\<app>\in
   <FLOW>#<FILEOUT_NODE>.directory=C:\temp\test\<app>\out
   ```
2. Run, profile-sourced:
   ```
   call "<ACE>\server\bin\mqsiprofile.cmd" && ibmint apply overrides "<overridesFile>" --input-bar-file "<barFile>" --output-bar-file "<barFile-test>"
   ```

### Legacy alternative (also valid on ACE 13, supports inline — no file needed)

```
call "<ACE>\server\bin\mqsiprofile.cmd" && "<ACE>\server\bin\mqsiapplybaroverride" -b "<barFile>" -o "<barFile-test>" -m "<FLOW>#<FILEIN_NODE>.inputDirectory=C:\temp\test\<app>\in,<FLOW>#<FILEOUT_NODE>.directory=C:\temp\test\<app>\out"
```

Either approach is acceptable — the inline `mqsiapplybaroverride -m` form is
simpler because it needs no temp file. Prefer it unless `ibmint` is required.

## Override KEY format (critical)

```
<messageFlowName>#<nodeLabel>.<propertyName>=<value>
```

- `messageFlowName` — the flow file name without extension, e.g. `Adapter`
- `nodeLabel` — the node's display label in the flow
  (FileInput label in the ptp template is `FILEIN`)
- `propertyName` — `inputDirectory` for a FileInput node;
  `directory` for a FileOutput node

**Do not hardcode the node labels/properties blindly.** The exact keys must match
the BAR. Discover them by running:

```
call "<ACE>\server\bin\mqsiprofile.cmd" && "<ACE>\server\bin\mqsireadbar" -b "<barFile>" -r
```

This prints every overridable `flow#node.property` key in the BAR. Use those exact
strings. (Optionally, `applyBarOverrides()` can run `mqsireadbar -r` first and log
the available keys to help diagnose mismatches.)

## Open choice (please implement one, keep it configurable)

- `<app>` value: default to `projectName`. Optionally accept an extra
  constructor arg / command-line arg / system property `-Dtest.app=<name>` to
  override it, defaulting to `projectName` when absent.
- Test base folder: default `C:\temp\test`, overridable via
  `-Dtest.base=<dir>` (default when absent).
- Node labels: default `FILEIN` / `FILEOUT` for the property keys, overridable
  via `-Dtest.filein=<label>` / `-Dtest.fileout=<label>` so it works for flows
  with different node names.

## Acceptance criteria

1. Running the deployer produces a `<projectName>-test.bar` whose FileInput input
   directory and FileOutput directory are `C:\temp\test\<app>\in` and `...\out`.
2. The overridden BAR is the one deployed (verify with `mqsireadbar -r` on the
   output BAR, or by checking the flow polls `C:\temp\test\<app>\in` after deploy).
3. A non-zero exit from the override command aborts deployment with a clear error.
4. Existing behaviour is unchanged when overrides are not applicable
   (e.g. guard so a flow without FileInput/FileOutput still deploys).
5. Coding style matches the existing `ACEDeployer.java` (profile sourcing,
   `executeCommand`, `String.format`, exception handling, progress prints).

## Notes / caveats

- The FileInput **message domain must be XMLNSC** for the mapping ESQL
  (`InputRoot.XMLNSC.*`) to populate — this is a flow design point, not part of
  this task, but the test won't produce mapped output otherwise.
- FileInput auto-creates `mqsiarchive` / `mqsibackout` subfolders under `in`.
  The `in` folder itself must exist and be writable by the server user.
- Use backslash Windows paths in the override values (`C:\temp\test\...`).
