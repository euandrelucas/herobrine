@echo off
set "DIR=%~dp0"
if exist "%DIR%maven\apache-maven-3.9.6\bin\mvn.cmd" (
    "%DIR%maven\apache-maven-3.9.6\bin\mvn.cmd" %*
) else (
    mvn %*
)
