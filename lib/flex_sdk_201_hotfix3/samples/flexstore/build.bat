@echo off
SET OPTS=-use-network=false
..\..\bin\mxmlc.exe %OPTS% flexstore.mxml
..\..\bin\mxmlc.exe %OPTS% beige.css
..\..\bin\mxmlc.exe %OPTS% blue.css