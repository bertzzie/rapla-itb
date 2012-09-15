@echo off
set _REALPATH=%~dp0
echo %_REALPATH%
call %_REALPATH%StopRaplaWrapper-NT.bat
call %_REALPATH%StartRaplaWrapper-NT.bat

