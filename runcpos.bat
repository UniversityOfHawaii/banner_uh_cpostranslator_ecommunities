@echo off
REM 
REM   Clears out logs, compiles, packages, and runs CPOS in
REM   a Windows environment
REM 
echo.
echo ====================================================================
echo           Checking required directories...
echo ====================================================================
echo.
if exist logs\ (
    echo directory logs exists
) else (
    echo ERROR:  directory logs does not exist
    Exit /b
)
REM
if exist config\ (
    echo directory config exists
) else (
    echo ERROR:  directory config does not exist
    Exit /b
)
REM
echo.
echo ====================================================================
echo           Checking required properties files...
echo ====================================================================
echo.
if exist config/application.properties (
    echo config/application.properties exists
) else (
    echo ERROR: config/application.properties doesn't exist
    Exit /b
)
REM
if exist config/log4j2.xml (
    echo config/log4j2.xml exists
) else (
    echo ERROR: config/log4j2.xml doesn't exist
    Exit /b
)
REM
REM if exist config/oracle.properties (
REM     echo config/oracle.properties exists
REM ) else (
REM     echo ERROR: config/oracle.properties doesn't exist
REM     Exit /b
REM )
echo.
echo ====================================================================
echo           Removing old log files...
echo ====================================================================
echo.
set archive=logs\archives\%date:~-4%.%date:~4,2%.%date:~7,2%.%time:~0,2%.%time:~3,2%.%time:~6,2%
REM
echo Archivng logs to: %archive%
REM
dir logs
REM
mkdir %archive%
move logs\*.log "%archive%"
REM
echo.
echo ====================================================================
echo           Building...
echo ====================================================================
echo.
@echo on
call mvn clean
call mvn package
REM
echo.
echo ====================================================================
echo           Starting...
echo ====================================================================
echo.
REM
REM  Start the jar via the -jar command, pass in the location of all configs
REM
REM call java -jar target\cpostranslator-1.2.0.jar --spring.config.location=config\application.properties,config\oracle.properties
call java -jar target\cpostranslator-1.2.0.jar --spring.config.location=config\application.properties