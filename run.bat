@echo off
cd /d "%~dp0"
echo Lancement de SecureStaff Desktop...
set "MVN=mvn"
where mvn >nul 2>nul
if errorlevel 1 set "MVN=C:\Users\douni\Downloads\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd"
"%MVN%" org.openjfx:javafx-maven-plugin:0.0.8:run
pause
