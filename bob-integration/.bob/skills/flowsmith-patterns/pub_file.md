# Pattern: PUB Publisher, File → MQ (`pub_file`)

**Integration type:** PUB · **Connectivity:** FILE
**Reference project:** `Existing_Templates/subsys_pub_appnm_funcnm_file/`
**App project:** `SUBSYS_PUB_APPNM_FUNCNM_FIL`
**Config project:** `SUBSYS_PUB_APPNM_FUNCNM_FIL_Configs`

## What it does
Publisher batch flow. Reads files and publishes them onto an ESB MQ queue via the ED6
`pub_file_connector` subflow. Generates embedded MQ queue definitions.

## Choose this when the requirement is about
- Reading files and publishing / emitting / broadcasting them onto a queue
- "File to queue", "file to MQ", "produce a message", "send downstream event"
- The application is the **producer** side of a pub/sub integration

## Tokens
- Required: `SUBSYS`, `APPNM`, `FUNCNM`
- Optional: `NDMNM` (NDM dataset — REFPGN/INFUID)

## Key artifacts (to tokenise)
- `PUB/SUBSYS/APPNM/FUNCNM/FIL/FileConnector.msgflow`
- `PUB/SUBSYS/APPNM/FUNCNM/FIL/Adapter.msgflow`
- `PUB/SUBSYS/APPNM/FUNCNM/FIL/Adapter_Map.esql`
- `..._Configs/embeddedMQ.properties` — MQ queue definitions
- `..._Configs/batchconfig/{DEV,ACC,PRO}/PUB_SUBSYS_APPNM_FUNCNM.prop`

## Result
Project `<SUBSYS>_PUB_<APPNM>_<FUNCNM>_FIL`. Pairs naturally with a `sub_*` subscriber on
the consuming side.
