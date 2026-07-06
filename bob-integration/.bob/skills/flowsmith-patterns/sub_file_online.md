# Pattern: SUB Subscriber, Online publishing (`sub_file_pubonline`)

**Integration type:** SUB · **Connectivity:** FILE · **Publisher type:** PUBONLINE
**Reference project:** `Existing_Templates/subsys_sub_appnm_funcnm_file_pubonline/`
**App project:** `SUBSYS_SUB_APPNM_FUNCNM_FIL`
**Config project:** `SUBSYS_SUB_APPNM_FUNCNM_FIL_Configs`

## What it does
Subscriber batch flow tuned for **ONLINE** publishing. Consumes from MQ, detects the last
message in a group (`isLastMsgGroup`) and writes files.

## Choose this when the requirement is about
- Consuming messages from a queue and writing files (queue → file)
- The upstream publisher emits messages **online / continuously / in real time**
- "Subscribe", "consume", "receive message", "MQ to file", "real-time consumer"

## The SUB decision (important)
Use this **online** variant when messages arrive continuously. If the upstream sends
**grouped / batched** messages and you must act only at **end of batch**, choose
[`sub_file_batch.md`](sub_file_batch.md) instead — that one uses an `Adapter_IsTrigger`
compute rather than `isLastMsgGroup` detection.

## Tokens
- Required: `SUBSYS`, `APPNM`, `FUNCNM`
- Optional: `NDMNM`

## Key artifacts (to tokenise)
- `SUB/SUBSYS/APPNM/FUNCNM/FIL/Adapter.msgflow`
- `SUB/SUBSYS/APPNM/FUNCNM/FIL/Adapter_Compute.esql`
- `SUB/SUBSYS/APPNM/FUNCNM/FIL/isLastMsgGroup.esql`
- `..._Configs/embeddedMQ.properties`
- `..._Configs/batchconfig/{DEV,ACC,PRO}/SUB_SUBSYS_APPNM_FUNCNM.prop`

## Result
Project `<SUBSYS>_SUB_<APPNM>_<FUNCNM>_FIL` (online consumer).
