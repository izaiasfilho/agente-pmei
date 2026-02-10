@echo off

set DATA=%date:~6,4%-%date:~3,2%-%date:~0,2%
set HORA=%time:~0,2%-%time:~3,2%-%time:~6,2%
set LOG=app-%DATA%_%HORA%.log

echo Iniciando aplicacao...
echo Log: %LOG%

java -jar agente-pmei-0.0.1-SNAPSHOT.jar > %LOG% 2>&1

pause
