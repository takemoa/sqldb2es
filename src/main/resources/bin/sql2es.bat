@echo off

SETLOCAL enabledelayedexpansion
TITLE SQL2ES 1.0

SET params='%*'

:loop
FOR /F "usebackq tokens=1* delims= " %%A IN (!params!) DO (
    SET current=%%A
    SET params='%%B'
	SET silent=N

	IF "!current!" == "-s" (
		SET silent=Y
	)
	IF "!current!" == "--silent" (
		SET silent=Y
	)

	IF "!silent!" == "Y" (
		SET nopauseonerror=Y
	) ELSE (
	    IF "x!newparams!" NEQ "x" (
	        SET newparams=!newparams! !current!
        ) ELSE (
            SET newparams=!current!
        )
	)

    IF "x!params!" NEQ "x" (
		GOTO loop
	)
)

if DEFINED JAVA_HOME goto cont

:err
ECHO JAVA_HOME environment variable must be set! 1>&2
EXIT /B 1 

:cont
set SCRIPT_DIR=%~dp0
for %%I in ("%SCRIPT_DIR%..") do set S2E_HOME=%%~dpfI


REM ***** JAVA options *****

REM set to headless, just in case
set JAVA_OPTS=%JAVA_OPTS% -Djava.awt.headless=true

REM Disables explicit GC
set JAVA_OPTS=%JAVA_OPTS% -XX:+DisableExplicitGC

REM Ensure UTF-8 encoding by default (e.g. filenames)
set JAVA_OPTS=%JAVA_OPTS% -Dfile.encoding=UTF-8

REM Add also config to the classpath so that log4j will fing the config
set S2E_CLASSPATH=%S2E_CLASSPATH%;%S2E_HOME%/lib/*;%S2E_HOME%/config/
set S2E_PARAMS=-Dsql2es.path.home="%S2E_HOME%"

if DEFINED S2E_CONFIG (
    set S2E_PARAMS=%S2E_PARAMS%  -Dsql2es.path.config=%S2E_CONFIG%
)

IF ERRORLEVEL 1 (
	IF NOT DEFINED nopauseonerror (
		PAUSE
	)
	EXIT /B %ERRORLEVEL%
)

echo "%JAVA_HOME%\bin\java" %JAVA_OPTS% %S2E_JAVA_OPTS% %S2E_PARAMS% !newparams! -cp "%S2E_CLASSPATH%" "org.takemoa.sql2es.App"
echo ""

"%JAVA_HOME%\bin\java" %JAVA_OPTS% %S2E_JAVA_OPTS% %S2E_PARAMS% !newparams! -cp "%S2E_CLASSPATH%" "org.takemoa.sql2es.App"

ENDLOCAL
