# ============================================================
# GL4DX12 模组测试部署脚本
# 功能：将编译产物部署到 PCLCE 测试实例
# ============================================================

param(
    [switch]$SkipJar,      # 跳过 JAR 部署
    [switch]$SkipDll,      # 跳过 DLL 部署
    [switch]$LaunchPCL     # 部署完成后启动 PCLCE
)

$ErrorActionPreference = "Stop"

# 路径配置
$ProjectRoot = "D:\dx12-lib-template-26.1.2"
$JarSource = "$ProjectRoot\fabric\build\libs\gl4dx12-0.1.0.jar"
$DllSource = "$ProjectRoot\native\build\bin\Release\dx12_mc.dll"

$McVersionDir = "D:\.minecraft\versions\26.1.2-Fabric_0.19.3"
$ModsDir = "$McVersionDir\mods"
$JarTarget = "$ModsDir\gl4dx12-0.1.0.jar"

$PclPath = "D:\0000000000-FFFFFFFFF\PCL2_CE_Beta_x64.exe"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  GL4DX12 测试部署工具 (C++/D3D12)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查目标目录
if (-not (Test-Path $McVersionDir)) {
    Write-Error "错误：找不到测试实例目录: $McVersionDir"
    exit 1
}

# 确保目标目录存在
if (-not (Test-Path $ModsDir)) {
    New-Item -ItemType Directory -Path $ModsDir -Force | Out-Null
}

# 部署 JAR
if (-not $SkipJar) {
    Write-Host "[1/1] 部署 Java 模组 JAR..." -ForegroundColor Yellow
    if (Test-Path $JarSource) {
        $srcSize = (Get-Item $JarSource).Length
        Copy-Item -Path $JarSource -Destination $JarTarget -Force
        $dstSize = (Get-Item $JarTarget).Length
        Write-Host "  源文件: $JarSource" -ForegroundColor Gray
        Write-Host "  目标:   $JarTarget" -ForegroundColor Gray
        Write-Host "  大小:   $([math]::Round($srcSize/1KB, 1)) KB -> $([math]::Round($dstSize/1KB, 1)) KB" -ForegroundColor Gray
        Write-Host "  状态:   OK" -ForegroundColor Green
    } else {
        Write-Host "  警告: 找不到 JAR 文件: $JarSource" -ForegroundColor Red
        Write-Host "  请先执行 Gradle 构建: cd fabric && .\gradlew.bat build" -ForegroundColor Yellow
    }
} else {
    Write-Host "[1/1] 跳过 JAR 部署" -ForegroundColor Gray
}

# 部署 DLL（DX12 native library）
if (-not $SkipDll) {
    Write-Host "[DLL] 部署原生 DLL..." -ForegroundColor Yellow
    if (Test-Path $DllSource) {
        $srcSize = (Get-Item $DllSource).Length
        $dllTarget = "$ModsDir\dx12_mc.dll"
        Copy-Item -Path $DllSource -Destination $dllTarget -Force
        $dstSize = (Get-Item $dllTarget).Length
        Write-Host "  源文件: $DllSource" -ForegroundColor Gray
        Write-Host "  目标:   $dllTarget" -ForegroundColor Gray
        Write-Host "  大小:   $([math]::Round($srcSize/1KB, 1)) KB -> $([math]::Round($dstSize/1KB, 1)) KB" -ForegroundColor Gray
        Write-Host "  状态:   OK" -ForegroundColor Green
    } else {
        Write-Host "  警告: 找不到 DLL 文件: $DllSource" -ForegroundColor Red
        Write-Host "  请先执行 CMake 构建: cmake --build native/build --config Release" -ForegroundColor Yellow
    }
} else {
    Write-Host "[DLL] 跳过 DLL 部署" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  部署完成" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "测试实例: $McVersionDir" -ForegroundColor White
Write-Host "启动器:   PCLCE" -ForegroundColor White
Write-Host ""

# 启动 PCLCE
if ($LaunchPCL) {
    Write-Host "正在启动 PCLCE..." -ForegroundColor Yellow
    Start-Process -FilePath $PclPath
    Write-Host "PCLCE 已启动，请手动选择版本并点击启动" -ForegroundColor Green
}

exit 0
