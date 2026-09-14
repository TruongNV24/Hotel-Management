@echo off
echo ========================================
echo HOTEL MANAGEMENT - BUILD AND RUN
echo ========================================
echo.

REM Change to project directory
cd /d "%~dp0"

echo [1/3] Killing any existing Java processes...
taskkill /F /IM java.exe >nul 2>&1
timeout /t 2 /nobreak

echo [2/3] Building project with Maven...
call mvn clean package -q
if %ERRORLEVEL% neq 0 (
    echo.
    echo ERROR: Build failed!
    pause
    exit /b 1
)
echo Build SUCCESS!

echo.
echo [3/3] Starting application...
echo Looking for JAR file...

if exist "target\Hotel-Management-1.0-SNAPSHOT-shaded.jar" (
    echo Found: Hotel-Management-1.0-SNAPSHOT-shaded.jar
    echo Starting application...
    java -jar "target\Hotel-Management-1.0-SNAPSHOT-shaded.jar"
) else if exist "target\Hotel-Management-1.0-SNAPSHOT.jar" (
    echo Found: Hotel-Management-1.0-SNAPSHOT.jar
    echo Starting application...
    java -jar "target\Hotel-Management-1.0-SNAPSHOT.jar"
) else (
    echo ERROR: No JAR file found in target directory!
    echo Please run: mvn clean package
    pause
    exit /b 1
)

pause
