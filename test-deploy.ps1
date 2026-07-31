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
$DllSource = "$ProjectRoot\rust\target\release\wgpu_mc_jni.dll"
$PdbSource = "$ProjectRoot\rust\target\release\wgpu_mc_jni.pdb"

$McVersionDir = "D:\.minecraft\versions\26.1.2-Fabric_0.19.3"
$ModsDir = "$McVersionDir\mods"
$Dx12ModDir = "$McVersionDir\dx12mod"
$JarTarget = "$ModsDir\gl4dx12-0.1.0.jar"
$DllTarget = "$Dx12ModDir\wgpu_mc_jni.dll"
$PdbTarget = "$Dx12ModDir\wgpu_mc_jni.pdb"

$PclPath = "D:\0000000000-FFFFFFFFF\PCL2_CE_Beta_x64.exe"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  GL4DX12 测试部署工具" -ForegroundColor Cyan
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
if (-not (Test-Path $Dx12ModDir)) {
    New-Item -ItemType Directory -Path $Dx12ModDir -Force | Out-Null
}

# 部署 JAR
if (-not $SkipJar) {
    Write-Host "[1/2] 部署 Java 模组 JAR..." -ForegroundColor Yellow
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
    Write-Host "[1/2] 跳过 JAR 部署" -ForegroundColor Gray
}

Write-Host ""

# 部署 DLL
if (-not $SkipDll) {
    Write-Host "[2/2] 部署原生 DLL..." -ForegroundColor Yellow
    if (Test-Path $DllSource) {
        $srcSize = (Get-Item $DllSource).Length
        Copy-Item -Path $DllSource -Destination $DllTarget -Force
        $dstSize = (Get-Item $DllTarget).Length
        Write-Host "  源文件: $DllSource" -ForegroundColor Gray
        Write-Host "  目标:   $DllTarget" -ForegroundColor Gray
        Write-Host "  大小:   $([math]::Round($srcSize/1KB, 1)) KB -> $([math]::Round($dstSize/1KB, 1)) KB" -ForegroundColor Gray
        Write-Host "  状态:   OK" -ForegroundColor Green
        
        # 同时复制 PDB 调试符号（如果存在）
        if (Test-Path $PdbSource) {
            Copy-Item -Path $PdbSource -Destination $PdbTarget -Force
            Write-Host "  PDB:    已复制调试符号" -ForegroundColor Gray
        }
    } else {
        Write-Host "  警告: 找不到 DLL 文件: $DllSource" -ForegroundColor Red
        Write-Host "  请先执行 Rust 构建: cd rust && cargo build --release" -ForegroundColor Yellow
    }
} else {
    Write-Host "[2/2] 跳过 DLL 部署" -ForegroundColor Gray
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
