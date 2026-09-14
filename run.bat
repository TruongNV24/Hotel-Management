@echo off
cd /d %~dp0

set JAR=

REM Rebuild before running to avoid stale/corrupted JAR files causing runtime ClassFormatError
call mvn clean package -DskipTests -q
if %ERRORLEVEL% neq 0 (
  echo Build failed. Please check Maven output.
  exit /b 1
)

for %%f in (target\*-all.jar target\*-shaded.jar target\*.jar) do (
  if exist "%%f" (
    set JAR=%%f
    goto :run
  )
)
:run
if "%JAR%"=="" (
  echo No jar found in target\. Please run: mvn clean package
  exit /b 1
)

echo Running %JAR%
java -jar "%JAR%"
