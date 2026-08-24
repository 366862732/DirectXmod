@echo off
setlocal enabledelayedexpansion

set "ROOT=d:\dx12-lib-template-26.1.2"
set "NATIVE_BUILD=%ROOT%\native\build"
set "FABRIC=%ROOT%\fabric"
set "RESOURCES_DLL=%FABRIC%\src\main\resources\dx12_mc.dll"
set "MC_VERSION=26.2-Fabric_0.19.3"
set "MC_DIR=D:\.minecraft\versions\%MC_VERSION%"
set "MC_DLL=%MC_DIR%\dx12mod\dx12_mc.dll"
set "MC_JAR=%MC_DIR%\mods\gl4dx12-0.1.0.jar"
set "BUILD_DLL=%NATIVE_BUILD%\bin\Release\dx12_mc.dll"
set "BUILD_JAR=%FABRIC%\build\libs\gl4dx12-0.1.0.jar"

set "do_clean=0"
set "do_jar=0"
set "do_dll=0"

if "%~1"=="" (
    set "do_clean=1"
    set "do_jar=1"
    set "do_dll=1"
) else (
    if "%~1"=="clean" set "do_clean=1"
    if "%~1"=="jar"   set "do_jar=1"
    if "%~1"=="dll"   set "do_dll=1"
    if "%~1"=="all"   (
        set "do_clean=1"
        set "do_jar=1"
        set "do_dll=1"
    )
)

echo.
echo ==== DX12-MC Deploy ====
echo.

if "%do_clean%"=="1" (
    echo [1/3] CMake configure...
    cmake -S "%ROOT%\native" -B "%NATIVE_BUILD%" -G "Visual Studio 18 2026" -A x64
    if %errorlevel% neq 0 ( echo ERROR: cmake configure failed & pause & exit /b 1 )
    echo       OK
)

if "%do_dll%"=="1" (
    echo [2/3] Building DLL...
    cmake --build "%NATIVE_BUILD%" --config Release --parallel
    if %errorlevel% neq 0 ( echo ERROR: DLL build failed & pause & exit /b 1 )
    if not exist "%BUILD_DLL%" ( echo ERROR: DLL not found & pause & exit /b 1 )
    echo       Copying to resources/...
    copy /Y "%BUILD_DLL%" "%RESOURCES_DLL%" >nul
    echo       Copying to game folder...
    if exist "%MC_DLL%" del /F "%MC_DLL%" >nul
    copy /Y "%BUILD_DLL%" "%MC_DLL%" >nul
    if %errorlevel% neq 0 ( echo WARNING: game folder copy failed (game may be running) )
    echo       DLL OK
)

if "%do_jar%"=="1" (
    echo [3/3] Building JAR...
    cd /d "%FABRIC%"
    .\gradlew.bat jar
    if %errorlevel% neq 0 ( echo ERROR: JAR build failed & pause & exit /b 1 )
    if not exist "%BUILD_JAR%" ( echo ERROR: JAR not found & pause & exit /b 1 )
    echo       Copying to game mods/...
    if exist "%MC_JAR%" del /F "%MC_JAR%" >nul
    copy /Y "%BUILD_JAR%" "%MC_JAR%" >nul
    if %errorlevel% neq 0 ( echo WARNING: game mods copy failed (game may be running) )
    echo       JAR OK
)

echo.
echo ==== Deploy complete ====
echo.
pause
