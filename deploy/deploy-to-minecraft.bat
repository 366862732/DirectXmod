@echo off
chcp 65001 >nul
REM 一键部署 dx12_mc.dll 和 gl4dx12 jar 到 .minecraft（双击运行）
set MC=D:\.minecraft\versions\26.2-Fabric_0.19.3
set SRC=%~dp0..

echo [1/2] 复制 dx12_mc.dll -> %MC%\dx12mod\
copy /Y "%SRC%\deploy\dx12_mc.dll" "%MC%\dx12mod\dx12_mc.dll" || goto :fail

echo [2/2] 复制 gl4dx12-0.1.0.jar -> %MC%\mods\
copy /Y "%SRC%\fabric\build\libs\gl4dx12-0.1.0.jar" "%MC%\mods\gl4dx12-0.1.0.jar" || goto :fail

echo.
echo 部署完成，可以启动游戏了。
pause
exit /b 0

:fail
echo.
echo 部署失败！请检查路径或关闭 Minecraft 后重试。
pause
exit /b 1
