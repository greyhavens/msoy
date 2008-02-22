@echo off

rem
rem acompc.bat script for Windows.
rem This simply executes compc.exe in the same directory,
rem inserting the option +configname=air, which makes
rem compc.exe use air-config.xml instead of flex-config.xml.
rem On Unix, acompc is used instead.
rem

"%~dp0compc.exe" +configname=air %*

