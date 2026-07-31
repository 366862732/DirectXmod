# ============================================================
# GL4DX12 测试日志收集脚本
# 功能：收集游戏日志并输出到项目文件夹，供 IDE AI 分析
# ============================================================

param(
    [switch]$Watch,        # 实时监控模式（tail -f）
    [switch]$CollectAll,   # 收集所有日志（包括历史）
    [string]$OutputName    # 自定义输出文件名
)

$ErrorActionPreference = "Stop"

# 路径配置
$ProjectRoot = "D:\dx12-lib-template-26.1.2"
$McVersionDir = "D:\.minecraft\versions\26.1.2-Fabric_0.19.3"
$LogsDir = "$McVersionDir\logs"
$CrashReportsDir = "$McVersionDir\crash-reports"
$LatestLog = "$LogsDir\latest.log"
$DebugLog = "$LogsDir\debug.log"

$OutputDir = $ProjectRoot

# 时间戳
$Timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  GL4DX12 测试日志收集工具" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 实时监控模式
if ($Watch) {
    Write-Host "实时监控模式 - 监控日志文件: $LatestLog" -ForegroundColor Yellow
    Write-Host "按 Ctrl+C 停止监控" -ForegroundColor Gray
    Write-Host ""
    
    if (-not (Test-Path $LatestLog)) {
        Write-Host "警告: 日志文件不存在，等待游戏启动..." -ForegroundColor Yellow
    }
    
    # 使用 Get-Content -Wait 实时监控
    Get-Content -Path $LatestLog -Wait -Tail 50 -ErrorAction SilentlyContinue
    
    exit 0
}

# 收集模式
Write-Host "收集模式" -ForegroundColor Yellow
Write-Host ""

# 确定输出文件名
if ($OutputName) {
    $OutputFile = "$OutputDir\$OutputName"
} else {
    $OutputFile = "$OutputDir\游戏日志 - 26.1.2-Fabric_0.19.3.log"
}

# 收集 latest.log
$logContent = @()
$collected = @()

if (Test-Path $LatestLog) {
    $logContent += Get-Content $LatestLog
    $collected += "latest.log ($((Get-Item $LatestLog).Length) bytes)"
}

if ($CollectAll) {
    # 收集 debug.log
    if (Test-Path $DebugLog) {
        $logContent += "`n`n===== DEBUG LOG =====`n"
        $logContent += Get-Content $DebugLog
        $collected += "debug.log ($((Get-Item $DebugLog).Length) bytes)"
    }
    
    # 收集历史日志
    $oldLogs = Get-ChildItem -Path $LogsDir -Filter "*.log.gz" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 5
    foreach ($oldLog in $oldLogs) {
        $logContent += "`n`n===== HISTORICAL LOG: $($oldLog.Name) =====`n"
        # gzip 文件需要特殊处理
        $collected += "$($oldLog.Name) ($($oldLog.Length) bytes)"
    }
}

# 收集崩溃报告
if (Test-Path $CrashReportsDir) {
    $crashes = Get-ChildItem -Path $CrashReportsDir -Filter "*.txt" | Sort-Object LastWriteTime -Descending | Select-Object -First 3
    foreach ($crash in $crashes) {
        $logContent += "`n`n===== CRASH REPORT: $($crash.Name) =====`n"
        $logContent += Get-Content $crash.FullName
        $collected += "crash: $($crash.Name)"
    }
}

# 写入输出文件
if ($logContent.Count -gt 0) {
    $logContent | Out-File -FilePath $OutputFile -Encoding UTF8
    $outputSize = (Get-Item $OutputFile).Length
    
    Write-Host "收集的日志:" -ForegroundColor White
    foreach ($item in $collected) {
        Write-Host "  - $item" -ForegroundColor Gray
    }
    Write-Host ""
    Write-Host "输出文件: $OutputFile" -ForegroundColor Green
    Write-Host "总行数:   $($logContent.Count)" -ForegroundColor Green
    Write-Host "文件大小: $([math]::Round($outputSize/1KB, 1)) KB" -ForegroundColor Green
} else {
    Write-Host "警告: 没有收集到任何日志内容" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  日志收集完成" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan

exit 0
