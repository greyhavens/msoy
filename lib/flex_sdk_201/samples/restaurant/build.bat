@echo off
SET OPTS=-use-network=true -accessible=true
..\..\bin\mxmlc.exe %OPTS% finder.mxml
..\..\bin\mxmlc.exe %OPTS% recentReviews.mxml
