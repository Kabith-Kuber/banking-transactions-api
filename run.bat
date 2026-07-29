@echo off
cd /d "%~dp0"
echo.
echo  BrainRidge Banking
echo  ------------------
echo  Starting the app... this may take a minute the first time.
echo.
echo  When you see "Started BankingApplication", open your browser to:
echo  http://localhost:8080
echo.
call mvnw.cmd spring-boot:run
pause
