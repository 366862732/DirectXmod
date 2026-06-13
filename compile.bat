@echo off
echo Setting up Java 25...
set JAVA_HOME=C:\Program Files\BellSoft\LibericaJDK-25-Full
set PATH=%JAVA_HOME%\bin;%PATH%

echo Java version:
java -version

echo.
echo Starting compilation...
echo.

set CP=libs/fabric-loader-0.19.2.jar;D:\.minecraft\versions\26.1.2\26.1.2.jar

javac -cp "%CP%" ^
    -d build/classes ^
    -sourcepath src/main/java ^
    src/main/java/com/dx12/Dx12Mod.java ^
    src/main/java/com/dx12/DX12LibClient.java ^
    src/main/java/com/dx12/NativeUtils.java ^
    src/main/java/com/dx12/client/com.dx12.D3D12Bridge.java

if %errorlevel% equ 0 (
    echo.
    echo [SUCCESS] Compilation successful!
    echo Copying resources...
    xcopy /E /Y src\main\resources\* build\classes\
    
    echo Creating JAR...
    cd build\classes
    jar cf ..\gl4dx12-1.0.0.jar *
    cd ..\..

    echo.
    echo [SUCCESS] JAR created: build\gl4dx12-1.0.0.jar
    dir build\gl4dx12-1.0.0.jar
) else (
    echo.
    echo [FAILED] Compilation failed with error code %errorlevel%
    echo Check the errors above.
)

echo.
echo Press any key to close this window...
pause > nul