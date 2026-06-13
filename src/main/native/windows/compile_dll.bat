@echo off
echo ============================================================
echo Compiling REAL D3D12 rendering DLL from d3d12bridge.cpp
echo ============================================================

cd /d "%~dp0"

:: Set up MSVC environment (must run from VS Developer Command Prompt or vcvars)
set PATH=D:\Visual studio\VC\Tools\MSVC\14.51.36231\bin\Hostx64\x64;D:\Windows Kits\10\bin\10.0.28000.0\x64;%PATH%
set INCLUDE=D:\Visual studio\VC\Tools\MSVC\14.51.36231\include;D:\Windows Kits\10\include\10.0.28000.0\ucrt;D:\Windows Kits\10\include\10.0.28000.0\um;D:\Windows Kits\10\include\10.0.28000.0\shared;D:\Windows Kits\10\include\10.0.28000.0\winrt
set LIB=D:\Visual studio\VC\Tools\MSVC\14.51.36231\lib\x64;D:\Windows Kits\10\lib\10.0.28000.0\um\x64;D:\Windows Kits\10\lib\10.0.28000.0\ucrt\x64
set JAVA_HOME=C:\Program Files\BellSoft\LibericaJDK-25-Full

cl /nologo /EHsc /O2 /MD /LD ^
  /Fe:gl4dx12.dll ^
  /I"%JAVA_HOME%\include" ^
  /I"%JAVA_HOME%\include\win32" ^
  d3d12bridge.cpp ^
  /link d3d12.lib dxgi.lib d3dcompiler.lib user32.lib gdi32.lib /OUT:gl4dx12.dll

if %errorlevel% equ 0 (
    echo.
    echo [SUCCESS] Real D3D12 rendering DLL compiled!
    copy /Y gl4dx12.dll ..\..\resources\native\windows\
    echo [SUCCESS] DLL copied to resources.
    dumpbin /exports gl4dx12.dll | findstr Java
) else (
    echo.
    echo [FAILED] Compilation failed!
)

echo.
pause