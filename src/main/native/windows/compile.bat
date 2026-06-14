@echo off
call "D:\Visual studio\VC\Auxiliary\Build\vcvars64.bat"

cl /nologo /LD /EHsc /O2 /Fe:gl4dx12.dll d3d12bridge.cpp ^
   /I "C:\Program Files\Amazon Corretto\jdk25.0.3_9\include" ^
   /I "C:\Program Files\Amazon Corretto\jdk25.0.3_9\include\win32" ^
   d3d12.lib dxgi.lib d3dcompiler.lib user32.lib gdi32.lib

if %errorlevel% == 0 (
    echo [SUCCESS] gl4dx12.dll built
    copy /Y gl4dx12.dll ..\..\resources\native\windows\
) else (
    echo [FAILED] Compilation failed
)