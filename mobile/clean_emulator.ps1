Get-Process | Where-Object { $_.ProcessName -like "*qemu*" -or $_.ProcessName -like "*emulator*" } | ForEach-Object {
    Write-Host "Killing: $($_.ProcessName) ($($_.Id))"
    Stop-Process -Id $_.Id -Force
}

$avdPath = "$env:USERPROFILE\.android\avd"
if (Test-Path $avdPath) {
    Get-ChildItem -Path $avdPath -Recurse -Filter "*.lock" -Force -ErrorAction SilentlyContinue | ForEach-Object {
        Write-Host "Removing lock: $($_.FullName)"
        Remove-Item -Path $_.FullName -Force -Recurse -ErrorAction SilentlyContinue
    }
}

Write-Host "=== Current Disk Free Space ==="
Get-PSDrive C | Select-Object Name, Used, Free

Write-Host "=== Large Temp / Cache Folders ==="
$paths = @(
    "$env:LOCALAPPDATA\Temp",
    "$env:USERPROFILE\.gradle\caches",
    "$env:LOCALAPPDATA\Android\Sdk\.downloadIntermediates",
    "$env:USERPROFILE\.android\avd"
)

foreach ($p in $paths) {
    if (Test-Path $p) {
        $size = (Get-ChildItem $p -Recurse -Force -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum / 1GB
        Write-Host "$p : $([math]::Round($size, 2)) GB"
    }
}
