# Pattern: SUB Subscriber, Batch publishing (`sub_file_pubbatch`)

**Integration type:** SUB · **Connectivity:** FILE · **Publisher type:** PUBBATCH
**Reference project:** `Existing_Templates/subsys_sub_appnm_funcnm_file_pubbatch/`
**App project:** `SUBSYS_SUB_APPNM_FUNCNM_FIL`
**Config project:** `SUBSYS_SUB_APPNM_FUNCNM_FIL_Configs`

## What it does
Subscriber batch flow tuned for **BATCH** publishing. Consumes from MQ and uses an
`Adapter_IsTrigger` compute to fire **only on the last message of a batch group**, then
writes files.

## Choose this when the requirement is about
- Consuming **grouped / batched** messages from a queue
- Acting at **end of batch** / "trigger on last message" / "process when the group completes"
- "Consume grouped messages from queue at end of batch and write files" (the canonical demo)

## The SUB decision (important)
Use this **batch** variant when messages arrive in groups and processing must wait for the
whole batch. If the upstream publishes **online / continuously**, choose
[`sub_file_online.md`](sub_file_online.md) instead (`isLastMsgGroup` detection rather than
the `Adapter_IsTrigger` compute).

## Tokens
- Required: `SUBSYS`, `APPNM`, `FUNCNM`
- Optional: `NDMNM`

## Key artifacts (to tokenise)
- `SUB/SUBSYS/APPNM/FUNCNM/FIL/Adapter.msgflow`
- `SUB/SUBSYS/APPNM/FUNCNM/FIL/Adapter_Compute.esql`
- `SUB/SUBSYS/APPNM/FUNCNM/FIL/Adapter_IsTrigger.esql`
- `..._Configs/embeddedMQ.properties`
- `..._Configs/batchconfig/{DEV,ACC,PRO}/SUB_SUBSYS_APPNM_FUNCNM.prop`

## Result
Project `<SUBSYS>_SUB_<APPNM>_<FUNCNM>_FIL` (end-of-batch consumer).
