@echo off

SETLOCAL enabledelayedexpansion
TITLE SQL2ES TUTORIAL 1.0

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

set THIS_SCRIPT_DIR=%~dp0
for %%I in ("%THIS_SCRIPT_DIR%..") do set THIS_HOME=%%~dpfI

set S2E_CONFIG=%THIS_HOME%/config

%THIS_HOME%/../bin/sql2es.bat

ENDLOCAL
