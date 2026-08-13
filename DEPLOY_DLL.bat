@echo off
REM 部署新的 dx12_mc.dll 到游戏目录
copy /Y "native\build\bin\Release\dx12_mc.dll" ".minecraft\versions\26.2-Fabric_0.19.3\dx12mod\dx12_mc.dll"
echo Done. New DLL deployed.
