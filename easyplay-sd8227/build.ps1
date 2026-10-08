param(
    [string]$PythonPath = 'D:\Program Files\Python3.13.13\python.exe',
    [string]$ToolingRoot = '',
    [string]$BaseApk = '',
    [string]$Keystore = ''
)
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath("$PSScriptRoot/..")
if (-not $ToolingRoot) { $ToolingRoot = "$root/.tooling" }
if (-not $BaseApk) { $BaseApk = "$root/artifacts/CarConnect-0.1.4-beta-OEM-test-Android4.2.apk" }
if (-not $Keystore) { $Keystore = "$root/.private/kitkat-probe-debug.jks" }
$java = "$ToolingRoot/jdk25/bin/java.exe"
$bt = "$ToolingRoot/build-tools/android-15"
$work = "$root/.private/apk-analysis/easyplay/sd8227"
$output = "$root/artifacts/CarConnect-0.1.4-SD8227-V1-test.apk"
$signerDigest = '88a0fb9152b0b9bc09dff600d50642dbf17bb3fef0f5692b513c8cd4f4193323'
New-Item -ItemType Directory -Force $work, "$root/artifacts" | Out-Null

function Run([string]$stage, [scriptblock]$block) {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { $lines = & $block 2>&1; $code = $LASTEXITCODE }
    finally { $ErrorActionPreference = $previous }
    foreach ($line in $lines) { Write-Host "$line" }
    if ($code -ne 0) { throw "$stage failed: $code" }
}

function VerifySigner([string]$apk) {
    $lines = & $java --enable-native-access=ALL-UNNAMED -jar "$bt/lib/apksigner.jar" verify --print-certs $apk
    if ($LASTEXITCODE -ne 0 -or -not ($lines -match [regex]::Escape($signerDigest))) {
        throw 'APK signer differs from the published CarConnect baseline'
    }
}

VerifySigner $BaseApk
Run 'Classic ZIP repack' { & $PythonPath "$PSScriptRoot/profile.py" repack $BaseApk "$work/unsigned.apk" }
Run 'Alignment' { & "$bt/zipalign.exe" -f 4 "$work/unsigned.apk" "$work/aligned.apk" }
# Explicitly disable every newer scheme. Do not merely delete the signature
# block: its anti-stripping declaration would remain in the V1 signature.
Run 'V1-only signing' {
    & $java --enable-native-access=ALL-UNNAMED -jar "$bt/lib/apksigner.jar" sign --ks $Keystore --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --min-sdk-version 17 --v1-signing-enabled true --v2-signing-enabled false --v3-signing-enabled false --v4-signing-enabled false --out $output "$work/aligned.apk"
}
foreach ($api in @(17, 19, 24)) {
    Run "API $api signature verification" {
        & $java --enable-native-access=ALL-UNNAMED -jar "$bt/lib/apksigner.jar" verify --verbose --min-sdk-version $api --max-sdk-version $api $output
    }
}
VerifySigner $output
Run 'Final alignment verification' { & "$bt/zipalign.exe" -c 4 $output }
Run 'Payload, ABI, DEX, ZIP and signing-block audit' { & $PythonPath "$PSScriptRoot/profile.py" audit $BaseApk $output }
Write-Host "SD8227 V1-only comparison APK: $output"
