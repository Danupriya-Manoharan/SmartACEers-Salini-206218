# Pattern: PTP File-to-File (`ptp_file`)

**Integration type:** PTP · **Connectivity:** FILE
**Reference project:** `Existing_Templates/subsys_ptp_appnm_funcnm_file/`
**App project:** `SUBSYS_PTP_APPNM_FUNCNM_FIL`
**Config project:** `SUBSYS_PTP_APPNM_FUNCNM_FIL_Configs`

## What it does
Point-to-point batch flow. Reads files from an input directory, resets the content
descriptor, and routes them file-to-file via the ED6 batch fileIn/fileOut connector
subflows. No MQ involved.

## Choose this when the requirement is about
- Moving or transferring files directory-to-directory
- Passthrough / "just move the file", batch file transfer
- File-to-file with no publish/subscribe and no queue

Do **not** choose this if a queue (MQ) is mentioned on either side — use `pub_file`
(file→queue) or a `sub_*` pattern (queue→file) instead.

## Tokens
- Required: `SUBSYS`, `APPNM`, `FUNCNM`
- Optional: none

## Key artifacts (to tokenise)
- `PTP/SUBSYS/APPNM/FUNCNM/FIL/Adapter.msgflow`
- `PTP/SUBSYS/APPNM/FUNCNM/FIL/Adapter_Compute.esql` — `BROKER SCHEMA PTP.SUBSYS.APPNM.FUNCNM.FIL`
- `..._Configs/batchconfig/{DEV,ACC,PRO}/PTP_SUBSYS_APPNM_FUNCNM.prop`
  (keys: `RUNTIME.LOCATION`, `FLOW.MODE=PTP`, `INPUT.DIR`, `OUTPUT.DIR`, `INPUT.FILE`, `OUTPUT.FILE`)

## Result
Project `<SUBSYS>_PTP_<APPNM>_<FUNCNM>_FIL` depending on the shared libraries
`ED6_BatchFramework_Shared`, `ED6_CommonFunctions_Shared`, `ED6_MessageLoggingTracking_Shared`.
