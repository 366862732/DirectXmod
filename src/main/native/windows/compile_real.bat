@echo off
echo ============================================================
echo Compiling REAL D3D12 rendering DLL from d3d12bridge.cpp
echo ============================================================

set MSVC_BIN=D:\Visual studio\VC\Tools\MSVC\14.51.36231\bin\Hostx64\x64
set MSVC_INC=D:\Visual studio\VC\Tools\MSVC\14.51.36231\include
set MSVC_LIB=D:\Visual studio\VC\Tools\MSVC\14.51.36231\lib\x64
set SDK_BIN=D:\Windows Kits\10\bin\10.0.28000.0\x64
set SDK_INC=D:\Windows Kits\10\include\10.0.28000.0
set SDK_LIB=D:\Windows Kits\10\lib\10.0.28000.0
set JAVA_HOME=C:\Program Files\BellSoft\LibericaJDK-25-Full

set PATH=%MSVC_BIN%;%SDK_BIN%;%PATH%
set INCLUDE=%MSVC_INC%;%SDK_INC%\ucrt;%SDK_INC%\um;%SDK_INC%\shared
set LIB=%MSVC_LIB%;%SDK_LIB%\um\x64;%SDK_LIB%\ucrt\x64

echo MSVC: %MSVC_BIN%
echo SDK:  %SDK_BIN%
echo JDK:  %JAVA_HOME%

"%MSVC_BIN%\cl.exe" /nologo /EHsc /O2 /MD /LD ^
  /Fe:gl4dx12.dll ^
  /I"%JAVA_HOME%\include" ^
  /I"%JAVA_HOME%\include\win32" ^
  d3d12bridge.cpp ^
  /link d3d12.lib dxgi.lib d3dcompiler.lib user32.lib /OUT:gl4dx12.dll

if %errorlevel% equ 0 (
    echo.
    echo [SUCCESS] Real D3D12 DLL compiled successfully!
    copy /Y gl4dx12.dll ..\..\resources\native\windows\
    echo [SUCCESS] DLL copied to resources folder
) else (
    echo.
    echo [FAILED] Compilation failed with error %errorlevel%
)

echo.
echo ============================================================
pause
