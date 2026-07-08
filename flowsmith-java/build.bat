@echo off
setlocal

REM ============================================================================
REM  ACE FlowSmith - build flowsmith.jar (no Maven, no Apache POI required)
REM  Mapping documents are read as CSV using pure Java, so there are NO
REM  external dependencies - just the JDK.
REM  Run this from the flowsmith-java directory:  build.bat
REM ============================================================================

set SRC=src
set BIN=bin
set JAR=flowsmith.jar
set MAIN=com.flowsmith.FlowSmith

echo.
echo [1/3] Checking for JDK (javac)...
where javac >nul 2>&1
if errorlevel 1 (
  echo ERROR: javac not found. Install a JDK 17+ and add it to PATH.
  exit /b 1
)

echo [2/3] Compiling sources...
if exist "%BIN%" rmdir /s /q "%BIN%"
mkdir "%BIN%"
javac -d "%BIN%" %SRC%\com\flowsmith\*.java
if errorlevel 1 (
  echo ERROR: compilation failed.
  exit /b 1
)

echo [3/3] Creating %JAR% with Main-Class %MAIN% ...
if exist "%JAR%" del /q "%JAR%"
jar cfe "%JAR%" %MAIN% -C "%BIN%" .
if errorlevel 1 (
  echo ERROR: jar creation failed.
  exit /b 1
)

rmdir /s /q "%BIN%"

echo.
echo ============================================================================
echo  DONE. Built %JAR% (pure JDK - no external libraries).
echo  Verify:  jar tf %JAR% ^| findstr /i "MappingDocument ESQLMappingGenerator"
echo  Run:     java -jar %JAR% generate --subsys XAJ --app TLMTF --func FINANCING
echo ============================================================================
echo.
endlocal
