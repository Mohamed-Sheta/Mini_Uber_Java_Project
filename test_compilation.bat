@echo off
cd C:\Users\LOQ\IdeaProjects\MiniUber_IntelliJ_Project\Mini_Uber_Java_Project
javac -cp "libs/*" -d . src/services/RideManager.java src/Model/Passenger.java 2>&1
if %errorlevel% equ 0 (
    echo Compilation successful!
) else (
    echo Compilation failed!
)
pause

