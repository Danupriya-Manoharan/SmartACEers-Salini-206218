#!/bin/sh
# FlowSmith Toolkit launcher (macOS / Linux)
# Invoked by the Eclipse/ACE Toolkit "External Tools" launch config.
#
# Args (positional, passed in by the .launch string-prompt dialogs):
#   $1 = pattern id   (ptp_file | pub_file | sub_file_pubonline | sub_file_pubbatch)
#   $2 = SUBSYS       (subsystem code, e.g. XAJ)
#   $3 = APPNM        (application code, e.g. TLMTF)
#   $4 = FUNCNM       (functionality, e.g. FINANCING)
#   $5 = NDMNM        (optional NDM name; leave blank to skip)
#   $6 = OUT_DIR      (output dir; the .launch passes the Eclipse workspace path)

set -e

# Resolve the flowsmith/ folder (this script lives in flowsmith/tools/).
SCRIPT_DIR=$(cd "$(dirname "$0")/.." && pwd)
cd "$SCRIPT_DIR"

PYTHON=$(command -v python3 || command -v python)
if [ -z "$PYTHON" ]; then
  echo "ERROR: Python 3 not found on PATH. Install it or edit this script." >&2
  exit 1
fi

NDM_ARG=""
[ -n "$5" ] && NDM_ARG="--ndm $5"

OUT_DIR=${6:-"$SCRIPT_DIR/../Generated"}

echo "=== FlowSmith (Toolkit) ==="
echo "Python : $PYTHON"
echo "Pattern: $1   Tokens: SUBSYS=$2 APPNM=$3 FUNCNM=$4 NDMNM=${5:-<none>}"
echo "Output : $OUT_DIR"
echo

"$PYTHON" flowsmith.py generate \
  --pattern "$1" --subsys "$2" --app "$3" --func "$4" $NDM_ARG \
  --out "$OUT_DIR" --force

echo
echo ">>> Done. In the Toolkit: File > Import > Existing Projects, root = $OUT_DIR"
echo ">>> (The workspace is auto-refreshed by this launch configuration.)"
