@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM  ACE FlowSmith - build a self-contained flowsmith.jar (no Maven required)
REM  Run this from the flowsmith-java directory:  build.bat
REM
REM  Requires: JDK 8+ on PATH, and the Apache POI jars in a lib\ folder.
REM  Output:   flowsmith.jar (fat jar - includes POI, runnable with java -jar)
REM ============================================================================

set SRC=src
set BIN=bin
set STAGE=build_stage
set LIB=lib
set JAR=flowsmith.jar
set MAIN=com.flowsmith.FlowSmith

echo.
echo [1/5] Checking for JDK (javac)...
where javac >nul 2>&1
if errorlevel 1 (
  echo ERROR: javac not found. Install a JDK 8+ and add it to PATH.
  exit /b 1
)

echo [2/5] Checking Apache POI jars in %LIB%\ ...
if not exist "%LIB%\*.jar" (
  echo ERROR: No jars found in %LIB%\ .
  echo        Download the Apache POI 5.2.3 binary bundle from
  echo        https://poi.apache.org/download.html and copy these jars into %LIB%\ :
  echo          poi-5.2.3.jar  poi-ooxml-5.2.3.jar  poi-ooxml-lite-5.2.3.jar
  echo          xmlbeans-5.1.1.jar  commons-compress-1.21.jar
  echo          commons-collections4-4.4.jar  commons-io-2.11.0.jar
  echo          log4j-api-2.18.0.jar  SparseBitSet-1.2.jar
  exit /b 1
)

echo [3/5] Compiling sources...
if exist "%BIN%" rmdir /s /q "%BIN%"
mkdir "%BIN%"
javac -cp "%LIB%\*" -d "%BIN%" %SRC%\com\flowsmith\*.java
if errorlevel 1 (
  echo ERROR: compilation failed.
  exit /b 1
)

echo [4/5] Assembling fat jar (unpacking POI into the jar)...
if exist "%STAGE%" rmdir /s /q "%STAGE%"
mkdir "%STAGE%"
REM unpack every dependency jar into the staging folder
pushd "%STAGE%"
for %%f in (..\%LIB%\*.jar) do (
  jar xf "%%f"
)
popd
REM layer our compiled classes on top
xcopy /e /y /q "%BIN%\*" "%STAGE%\" >nul
REM remove signature files that would make java -jar reject the bundled jars
if exist "%STAGE%\META-INF\*.SF"  del /q "%STAGE%\META-INF\*.SF"
if exist "%STAGE%\META-INF\*.RSA" del /q "%STAGE%\META-INF\*.RSA"
if exist "%STAGE%\META-INF\*.DSA" del /q "%STAGE%\META-INF\*.DSA"

echo [5/5] Creating %JAR% with Main-Class %MAIN% ...
if exist "%JAR%" del /q "%JAR%"
jar cfe "%JAR%" %MAIN% -C "%STAGE%" .
if errorlevel 1 (
  echo ERROR: jar creation failed.
  exit /b 1
)

REM cleanup intermediates
rmdir /s /q "%BIN%"
rmdir /s /q "%STAGE%"

echo.
echo ============================================================================
echo  DONE. Built %JAR% (self-contained - includes Apache POI).
echo  Verify:  jar tf %JAR% ^| findstr /i "MappingDocument ESQLMappingGenerator poi"
echo  Run:     java -jar %JAR% generate --subsys XAJ --app TLMTF --func FINANCING ^
--mapping example-mapping.xlsx
echo ============================================================================
echo.
endlocal
