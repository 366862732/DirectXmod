@echo off
REM 部署新的 dx12_mc.dll 和 gl4dx12 JAR 到游戏目录
copy /Y "native\build\bin\Release\dx12_mc.dll" ".minecraft\versions\26.2-Fabric_0.19.3\dx12mod\dx12_mc.dll"
copy /Y "fabric\build\libs\gl4dx12-0.1.0.jar" ".minecraft\versions\26.2-Fabric_0.19.3\mods\gl4dx12-0.1.0.jar"
echo Done. New DLL and JAR deployed.
