@echo off 
set JAVA_HOME=C:\Program Files\BellSoft\LibericaJDK-25-Full 
cl /nologo /EHsc /O2 /MD /LD /Fe:gl4dx12.dll /I"%C:\Program Files\BellSoft\LibericaJDK-25-Full%\include" /I"%C:\Program Files\BellSoft\LibericaJDK-25-Full%\include\win32" d3d12_simple.cpp /link d3d12.lib 
if %0% equ 0 ( 
    echo [SUCCESS] DLL compiled 
    copy gl4dx12.dll ..\..\resources\native\windows\ /Y 
) else ( 
    echo [FAILED] Compilation failed 
) 
