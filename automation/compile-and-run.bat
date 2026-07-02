@echo off
REM ============================================================================
REM ACE FlowSmith AI - Compile and Run Deployment Automation
REM ============================================================================

echo.
echo ========================================================================
echo   ACE FlowSmith AI - Deployment Automation Setup
echo ========================================================================
echo.

REM Check if Java is available
where javac >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ERROR: javac not found. Please ensure Java JDK is installed and in PATH.
    echo.
    echo Try using the Java from ACE Toolkit:
    echo set PATH=%%PATH%%;C:\Program Files (x86)\Common Files\Oracle\Java\java8path
    pause
    exit /b 1
)

echo [1/2] Compiling ACEDeployer.java...
javac ACEDeployer.java

if %ERRORLEVEL% neq 0 (
    echo ERROR: Compilation failed
    pause
    exit /b 1
)

echo SUCCESS: Compilation complete
echo.

REM Check if arguments were provided
if "%1"=="" (
    echo [2/2] Running in interactive mode...
    echo.
    java com.flowsmith.automation.ACEDeployer
) else (
    echo [2/2] Running with arguments: %*
    echo.
    java com.flowsmith.automation.ACEDeployer %*
)

echo.
pause

@REM Made with Bob
