@ECHO OFF
SETLOCAL

set WRAPPER_DIR=%~dp0.mvn\wrapper
set PROPERTIES_FILE=%WRAPPER_DIR%\maven-wrapper.properties
set MAVEN_BASE_DIR=%~dp0.mvn
set DIST_DIR=%MAVEN_BASE_DIR%\apache-maven

if not exist "%PROPERTIES_FILE%" (
  echo Missing "%PROPERTIES_FILE%".
  exit /b 1
)

for /f "tokens=1,* delims==" %%A in (%PROPERTIES_FILE%) do (
  if "%%A"=="distributionUrl" set DISTRIBUTION_URL=%%B
)

if "%DISTRIBUTION_URL%"=="" (
  echo distributionUrl is missing in "%PROPERTIES_FILE%".
  exit /b 1
)

for %%F in ("%DISTRIBUTION_URL%") do set DIST_FILE=%%~nxF
set ZIP_PATH=%WRAPPER_DIR%\%DIST_FILE%
set EXTRACTED_DIR=%DIST_DIR%\apache-maven-3.9.9
set MVN_CMD=%EXTRACTED_DIR%\bin\mvn.cmd

if not exist "%MVN_CMD%" (
  if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"
  if not exist "%ZIP_PATH%" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%ZIP_PATH%'"
    if errorlevel 1 exit /b 1
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%ZIP_PATH%' -DestinationPath '%DIST_DIR%' -Force"
  if errorlevel 1 exit /b 1
)

call "%MVN_CMD%" %*
ENDLOCAL
