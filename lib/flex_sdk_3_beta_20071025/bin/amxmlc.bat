@echo off

rem
rem amxmlc.bat script for Windows.
rem This simply executes mxmlc.exe in the same directory,
rem inserting the option +configname=air, which makes
rem mxmlc.exe use air-config.xml instead of flex-config.xml.
rem On Unix, amxmlc is used instead.
rem

"%~dp0mxmlc.exe" +configname=air %*

