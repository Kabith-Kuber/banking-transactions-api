@echo off
cd /d "%~dp0"
echo Starting Banking Transactions API...
call mvnw.cmd spring-boot:run
pause
