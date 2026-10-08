param(
    [Parameter(Mandatory = $true)][string]$ApkPath,
    [string]$AdbPath = 'adb',
    [string]$Serial = '',
    [string]$OutputPath = ''
)
# Run on the owner's PC after enabling USB debugging on the head unit.
# Installs with -r: no uninstall, data wipe, root, system changes or log clearing.
$ErrorActionPreference = 'Stop'
$ApkPath = (Resolve-Path -LiteralPath $ApkPath).Path
if (-not $OutputPath) {
    $OutputPath = Join-Path (Get-Location) ("CarConnect-install-" + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.txt')
}
$prefix = @()
if ($Serial) { $prefix = @('-s', $Serial) }
$report = [Collections.Generic.List[string]]::new()
function Capture([string]$label, [string[]]$arguments) {
    $report.Add("`r`n[$label]")
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { $lines = & $AdbPath @prefix @arguments 2>&1; $code = $LASTEXITCODE }
    finally { $ErrorActionPreference = $previous }
    foreach ($line in $lines) { $report.Add("$line"); Write-Host "$line" }
    $report.Add("exitCode=$code")
    return $code
}
$report.Add('APK=' + [IO.Path]::GetFileName($ApkPath))
$report.Add('Bytes=' + (Get-Item -LiteralPath $ApkPath).Length)
$report.Add('SHA256=' + (Get-FileHash -LiteralPath $ApkPath -Algorithm SHA256).Hash.ToLowerInvariant())
$connected = Capture 'Device state' @('get-state')
if ($connected -ne 0) { throw 'ADB cannot select a device. Check the connection or supply -Serial.' }
foreach ($property in @('ro.product.model', 'ro.product.board', 'ro.build.version.release', 'ro.build.version.sdk', 'ro.product.cpu.abi', 'ro.product.cpu.abi2', 'ro.product.cpu.abilist')) {
    $null = Capture $property @('shell', 'getprop', $property)
}
$null = Capture 'Kernel' @('shell', 'uname', '-r')
$installCode = Capture 'Install (preserve app data)' @('install', '-r', $ApkPath)
$null = Capture 'Installer logs' @('logcat', '-d', '-v', 'time', 'PackageManager:V', 'PackageInstaller:V', 'installd:V', '*:S')
[IO.File]::WriteAllLines([IO.Path]::GetFullPath($OutputPath), $report, [Text.UTF8Encoding]::new($false))
Write-Host "Saved: $OutputPath; install exit code: $installCode"
Write-Host 'Review the log before sharing it; it can contain other installed package names. Do not upload it to the public repository.'
