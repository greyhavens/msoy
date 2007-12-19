@echo off
REM other locales are de_DE, fr_FR, and ja_JP
SET OPTS=-use-network=false -library-path+=../../frameworks/locale/{locale} -source-path+=locale/{locale} -locale=en_US
..\..\bin\mxmlc.exe %OPTS% PhotoViewer.mxml