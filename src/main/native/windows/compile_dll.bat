@echo off
echo Compiling gl4dx12.dll...

cd /d D:\dx12-lib-template-26.1.2\src\main\native\windows

cl /nologo /EHsc /O2 /MD /LD /Fe:gl4dx12.dll /I"C:\Program Files\BellSoft\LibericaJDK-25-Full\include" /I"C:\Program Files\BellSoft\LibericaJDK-25-Full\include\win32" gl4dx12.cpp /link d3d12.lib dxgi.lib user32.lib

if %errorlevel% equ 0 (
    echo Compilation successful!
    copy /Y gl4dx12.dll ..\..\resources\native\windows\
    echo DLL copied to resources.
    dumpbin /exports gl4dx12.dll | findstr Java
) else (
    echo Compilation failed!
)

pause