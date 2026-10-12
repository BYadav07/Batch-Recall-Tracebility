@echo off
rem Compile and run Batch Recall Traceability (needs JDK 8+ and lib\ojdbc8.jar or lib\ojdbc11.jar)
cd /d "%~dp0"
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
if errorlevel 1 goto :end
java -cp "out;lib\*" trace.App db.properties
:end
pause
