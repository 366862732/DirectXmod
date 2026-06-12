@echo off
set PATH=D:\mingw64\bin;%PATH%
set JAVA_HOME=C:\Program Files\BellSoft\LibericaJDK-25-Full
g++ -shared -o gl4dx12.dll -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32" d3d12bridge_simple.cpp -static-libgcc -static-libstdc++ -Wl,--kill-at
if %errorlevel% equ 0 (
    echo [SUCCESS] DLL compiled
    copy gl4dx12.dll ..\..\resources\native\windows\
) else (
    echo [FAILED] Compilation failed
)