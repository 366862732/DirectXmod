@echo off
echo Generating JNI headers...
cd /d %~dp0
.\gradlew build -x test
javac -h src/main/native/windows -cp build/classes/java/main build/classes/java/main/com/dx12/client/DX12LibClient.class
echo Headers generated in src/main/native/windows/
pause
