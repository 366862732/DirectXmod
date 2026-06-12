@echo off
setlocal enabledelayedexpansion

echo ========================================
echo Building gl4dx12.dll
echo ========================================
echo.

set VS_PATH=D:\Visual studio
set SDK_PATH=D:\VS SDKS
set JAVA_HOME=C:\Program Files\BellSoft\LibericaJDK-25-Full

echo Using Java: %JAVA_HOME%
echo.

set VC_VARS_PATH=%VS_PATH%\VC\Auxiliary\Build\vcvars64.bat

if not exist "%VC_VARS_PATH%" (
    echo ERROR: Cannot find vcvars64.bat
    echo Path: %VC_VARS_PATH%
    exit /b 1
)

echo Found vcvars64.bat
call "%VC_VARS_PATH%" x64

if %errorlevel% neq 0 (
    echo ERROR: Failed to set up VS environment
    exit /b 1
)

set JNI_INCLUDE=/I "%JAVA_HOME%\include" /I "%JAVA_HOME%\include\win32"

if not exist "%JAVA_HOME%\include\jni.h" (
    echo ERROR: Cannot find jni.h
    exit /b 1
)

echo JNI includes configured
echo.

set COMPILE_FLAGS=/nologo /EHsc /O2 /MD /LD /std:c++17
set LINK_FLAGS=d3d12.lib dxgi.lib dxguid.lib user32.lib

echo Compiling d3d12bridge.cpp...
cl %COMPILE_FLAGS% %JNI_INCLUDE% d3d12bridge.cpp /link %LINK_FLAGS% /OUT:gl4dx12.dll

if %errorlevel% neq 0 (
    echo.
    echo ERROR: Compilation failed
    exit /b 1
)

echo.
echo SUCCESS: gl4dx12.dll built
echo.

set RESOURCE_DIR=..\..\resources\native\windows
if not exist "%RESOURCE_DIR%" mkdir "%RESOURCE_DIR%"
copy /Y gl4dx12.dll "%RESOURCE_DIR%\"

echo Copied to resources
echo.
echo Done.

exit /b 0