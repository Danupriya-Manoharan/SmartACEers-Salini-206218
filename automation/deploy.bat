@echo off
REM ============================================================================
REM ACE FlowSmith AI - Automated Deployment Script
REM ============================================================================
REM This script automates the complete deployment workflow:
REM 1. Build BAR file from generated project
REM 2. Start Queue Manager
REM 3. Start Integration Node
REM 4. Deploy BAR file to Integration Server
REM ============================================================================

setlocal enabledelayedexpansion

echo.
echo ========================================================================
echo   ACE FlowSmith AI - Automated Deployment
echo ========================================================================
echo.

REM ============================================================================
REM Configuration - Get user inputs or use defaults
REM ============================================================================

set /p PROJECT_NAME="Enter Project Name (e.g., XAJ_PUB_TLMTF_FINANCING_FIL): "
if "%PROJECT_NAME%"=="" (
    echo ERROR: Project name is required
    exit /b 1
)

set /p QUEUE_MANAGER="Enter Queue Manager Name [default: MB8QMGR]: "
if "%QUEUE_MANAGER%"=="" set QUEUE_MANAGER=MB8QMGR

set /p INTEGRATION_NODE="Enter Integration Node Name [default: Test_node_test]: "
if "%INTEGRATION_NODE%"=="" set INTEGRATION_NODE=Test_node_test

set /p INTEGRATION_SERVER="Enter Integration Server Name [default: default]: "
if "%INTEGRATION_SERVER%"=="" set INTEGRATION_SERVER=default

set /p WORKSPACE="Enter Workspace Path [default: C:\Users\%USERNAME%\git\FlowSmith_Generated]: "
if "%WORKSPACE%"=="" set WORKSPACE=C:\Users\%USERNAME%\git\FlowSmith_Generated

REM BAR file output directory
set BAR_OUTPUT=%WORKSPACE%\BAR_Files
if not exist "%BAR_OUTPUT%" mkdir "%BAR_OUTPUT%"

set BAR_FILE=%BAR_OUTPUT%\%PROJECT_NAME%.bar

echo.
echo Configuration:
echo   Project Name      : %PROJECT_NAME%
echo   Queue Manager     : %QUEUE_MANAGER%
echo   Integration Node  : %INTEGRATION_NODE%
echo   Integration Server: %INTEGRATION_SERVER%
echo   Workspace         : %WORKSPACE%
echo   BAR File          : %BAR_FILE%
echo.

pause

REM ============================================================================
REM Step 1: Build BAR File
REM ============================================================================

echo.
echo [Step 1/4] Building BAR file...
echo ========================================================================

REM Check if mqsicreatebar command exists
where mqsicreatebar >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ERROR: mqsicreatebar command not found. Please ensure ACE is installed and in PATH.
    exit /b 1
)

REM Build BAR file
echo Building BAR file for project: %PROJECT_NAME%
mqsicreatebar -data "%WORKSPACE%" -b "%BAR_FILE%" -a "%PROJECT_NAME%" -o

if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to create BAR file
    exit /b 1
)

echo SUCCESS: BAR file created at %BAR_FILE%

REM ============================================================================
REM Step 2: Start Queue Manager
REM ============================================================================

echo.
echo [Step 2/4] Starting Queue Manager...
echo ========================================================================

REM Check if Queue Manager is already running
dspmq -m %QUEUE_MANAGER% | findstr /C:"Running" >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo Queue Manager %QUEUE_MANAGER% is already running
) else (
    echo Starting Queue Manager %QUEUE_MANAGER%...
    strmqm %QUEUE_MANAGER%
    
    if %ERRORLEVEL% neq 0 (
        echo ERROR: Failed to start Queue Manager
        exit /b 1
    )
    
    REM Wait for Queue Manager to start
    timeout /t 5 /nobreak >nul
    echo SUCCESS: Queue Manager started
)

REM ============================================================================
REM Step 3: Start Integration Node
REM ============================================================================

echo.
echo [Step 3/4] Starting Integration Node...
echo ========================================================================

REM Check if Integration Node is already running
mqsilist | findstr /C:"%INTEGRATION_NODE%" | findstr /C:"running" >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo Integration Node %INTEGRATION_NODE% is already running
) else (
    echo Starting Integration Node %INTEGRATION_NODE%...
    mqsistart %INTEGRATION_NODE%
    
    if %ERRORLEVEL% neq 0 (
        echo ERROR: Failed to start Integration Node
        exit /b 1
    )
    
    REM Wait for Integration Node to start
    timeout /t 10 /nobreak >nul
    echo SUCCESS: Integration Node started
)

REM ============================================================================
REM Step 4: Deploy BAR File
REM ============================================================================

echo.
echo [Step 4/4] Deploying BAR file to Integration Server...
echo ========================================================================

echo Deploying %BAR_FILE% to %INTEGRATION_NODE%:%INTEGRATION_SERVER%...
mqsideploy %INTEGRATION_NODE% -e %INTEGRATION_SERVER% -a "%BAR_FILE%"

if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to deploy BAR file
    exit /b 1
)

echo SUCCESS: BAR file deployed

REM Wait for deployment to complete
timeout /t 5 /nobreak >nul

REM ============================================================================
REM Verification
REM ============================================================================

echo.
echo [Verification] Checking deployment status...
echo ========================================================================

mqsilist %INTEGRATION_NODE% -e %INTEGRATION_SERVER% -d 2

echo.
echo ========================================================================
echo   Deployment Complete!
echo ========================================================================
echo.
echo   Project: %PROJECT_NAME%
echo   BAR File: %BAR_FILE%
echo   Deployed to: %INTEGRATION_NODE%:%INTEGRATION_SERVER%
echo.
echo   Next Steps:
echo   1. Test the deployed flow
echo   2. Monitor logs: mqsireadlog %INTEGRATION_NODE%
echo   3. Check message flow status in ACE Toolkit
echo.
echo ========================================================================

pause

@REM Made with Bob
