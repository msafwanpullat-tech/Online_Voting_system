@echo off
echo Starting Voting System Server...
echo.
echo Make sure Java is installed and in your PATH
echo.
javac VotingSystemServer.java
if %errorlevel% neq 0 (
    echo Compilation failed! Please check Java installation.
    pause
    exit /b 1
)

echo Server compiled successfully!
echo Starting server on http://localhost:8080
echo.
echo Press Ctrl+C to stop the server
echo.

java VotingSystemServer
pause

