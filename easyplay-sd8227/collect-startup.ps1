param([string]$AdbPath='adb', [string]$Serial='', [string]$OutputPath='')
# Collect only: no uninstall, app data deletion, root or device configuration changes.
$ErrorActionPreference='Stop'
if (-not $OutputPath) { $OutputPath=Join-Path (Get-Location) ('CarConnect-startup-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.txt') }
$prefix=@(); if ($Serial) { $prefix=@('-s',$Serial) }
$report=[Collections.Generic.List[string]]::new()
function Capture([string]$label,[string[]]$arguments) {
    $report.Add("`r`n[$label]")
    $previous=$ErrorActionPreference; $ErrorActionPreference='Continue'
    try { $lines=& $AdbPath @prefix @arguments 2>&1; $code=$LASTEXITCODE } finally { $ErrorActionPreference=$previous }
    foreach($line in $lines) { $report.Add("$line"); Write-Host "$line" }
    $report.Add("exitCode=$code"); return $code
}
if ((Capture 'Selected device' @('get-state')) -ne 0) { throw 'ADB device unavailable; check authorization or supply -Serial.' }
foreach($property in @('ro.product.model','ro.build.version.release','ro.build.version.sdk','ro.product.cpu.abi','ro.product.cpu.abi2','ro.product.board')) {
    $null=Capture $property @('shell','getprop',$property)
}
$null=Capture 'Package and activity resolver' @('shell','dumpsys','package','com.shihab.diplay.legacy')
$null=Capture 'Stored report (run-as may be restricted by firmware)' @('shell','run-as','com.shihab.diplay.legacy','cat','files/carconnect-startup.txt')
$null=Capture 'Startup and crash logcat' @('logcat','-d','-v','time','AndroidRuntime:V','CarConnect-Startup:V','MultiDex:V','dalvikvm:W','libc:W','DEBUG:E','ActivityManager:I','*:S')
[IO.File]::WriteAllLines([IO.Path]::GetFullPath($OutputPath),$report,[Text.UTF8Encoding]::new($false))
Write-Host "Saved: $OutputPath. Review personal information before sending; keep device logs out of the public repository."
