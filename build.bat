@echo off
chcp 65001 >nul
echo [GL4DX12] Building D3D12 bridge DLL...

:: Locate vcvars64.bat
set VCVARS="C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"
if not exist %VCVARS% (
    set VCVARS="C:\Program Files\Microsoft Visual Studio\2022\Professional\VC\Auxiliary\Build\vcvars64.bat"
)
if not exist %VCVARS% (
    set VCVARS="C:\Program Files\Microsoft Visual Studio\2022\Enterprise\VC\Auxiliary\Build\vcvars64.bat"
)
if not exist %VCVARS% (
    echo [ERROR] Cannot find vcvars64.bat! Check Visual Studio installation.
    pause
    exit /b 1
)

call %VCVARS% || (echo [ERROR] vcvars64.bat failed & pause & exit /b 1)

:: JDK include paths (adjust if needed)
set JDK_HOME=C:\Program Files\BellSoft\LibericaJDK-26-Full
set JDK_INC=/I "%JDK_HOME%\include" /I "%JDK_HOME%\include\win32"

:: Source file location
set SRC=src\main\native\windows\d3d12bridge.cpp

:: Target: copy to MC versions dir AND local for Gradle
set MC_TARGET="D:\.minecraft\versions\26.1.2-Fabric_0.19.3\dx12mod\d3d12bridge.dll"
set LOCAL_TARGET="src\main\resources\native\windows\d3d12bridge.dll"

echo.
echo Source: %SRC%
echo Target: %MC_TARGET%
echo.

cl /EHsc /MD /O2 /DNDEBUG /D_WINDOWS /std:c++17 %JDK_INC% %SRC% /link d3d12.lib dxgi.lib user32.lib d3dcompiler.lib /DLL /OUT:%MC_TARGET%
if %errorlevel% equ 0 (
    echo.
    echo [GL4DX12] Build SUCCESS! d3d12bridge.dll copied to MC versions dir
    echo [GL4DX12] Also copying to local resources...
    copy /Y %MC_TARGET% %LOCAL_TARGET% >nul
    if %errorlevel% equ 0 (
        echo [GL4DX12] Local copy also OK
    )
) else (
    echo.
    echo [GL4DX12] Build FAILED with error %errorlevel%
    echo Check that:
    echo   1. Visual Studio 2022 is installed with C++ workload
    echo   2. JDK at %JDK_HOME% exists
    echo   3. d3d12bridge.cpp exists at %SRC%
)
echo.
pause
