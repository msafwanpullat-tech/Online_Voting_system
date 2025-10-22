@echo off
echo Adding sample data to the voting system...
echo.

REM Create sample voters
echo VOT001,John Smith,25,Male > voters.csv
echo VOT002,Jane Doe,30,Female >> voters.csv
echo VOT003,Bob Johnson,45,Male >> voters.csv
echo VOT004,Alice Brown,28,Female >> voters.csv
echo VOT005,Charlie Wilson,35,Male >> voters.csv

REM Create sample candidates
echo 1,Alice Johnson,Progressive Party,42,Female > candidates.csv
echo 2,Bob Smith,Conservative Alliance,38,Male >> candidates.csv
echo 3,Carol Davis,Green Future Party,29,Female >> candidates.csv

echo Sample data created successfully!
echo.
echo Voters: VOT001, VOT002, VOT003, VOT004, VOT005
echo Admin: admin / admin123
echo.
pause
