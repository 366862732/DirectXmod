@echo off
set JAVA_HOME=C:\Program Files\Zulu\zulu-26
set PATH=%JAVA_HOME%\bin;%PATH%

echo Compiling GL4DX12...

REM 确保 libs 目录下有 JNA jar
if not exist libs\jna-5.14.0.jar (
    echo Downloading JNA...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/net/java/dev/jna/jna/5.14.0/jna-5.14.0.jar' -OutFile 'libs/jna-5.14.0.jar'"
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/net/java/dev/jna/jna-platform/5.14.0/jna-platform-5.14.0.jar' -OutFile 'libs/jna-platform-5.14.0.jar'"
)

javac -cp "libs/jna-5.14.0.jar;libs/jna-platform-5.14.0.jar;libs/fabric-loader-0.16.10.jar" -d build/classes -encoding UTF-8 src/main/java/com/dx12/*.java
if %errorlevel% neq 0 exit /b %errorlevel%

echo Compilation successful!
jar cvf build/libs/gl4dx12-1.0.0.jar -C build/classes . -C src/main/resources .
copy build\libs\gl4dx12-1.0.0.jar D:\.minecraft\mods\
echo Deployed!