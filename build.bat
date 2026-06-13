@echo off
echo [AI-D3D12] ???? D3D12 Bridge DLL...
set VCVARS="C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"
if not exist %VCVARS% (
    echo [AI-D3D12] ??? VS 2022??? VS 2026...
    set VCVARS="C:\Program Files\Microsoft Visual Studio\2026\Community\VC\Auxiliary\Build\vcvars64.bat"
    if not exist %VCVARS% (
        echo [AI-D3D12] ??: Visual Studio ???
        pause
        exit /b 1
    )
)
call %VCVARS%
echo [AI-D3D12] ?? d3d12bridge.cpp...
cl /EHsc /MD /O2 /DNDEBUG /D_WINDOWS d3d12bridge.cpp /link d3d12.lib dxgi.lib user32.lib d3dcompiler.lib /DLL /OUT:gl4dx12.dll
if %errorlevel% equ 0 (
    echo [AI-D3D12] ???? gl4dx12.dll
    echo [AI-D3D12] ????: rundll32.exe gl4dx12.dll,nativeRender
) else (
    echo [AI-D3D12] ????
)
pause
