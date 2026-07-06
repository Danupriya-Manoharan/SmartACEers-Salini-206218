# Setup — FlowSmith mode & skill in the ACE Toolkit

> Requires IBM Bob installed with the ACE Toolkit (Bob Shell available from the Toolkit
> toolbar) and IBM ACE. Bob and its `ace-bob` foundational skill are IBM products —
> install them per IBM's instructions first.

## 1. Install IBM's foundational ACE skill

In your ACE Toolkit **workspace**, create the `.bob/` folder if it does not exist, then add
IBM's `ace-bob` skill (this is the authoritative ACE skill Bob's ACE support builds on):

```bash
# from your ACE Toolkit workspace root
mkdir -p .bob/skills
git clone https://github.com/ot4i/ace-bob .bob/skills/ace-bob
```

## 2. Add the FlowSmith mode + skill

Copy the contents of this folder's `.bob/` into your workspace `.bob/` (merge, don't
overwrite the `ace-bob` skill from step 1):

```bash
# from this repo root
cp    bob-integration/.bob/custom_modes.yaml   <workspace>/.bob/custom_modes.yaml
cp -R bob-integration/.bob/rules-flowsmith      <workspace>/.bob/rules-flowsmith
cp -R bob-integration/.bob/skills/flowsmith-patterns \
                                                <workspace>/.bob/skills/flowsmith-patterns
```

Make the reference templates reachable so Bob can copy them — either work inside this repo
as the workspace, or copy `Existing_Templates/` into the workspace.

## 3. Launch Bob and select the mode

1. In the ACE Toolkit toolbar, open the **Bob Shell Terminal**.
2. Select the **FlowSmith ACE Developer** mode.
3. Give a requirement in plain English plus the tokens, e.g.:

   > *Consume grouped messages from a queue at end of batch and write files.*
   > SUBSYS=XAJ, APPNM=TLMTF, FUNCNM=FINANCING

Bob will match the pattern (here: `sub_file_pubbatch`), generate
`XAJ_SUB_TLMTF_FINANCING_FIL` + `_Configs`, and present it for your review.

## 4. Review, build, deploy

After you approve, Bob builds and deploys from the shell. Typical ACE commands Bob issues:

```bash
# Build a BAR from the generated application (ACE v12+/v13)
ibmint package --input-path <workspace> \
               --output-bar-file XAJ_SUB_TLMTF_FINANCING_FIL.bar \
               --project XAJ_SUB_TLMTF_FINANCING_FIL

# Ensure a local Integration Server / Node is running, then deploy
mqsideploy <IntegrationNode> -e <IntegrationServer> \
           -a XAJ_SUB_TLMTF_FINANCING_FIL.bar
```

> Exact flags depend on your ACE version and whether you deploy to an independent
> Integration Server (`ibmint`/`IntegrationServer`) or a managed node (`mqsideploy`).
> Bob adapts the commands to the local runtime; you approve before it runs them.

## Notes
- The reference projects under `Existing_Templates/` remain the source of truth; this
  setup never modifies them.
- Everything here is additive — the rest of the repository (Java/Python MVP, `mvp-web/`)
  is unchanged and still works independently.
