param([string]$PythonPath='D:/Program Files/Python3.13.13/python.exe', [string]$Keystore='')
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath("$PSScriptRoot/..")
$work="$root/.private/apk-analysis/easyplay/sd8227-startup-build"
$java="$root/.tooling/jdk25/bin/java.exe"
$javac="$root/.tooling/jdk25/bin/javac.exe"
$jar="$root/.tooling/jdk25/bin/jar.exe"
$bt="$root/.tooling/build-tools/android-15"
$android="$root/.tooling/platform/android-4.2.2/android.jar"
$jadx="$root/.tooling/jadx/lib/jadx-1.5.6-all.jar"
if (-not $Keystore) { $Keystore="$root/.private/kitkat-probe-debug.jks" }
function Run([string]$stage,[scriptblock]$block) {
    $prev=$ErrorActionPreference; $ErrorActionPreference='Continue'
    try { $lines=& $block 2>&1; $code=$LASTEXITCODE } finally { $ErrorActionPreference=$prev }
    foreach($line in $lines) { Write-Output "$line" }
    if($code -ne 0) { throw "$stage failed: $code" }
}
Push-Location $root
try {
    # Only remove resolved, generated directories belonging to this build.
    $generatedRoot=[IO.Path]::GetFullPath("$PSScriptRoot/build")
    foreach($name in @('classes','dex','tests')) {
        $target=[IO.Path]::GetFullPath("$generatedRoot/$name")
        if(-not $target.StartsWith($generatedRoot+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe generated directory' }
        if(Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target -Recurse -Force }
    }
    New-Item -ItemType Directory -Force "$PSScriptRoot/build/classes","$PSScriptRoot/build/dex","$PSScriptRoot/build/tests" | Out-Null
    Run 'Prepare fixed SD8227 baseline' { & $PythonPath "$PSScriptRoot/startup.py" prepare }
    $sources=@(Get-ChildItem "$PSScriptRoot/src" -Recurse -Filter '*.java' | ForEach-Object FullName)
    Run 'Compile bootstrap exclusively against API17' { & $javac --release 8 -encoding UTF-8 -cp $android -d "$PSScriptRoot/build/classes" @sources }
    Run 'Compile recovery decision tests' { & $javac --release 8 -encoding UTF-8 -cp "$PSScriptRoot/build/classes;$root/.tooling/test-libs/junit.jar" -d "$PSScriptRoot/build/tests" @(Get-ChildItem "$PSScriptRoot/test" -Filter "*.java" | ForEach-Object FullName) }
    Run 'Test failed and interrupted startup recovery' { & $java -cp "$PSScriptRoot/build/tests;$PSScriptRoot/build/classes;$root/.tooling/test-libs/junit.jar;$root/.tooling/test-libs/hamcrest.jar" org.junit.runner.JUnitCore StartupStateTest LegacyDexInstallerTest }
    Run 'Bootstrap jar' { & $jar --create --file "$PSScriptRoot/build/helpers.jar" -C "$PSScriptRoot/build/classes" . }
    Run 'Primary DEX helpers' { & $java -cp "$bt/lib/d8.jar" com.android.tools.r8.D8 --release --min-api 17 --lib $android --output "$PSScriptRoot/build/dex" "$PSScriptRoot/build/helpers.jar" }
    Run 'Disassemble original activity' { & $java -cp $jadx "$PSScriptRoot/tools/DexTool.java" dis "$work/base-classes3.dex" "$work/original" 'Lcom/shilapi/xcertplay/legacy/LegacyActivity;' }
    Run 'Original primary MultiDex' { & $java -cp $jadx "$PSScriptRoot/tools/DexTool.java" dis "$work/base-classes.dex" "$work/original" 'Landroidx/multidex/MultiDex;' }
    Run 'Lifecycle recovery wrapper' { & $PythonPath "$PSScriptRoot/startup.py" patch }
    Run 'Assemble activity overlay' { & $java -cp $jadx "$PSScriptRoot/tools/DexTool.java" asm "$work/modified" "$work/overlay.dex" }
    Run 'Assemble MultiDex adapter call' { & $java -cp $jadx "$PSScriptRoot/tools/DexTool.java" asm "$work/modified-primary" "$work/primary-overlay.dex" }
    Run 'Merge main DEX bootstrap' { & $java -Xmx1g -cp $jadx "$PSScriptRoot/tools/DexTool.java" merge "$work/patched-classes.dex" "$work/base-classes.dex" "$work/primary-overlay.dex" "$PSScriptRoot/build/dex/classes.dex" }
    Run 'Merge activity overlay' { & $java -Xmx1g -cp $jadx "$PSScriptRoot/tools/DexTool.java" merge "$work/patched-classes3.dex" "$work/base-classes3.dex" "$work/overlay.dex" }
    Run 'Audit primary and secondary DEX' { & $java -Xmx1g -cp $jadx "$PSScriptRoot/tools/StartupAudit.java" $work }
    Run 'Repack classic ZIP and launcher manifest' { & $PythonPath "$PSScriptRoot/startup.py" repack }
    Run 'Independent manifest structure regressions' { & $PythonPath "$PSScriptRoot/test/test_manifest.py" }
    Run 'ZIP alignment' { & "$bt/zipalign.exe" -f 4 "$work/unsigned.apk" "$work/aligned.apk" }
    $apk="$root/artifacts/CarConnect-0.1.7-SD8227-multidex-test.apk"
    Run 'Same certificate V1 only signing' { & $java --enable-native-access=ALL-UNNAMED -jar "$bt/lib/apksigner.jar" sign --ks $Keystore --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --min-sdk-version 17 --v1-signing-enabled true --v2-signing-enabled false --v3-signing-enabled false --v4-signing-enabled false --out $apk "$work/aligned.apk" }
    foreach($api in @(17,19,24)) {
        Run "Signature API$api" { & $java --enable-native-access=ALL-UNNAMED -jar "$bt/lib/apksigner.jar" verify --verbose --min-sdk-version $api --max-sdk-version $api $apk }
    }
    $cert=& $java --enable-native-access=ALL-UNNAMED -jar "$bt/lib/apksigner.jar" verify --print-certs $apk
    if ($LASTEXITCODE -ne 0 -or -not ($cert -match '88a0fb9152b0b9bc09dff600d50642dbf17bb3fef0f5692b513c8cd4f4193323')) { throw 'Wrong signer; cannot preserve installed settings' }
    Run 'Final alignment' { & "$bt/zipalign.exe" -c 4 $apk }
    Run 'Payload invariants' { & $PythonPath "$PSScriptRoot/startup.py" audit }
    Run 'Launcher manifest' { & "$bt/aapt.exe" dump xmltree $apk AndroidManifest.xml }
    Write-Output "Output: $apk"
} finally { Pop-Location }
