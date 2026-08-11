Add-Type -AssemblyName System.IO.Compression.FileSystem

$dllPath = "d:\dx12-lib-template-26.1.2\native\build\bin\Release\dx12_mc.dll"
$jarPath = "d:\dx12-lib-template-26.1.2\fabric\build\libs\gl4dx12-0.1.0.jar"
$tmpDir = "d:\dx12-lib-template-26.1.2\_jar_tmp"

if (Test-Path $tmpDir) { Remove-Item -Recurse -Force $tmpDir }
New-Item -ItemType Directory -Path $tmpDir -Force | Out-Null

# Extract
[System.IO.Compression.ZipFile]::ExtractToDirectory($jarPath, $tmpDir)

# Replace DLL
Copy-Item -Force $dllPath "$tmpDir\dx12_mc.dll"

# Remove old jar to recreate
Remove-Item -Force $jarPath -ErrorAction SilentlyContinue

# Recreate JAR as ZIP
$dstJar = [System.IO.Compression.ZipFile]::Open($jarPath, [System.IO.Compression.ZipFileMode]::Create)
foreach ($file in Get-ChildItem $tmpDir -Recurse -File) {
    $relPath = $file.FullName.Substring($tmpDir.Length).TrimStart('\').Replace('\', '/')
    $entry = $dstJar.CreateEntry($relPath, [System.IO.Compression.CompressionLevel]::Optimal)
    $entry.LastWriteTime = $file.LastWriteTime
    $stream = $entry.Open()
    $fileStream = $file.OpenRead()
    $fileStream.CopyTo($stream)
    $fileStream.Close()
    $stream.Close()
}
$dstJar.Dispose()
Remove-Item -Recurse -Force $tmpDir

Write-Host "Done. JAR: $($jarPath) size: $((Get-Item $jarPath).Length)"
