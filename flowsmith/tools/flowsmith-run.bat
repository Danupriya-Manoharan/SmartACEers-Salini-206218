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
REM
REM Uses pushd (not cd) so it also works when the workspace is on a UNC / network
REM path (\\server\share\...) where plain "cd" fails with
REM "The filename, directory name, or volume syntax is incorrect".

setlocal EnableExtensions

REM Resolve the flowsmith\ folder (this script lives in flowsmith\tools\).
REM pushd maps a temporary drive letter for UNC paths, so cd works afterwards.
pushd "%~dp0.." || (echo ERROR: cannot enter "%~dp0.." & exit /b 1)

REM Locate Python (python or py launcher).
set "PY=python"
where python >nul 2>&1 || set "PY=py"

set "NDM_ARG="
if not "%~5"=="" if /I not "%~5"=="NONE" set "NDM_ARG=--ndm %~5"

REM Output: %6 if given, else the workspace root (parent of this project),
REM so generated projects are SIBLINGS of this project, not nested inside it.
set "OUT_DIR=%~6"
if "%OUT_DIR%"=="" set "OUT_DIR=%CD%\..\.."

echo === FlowSmith (Toolkit) ===
echo Working dir : %CD%
echo Python      : %PY%
echo Pattern     : %~1   Tokens: SUBSYS=%~2 APPNM=%~3 FUNCNM=%~4 NDMNM=%~5
echo Output      : %OUT_DIR%
echo.

%PY% flowsmith.py generate --pattern %~1 --subsys %~2 --app %~3 --func %~4 %NDM_ARG% --out "%OUT_DIR%" --force
set "RC=%ERRORLEVEL%"

echo.
if "%RC%"=="0" (
  echo ^>^>^> Done. In the Toolkit: File ^> Import ^> Existing Projects, root = %OUT_DIR%
  echo ^>^>^> ^(The workspace is auto-refreshed by this launch configuration.^)
) else (
  echo ^>^>^> FlowSmith failed with exit code %RC%. See messages above.
)

popd
endlocal & exit /b %RC%
