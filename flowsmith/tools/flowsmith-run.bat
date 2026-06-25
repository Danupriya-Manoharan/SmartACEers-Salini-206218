@echo off
REM FlowSmith Toolkit launcher (Windows)
REM Invoked by the Eclipse/ACE Toolkit "External Tools" launch config.
REM
REM Args (positional, passed in by the .launch string-prompt dialogs):
REM   %1 = pattern id   (ptp_file | pub_file | sub_file_pubonline | sub_file_pubbatch)
REM   %2 = SUBSYS       (subsystem code, e.g. XAJ)
REM   %3 = APPNM        (application code, e.g. TLMTF)
REM   %4 = FUNCNM       (functionality, e.g. FINANCING)
REM   %5 = NDMNM        (optional NDM name; leave blank to skip)
REM   %6 = OUT_DIR      (output dir; the .launch passes the Eclipse workspace path)

setlocal
REM Resolve the flowsmith\ folder (this script lives in flowsmith\tools\).
cd /d "%~dp0.."

set NDM_ARG=
if not "%~5"=="" set NDM_ARG=--ndm %~5

set OUT_DIR=%~6
if "%OUT_DIR%"=="" set OUT_DIR=%CD%\..\Generated

echo === FlowSmith (Toolkit) ===
echo Pattern: %~1   Tokens: SUBSYS=%~2 APPNM=%~3 FUNCNM=%~4 NDMNM=%~5
echo Output : %OUT_DIR%
echo.

python flowsmith.py generate --pattern %~1 --subsys %~2 --app %~3 --func %~4 %NDM_ARG% --out "%OUT_DIR%" --force

echo.
echo ^>^>^> Done. In the Toolkit: File ^> Import ^> Existing Projects, root = %OUT_DIR%
echo ^>^>^> (The workspace is auto-refreshed by this launch configuration.)
endlocal
