@echo off
for /R . %%A in (build.bat) do if exist %%A (
echo processing %%A
cd /d %%~dpA
call build.bat
)

GOTO END
:END