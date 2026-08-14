@echo off
REM P17 部署脚本：部署新 JAR + DLL 到测试实例
set JAR_SRC=d:\dx12-lib-template-26.1.2\fabric\build\libs\gl4dx12-0.1.0.jar
set JAR_DST=D:\.minecraft\versions\26.2-Fabric_0.19.3\mods\gl4dx12-0.1.0.jar
set DLL_DST=D:\.minecraft\versions\26.2-Fabric_0.19.3\dx12mod\dx12_mc.dll

echo [P17 Deploy] Copying JAR...
copy /Y "%JAR_SRC%" "%JAR_DST%"
if %errorlevel% neq 0 ( echo JAR copy FAILED; pause; exit /b 1 )
echo [P17 Deploy] JAR copied OK

echo [P17 Deploy] Copying DLL...
copy /Y "d:\dx12-lib-template-26.1.2\native\build\bin\Release\dx12_mc.dll" "%DLL_DST%"
if %errorlevel% neq 0 ( echo DLL copy FAILED; pause; exit /b 1 )
echo [P17 Deploy] DLL copied OK

echo.
echo [P17 Deploy] Done! Please restart Minecraft 26.2-Fabric_0.19.3.
echo [P17 Deploy] Key logs to check:
echo   1. [dx12-java] drawIndexed pipeline=... count=N  (stderr → 游戏日志)
echo   2. [dx12] drawIndexed[...]: count=N ... (dx12-native.log)
echo   3. blitSurface: ... wasWritten=0/1
echo.
pause
