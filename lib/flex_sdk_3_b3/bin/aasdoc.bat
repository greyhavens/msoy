@echo off

rem
rem aasdoc.bat script for Windows.
rem This simply executes asdoc.exe in the same directory,
rem inserting the option +configname=air, which makes
rem asdoc.exe use air-config.xml instead of flex-config.xml.
rem On Unix, aasdoc is used instead.
rem

"%~dp0asdoc.exe" +configname=air %*

