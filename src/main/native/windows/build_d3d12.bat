@echo off
call "D:\Visual studio\VC\Auxiliary\Build\vcvars64.bat" >nul 2>&1
set JAVA_HOME=C:\Program Files\Java\jdk-26-openjdk\jdk-26
cl /nologo /EHsc /O2 /MD /LD /Fe:gl4dx12.dll /I"%JAVA_HOME%\include" /I"%JAVA_HOME%\include\win32" d3d12bridge.cpp /link d3d12.lib dxgi.lib user32.lib /OUT:gl4dx12.dll
if %errorlevel% equ 0 (
    echo [SUCCESS] DLL compiled successfully!
    copy /Y gl4dx12.dll ..\..\resources\native\windows\
    echo [SUCCESS] DLL copied to resources
) else (
    echo [FAILED] Compilation failed with error %errorlevel%
)
