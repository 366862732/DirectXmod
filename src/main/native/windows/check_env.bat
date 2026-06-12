@echo off
echo ========================================
echo Environment Check for GL4DX12 Build
echo ========================================
echo.

echo 1. Checking Visual Studio...
set VS_PATH=D:\Visual studio
if exist "%VS_PATH%\VC\Auxiliary\Build\vcvars64.bat" (
    echo   [OK] vcvars64.bat found
) else (
    echo   [FAIL] vcvars64.bat NOT found at %VS_PATH%\VC\Auxiliary\Build\vcvars64.bat
)

echo.
echo 2. Checking Windows SDK...
if exist "D:\VS SDKS\Include" (
    echo   [OK] Windows SDK found at D:\VS SDKS
) else if exist "C:\Program Files (x86)\Windows Kits\10\Include" (
    echo   [OK] Windows SDK found at default location
) else (
    echo   [WARN] Windows SDK not found in common locations
)

echo.
echo 3. Checking Java/JNI...
set JAVA_HOME=C:\Program Files\BellSoft\LibericaJDK-25-Full
if exist "%JAVA_HOME%\include\jni.h" (
    echo   [OK] jni.h found
) else (
    echo   [FAIL] jni.h NOT found at %JAVA_HOME%\include\jni.h
)

echo.
echo 4. Checking source files...
if exist "d3d12bridge.h" (
    echo   [OK] d3d12bridge.h exists
) else (
    echo   [FAIL] d3d12bridge.h NOT found
)

if exist "d3d12bridge.cpp" (
    echo   [OK] d3d12bridge.cpp exists
) else (
    echo   [FAIL] d3d12bridge.cpp NOT found
)

echo.
echo 5. Checking for required headers in d3d12bridge.h...
findstr /C:"#include <jni.h>" d3d12bridge.h >nul 2>&1
if %errorlevel% equ 0 ( echo   [OK] #include ^<jni.h^> found ) else ( echo   [FAIL] #include ^<jni.h^> missing )

findstr /C:"#include <d3d12.h>" d3d12bridge.h >nul 2>&1
if %errorlevel% equ 0 ( echo   [OK] #include ^<d3d12.h^> found ) else ( echo   [FAIL] #include ^<d3d12.h^> missing )

echo.
echo ========================================
echo Check complete.
echo ========================================
pause