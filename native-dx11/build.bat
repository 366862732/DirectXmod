@echo off
setlocal EnableExtensions

rem Build dx11_mc.dll with MSVC + CMake.  The fabric-1.21.11 Gradle build
rem packages the DLL from native-dx11/build/bin automatically.
rem Run this from a "x64 Native Tools Command Prompt for VS 2022" or after
rem running vcvars64.bat.

cd /d "%~dp0"

if not defined JAVA_HOME (
    for /d %%D in ("C:\Program Files\Java\jdk-21*") do set "JAVA_HOME=%%~fD"
)
if not defined JAVA_HOME (
    for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do set "JAVA_HOME=%%~fD"
)
if not defined JAVA_HOME (
    echo [dx11] JAVA_HOME is not set and no JDK 21 was found.
    echo [dx11] Set JAVA_HOME to a 64-bit JDK 21 installation.
    exit /b 1
)

echo [dx11] JAVA_HOME=%JAVA_HOME%
cmake -S "%~dp0." -B "%~dp0build" -DCMAKE_BUILD_TYPE=Release -DJAVA_HOME="%JAVA_HOME%"
if errorlevel 1 exit /b 1

cmake --build "%~dp0build" --config Release
if errorlevel 1 exit /b 1

set "DLL=%~dp0build\bin\Release\dx11_mc.dll"
if not exist "%DLL%" set "DLL=%~dp0build\bin\dx11_mc.dll"
if not exist "%DLL%" (
    echo [dx11] Build succeeded but dx11_mc.dll was not found.
    exit /b 1
)

rem The fabric-1.21.11 Gradle build packages this DLL directly from
rem native-dx11/build/bin, so there is no need to copy it into the source
rem resources (which would also risk a stale duplicate in the jar).
echo [dx11] Built: %DLL%
echo [dx11] The jar task will package it automatically.
endlocal
