@echo off
chcp 65001 >nul
set "BIN_DIR=%~dp0"
set "LIB_DIR=%BIN_DIR%..\lib"
set "APP_JAR=%LIB_DIR%\tool-suite-1.0-SNAPSHOT-shaded.jar"
set "JAVAFX_MP=%LIB_DIR%\javafx"

if not exist "%APP_JAR%" (
  echo Jar not found: %APP_JAR%
  exit /b 1
)
if not exist "%JAVAFX_MP%" (
  echo JavaFX not found: %JAVAFX_MP%
  exit /b 1
)

start "" javaw --module-path "%JAVAFX_MP%" --add-modules javafx.controls,javafx.fxml,javafx.web -cp "%APP_JAR%" io.github.lemostic.toolsuite.WorkBenchApplication %*
exit
