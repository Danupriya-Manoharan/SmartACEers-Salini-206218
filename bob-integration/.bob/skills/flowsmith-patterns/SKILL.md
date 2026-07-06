---
name: flowsmith-patterns
description: >
  Match a plain-text ACE integration requirement to the correct reusable pattern,
  then instantiate that pattern's reference project with organisation tokens.
version: 0.1.0
---

# FlowSmith Pattern Catalog (Bob Skill)

This skill is the **"which template fits?" engine**. When the developer describes what
they need in plain English, use this skill to choose the single best pattern, then
instantiate it. This replaces the legacy keyword matcher in `flowsmith/catalog.json`
with semantic reasoning — but the catalog remains the source of truth for token names,
template locations and key artifacts.

## Organisation tokens

| Token   | Meaning                                   | Example    | Required |
|---------|-------------------------------------------|------------|----------|
| SUBSYS  | Subsystem code (uppercased)               | XAJ        | yes      |
| APPNM   | Application code                          | TLMTF      | yes      |
| FUNCNM  | Functionality name                        | FINANCING  | yes      |
| NDMNM   | NDM dataset name (REFPGN/INFUID)          | REFPGN     | some PUB/SUB only |

Environments are always `DEV`, `ACC`, `PRO`.

## How to select a pattern (the "search engine")

1. Read the requirement. Identify **direction** (file→file, file→queue, queue→file) and
   **cadence** (online/continuous vs batched/grouped/end-of-batch).
2. Score each pattern below on *intent*, not literal keywords. A requirement that says
   "pick up files and just move them to another directory" is PTP even if it never says
   the word "point-to-point".
3. Pick the single best pattern. If two are close, state the runner-up and one-line reason.
4. Confirm required tokens are present; ask only for what's missing.

| Pattern file | id | Direction | Use when the requirement is about… |
|---|---|---|---|
| [ptp_file.md](ptp_file.md) | `ptp_file` | File → File | moving/transferring files directory-to-directory, passthrough, no MQ |
| [pub_file.md](pub_file.md) | `pub_file` | File → MQ | reading files and publishing/emitting them onto an ESB MQ queue |
| [sub_file_online.md](sub_file_online.md) | `sub_file_pubonline` | MQ → File | consuming messages published **online/continuously** and writing files |
| [sub_file_batch.md](sub_file_batch.md) | `sub_file_pubbatch` | MQ → File | consuming **grouped/batched** messages, acting at **end of batch** |

The critical SUB distinction: **online** = `isLastMsgGroup` detection; **batch** =
`Adapter_IsTrigger` compute that fires only on the last message of a batch group.

## How to instantiate the chosen pattern

1. Copy the pattern's `templateDir` (see its pattern file) — e.g.
   `Existing_Templates/subsys_ptp_appnm_funcnm_file/`.
2. Replace tokens **everywhere**: folder names, the `_Configs` project, `.project`
   `<name>`, `BROKER SCHEMA <TYPE>.SUBSYS.APPNM.FUNCNM.FIL`, ESQL, `.msgflow`, and every
   `DEV/ACC/PRO` `.prop` file under `batchconfig/`.
3. Target project name: `<SUBSYS>_<TYPE>_<APPNM>_<FUNCNM>_FIL` (TYPE ∈ PTP/PUB/SUB).
4. Apply all `rules-flowsmith/*.xml` before presenting for review.
5. Never invent new logging/error-handling code — reuse the shared libraries
   (see `rules-flowsmith/2_logging_and_shared_libraries.xml`).

## After review

On the developer's approval, build and deploy from the Bob Shell — see
`../../docs/setup.md` for the exact `ibmint` / `mqsi` commands.
