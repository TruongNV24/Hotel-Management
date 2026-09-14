@echo off
setlocal enabledelayedexpansion

echo.
echo ========================================
echo  HOTEL MANAGEMENT SYSTEM - BUILD & RUN
echo ========================================
echo.

REM Change to project directory
cd /d "%~dp0"

echo [*] Killing any existing Java processes...
taskkill /F /IM java.exe >nul 2>&1
timeout /t 2 /nobreak >nul

echo [*] Building project...
call mvn clean package -DskipTests -q

if %ERRORLEVEL% neq 0 (
    echo.
    echo ERROR: Build failed!
    pause
    exit /b 1
)

echo [*] Build SUCCESS!
echo [*] Checking JAR file...

if exist "target\Hotel-Management-1.0-SNAPSHOT.jar" (
    echo [*] JAR file found: Hotel-Management-1.0-SNAPSHOT.jar
    echo.
    echo [*] Starting application...
    echo ========================================
    echo Press Ctrl+C to stop the application
    echo ========================================
    echo.
    java -jar "target\Hotel-Management-1.0-SNAPSHOT.jar"
) else (
    echo ERROR: JAR file not found!
    pause
    exit /b 1
)
