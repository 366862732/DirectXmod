# ============================================================
# GL4DX12 deployment verification script
# Checks the game log against all verification points after deploy.
# Usage: powershell -ExecutionPolicy Bypass -File verify-deploy.ps1
#    Optional: -LogPath "D:\.minecraft\logs\latest.log"
# ============================================================

param(
    [string]$LogPath = ""
)

# Auto-detect the newest game log when no path is given:
# 1) .minecraft latest.log  2) any *.log under the project root.
if (-not $LogPath) {
    $latest = "D:\.minecraft\logs\latest.log"
    if (Test-Path $latest) {
        $LogPath = $latest
    } else {
        $alt = Get-ChildItem "D:\dx12-lib-template-26.1.2" -Filter *.log -File |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($alt) { $LogPath = $alt.FullName }
    }
}

if (-not (Test-Path $LogPath)) {
    Write-Host "ERROR: log file not found. Pass one explicitly:" -ForegroundColor Red
    Write-Host "  powershell -ExecutionPolicy Bypass -File verify-deploy.ps1 -LogPath <path>" -ForegroundColor Yellow
    exit 1
}

$log = Get-Content $LogPath -Raw

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  GL4DX12 deployment verification" -ForegroundColor Cyan
Write-Host "  Log: $LogPath" -ForegroundColor Gray
Write-Host "  Time: $((Get-Item $LogPath).LastWriteTime)" -ForegroundColor Gray
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$passCount = 0
$failCount = 0

function Check {
    param(
        [string]$Name,
        [string]$Pattern,
        [bool]$ShouldExist = $true
    )
    $found = $log -match $Pattern
    if ($found -eq $ShouldExist) {
        $script:passCount++
        Write-Host "[PASS] $Name" -ForegroundColor Green
    } else {
        $script:failCount++
        if ($ShouldExist) {
            Write-Host "[FAIL] $Name (missing: $Pattern)" -ForegroundColor Red
        } else {
            Write-Host "[FAIL] $Name (unexpected: $Pattern)" -ForegroundColor Red
        }
    }
}

# 1. DLL build identity (git commit hash injected by build.rs)
Check "DLL version (wgpu-mc build cf40646)" "wgpu-mc build cf40646" $true

# 2. Frame-rate unlock (Phase 11h)
Check "Per-frame present (present moved to render TAIL)" "present moved to render TAIL" $true

# 3. Quantified FPS evidence
Check "FPS stats (D3D12 present FPS)" "D3D12 present FPS:" $true

# 4. Sky color fix (must NOT appear)
Check "Sky color fix (no getSkyColor fallback)" "getSkyColor fallback" $false

# 5. Chunk batching (Phase 11e)
Check "Chunk batching (Chunk batch rebuilt)" "Chunk batch rebuilt" $true

# 6. Incremental chunk merge (Phase 11j — stutter fix code path)
Check "Incremental chunk upload (incremental)" "\(incremental\)" $true

# 7. Batching enabled flag
Check "Batching enabled (batching=true)" "batching=true" $true

# 8. Hand/held item visible (GL renderLevel skipped, renderItemInHand kept)
Check "GL world render skipped (hand mixin injected)" "GL world render skipped" $true

# 9. Particle upload chain (Java ParticleGroup extraction + Rust billboard)
Check "Particles uploaded (chain alive)" "Particles uploaded:" $true

# 10. Renderer ready
Check "Renderer ready (Device created OK)" "Device created OK" $true

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
if ($failCount -eq 0) {
    Write-Host "  RESULT: $passCount PASS / $failCount FAIL  (ALL GOOD)" -ForegroundColor Green
} else {
    Write-Host "  RESULT: $passCount PASS / $failCount FAIL" -ForegroundColor Red
}
Write-Host "========================================" -ForegroundColor Cyan

if ($failCount -gt 0) {
    Write-Host ""
    Write-Host "NOTE: if DLL version / FPS checks fail, make sure the deployed"
    Write-Host "      JAR is the 17:14 build (fabric\build\libs\gl4dx12-0.1.0.jar)"
    Write-Host "      and the game was fully restarted." -ForegroundColor Yellow
}

exit $failCount
