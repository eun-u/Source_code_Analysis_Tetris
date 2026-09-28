@echo off
setlocal
if exist "%~dp0out\tetris.jar" (
    java -jar "%~dp0out\tetris.jar"
    if errorlevel 1 pause
) else (
    powershell.exe -NoProfile -File "%~dp0build.ps1" -Task Run
    if errorlevel 1 pause
)
