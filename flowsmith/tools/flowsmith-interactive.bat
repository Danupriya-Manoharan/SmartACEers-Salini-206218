@echo off
REM FlowSmith standalone interactive launcher (Windows)
REM Double-click this file, or run it from a normal Command Prompt.
REM It prompts for inputs itself - it does NOT depend on Eclipse / ACE Toolkit
REM variables, so it's the most reliable way to verify generation works.
REM
REM Uses pushd so it also works on UNC / network paths.

setlocal EnableExtensions

pushd "%~dp0.." || (echo ERROR: cannot enter "%~dp0.." & pause & exit /b 1)

set "PY=python"
where python >nul 2>&1 || set "PY=py"

echo ============================================
echo   ACE FlowSmith - interactive generator
echo ============================================
echo Patterns: ptp_file ^| pub_file ^| sub_file_pubonline ^| sub_file_pubbatch
echo.
set "PATTERN="
set "SUBSYS="
set "APPNM="
set "FUNCNM="
set "NDMNM="
set "OUTDIR="
set /p PATTERN=Pattern id            :
set /p SUBSYS=Subsystem code SUBSYS :
set /p APPNM=Application code APPNM :
set /p FUNCNM=Functionality FUNCNM  :
set /p NDMNM=NDM name (optional)   :
set /p OUTDIR=Output dir (blank=..\Generated) :

if "%OUTDIR%"=="" set "OUTDIR=%CD%\..\Generated"

set "NDM_ARG="
if not "%NDMNM%"=="" set "NDM_ARG=--ndm %NDMNM%"

echo.
echo Generating into: %OUTDIR%
echo.
%PY% flowsmith.py generate --pattern %PATTERN% --subsys %SUBSYS% --app %APPNM% --func %FUNCNM% %NDM_ARG% --out "%OUTDIR%" --force
set "RC=%ERRORLEVEL%"

echo.
if "%RC%"=="0" (
  echo Done. Import into ACE Toolkit: File ^> Import ^> Existing Projects, root = %OUTDIR%
) else (
  echo FlowSmith failed with exit code %RC%.
)

popd
echo.
pause
endlocal & exit /b %RC%
