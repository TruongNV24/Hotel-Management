@echo off
cd /d %~dp0
set JAR=
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
