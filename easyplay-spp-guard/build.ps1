param([string]$PythonPath='D:/Program Files/Python3.13.13/python.exe')
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath("$PSScriptRoot/..")
$work="$root/.private/apk-analysis/easyplay/oem-spp-guard"
$java="$root/.tooling/jdk25/bin/java.exe"
$javac="$root/.tooling/jdk25/bin/javac.exe"
$jar="$root/.tooling/jdk25/bin/jar.exe"
$android="$root/.tooling/platform/android-4.2.2/android.jar"
$bt="$root/.tooling/build-tools/android-15"
$jadx="$root/.tooling/jadx/lib/jadx-1.5.6-all.jar"
function Run([string]$stage,[scriptblock]$block) {
    $prev=$ErrorActionPreference; $ErrorActionPreference='Continue'
    try { $lines=& $block 2>&1; $code=$LASTEXITCODE } finally { $ErrorActionPreference=$prev }
    foreach($line in $lines) { Write-Output "$line" }
    if($code -ne 0) { throw "$stage failed: $code" }
}
Push-Location $root
try {
    Run 'Verify released BC03 baseline' { & $PythonPath "$PSScriptRoot/patch_apk.py" prepare }
    foreach($name in @('original','modified','build')) {
        $target=[IO.Path]::GetFullPath("$work/$name")
        if([IO.Path]::GetDirectoryName($target) -ne [IO.Path]::GetFullPath($work) -or
            -not $target.StartsWith($root+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe generated path' }
        if(Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target -Recurse -Force }
    }
    New-Item -ItemType Directory -Force "$work/build/stubs","$work/build/classes","$work/build/tests","$work/build/dex" | Out-Null
    $selected=@('Lcom/shilapi/xcertplay/patch/EnhancementPanel;','Lcom/shilapi/xcertplay/patch/CarConnectInfo;',
        'Lcom/shilapi/xcertplay/legacy/LegacyConnectionScreen;','Lcom/shilapi/xcertplay/legacy/LegacySessionService;')
    Run 'Disassemble UI version labels' { & $java -cp $jadx "$root/easyplay-oem4/tools/DexTool.java" dis "$work/base.dex" "$work/original" @selected }
    $stubs=@(Get-ChildItem "$PSScriptRoot/stubs" -Recurse -Filter '*.java' | ForEach-Object FullName)
    $stubs+=@("$root/easyplay-oem4/stubs/com/shilapi/xcertplay/transport/BluetoothRfcommDuplexStream.java",
        "$root/easyplay-oem4/src/com/shilapi/xcertplay/patch/OemProtocol.java")
    Run 'Compile API17 compile-only dependencies' { & $javac --release 8 -Xlint:-options -encoding UTF-8 -cp $android -d "$work/build/stubs" @stubs }
    $sources=@(Get-ChildItem "$PSScriptRoot/src" -Recurse -Filter '*.java' | ForEach-Object FullName)
    Run 'Compile new transport and bounded gate' { & $javac --release 8 -Xlint:-options -encoding UTF-8 -cp "$work/build/stubs;$android" -d "$work/build/classes" @sources }
    New-Item -ItemType Directory -Force "$work/build/test-stubs" | Out-Null
    $fakes=@(Get-ChildItem "$PSScriptRoot/test-stubs" -Recurse -Filter '*.java' | ForEach-Object FullName)
    $fakes+=@("$root/easyplay-oem4/test-stubs/android/content/Context.java")
    Run 'Compile JVM lifecycle, timer and Service fakes' { & $javac --release 8 -Xlint:-options -encoding UTF-8 -cp "$work/build/classes;$work/build/stubs;$android" -d "$work/build/test-stubs" @fakes }
    $cp="$work/build/test-stubs;$work/build/classes;$work/build/stubs;$root/.tooling/test-libs/junit.jar;$root/.tooling/test-libs/hamcrest.jar;$android"
    $tests=@(Get-ChildItem "$PSScriptRoot/test" -Recurse -Filter '*.java' | ForEach-Object FullName)
    Run 'Compile cleanup regressions' { & $javac --release 8 -encoding UTF-8 -cp $cp -d "$work/build/tests" @tests }
    Run 'Journal, single-request, idle path, native opt-in and Service pause tests' { & $java -cp "$cp;$work/build/tests" org.junit.runner.JUnitCore com.shilapi.xcertplay.patch.SppGuardTest com.shilapi.xcertplay.patch.SppRecoveryPrefsTest com.shilapi.xcertplay.patch.OemSppPauseTest }
    Run 'Compile-only classpath jar' { & $jar --create --file "$work/build/stubs.jar" -C "$work/build/stubs" . }
    Run 'Authored helper jar' { & $jar --create --file "$work/build/helper.jar" -C "$work/build/classes" . }
    Run 'D8 API17' { & $java -cp "$bt/lib/d8.jar" com.android.tools.r8.D8 --release --min-api 17 --lib $android --classpath "$work/build/stubs.jar" --output "$work/build/dex" "$work/build/helper.jar" }
    Run 'Version labels and two exact call sites' { & $PythonPath "$PSScriptRoot/patch_apk.py" patch }
    Run 'Assemble UI' { & $java -cp $jadx "$root/easyplay-oem4/tools/DexTool.java" asm "$work/modified" "$work/overlay.dex" }
    Run 'Merge' { & $java -Xmx1g -cp $jadx "$root/easyplay-oem4/tools/DexTool.java" merge "$work/patched.dex" "$work/base.dex" "$work/overlay.dex" "$work/build/dex/classes.dex" }
    Run 'Audit unchanged business and protocol classes' { & $java -Xmx1g -cp $jadx "$PSScriptRoot/tools/Audit.java" "$work/base.dex" "$work/patched.dex" }
    Run 'Repack' { & $PythonPath "$PSScriptRoot/patch_apk.py" repack }
    Run 'Align' { & "$bt/zipalign.exe" -f 4 "$work/unsigned.apk" "$work/aligned.apk" }
    $apk="$root/artifacts/CarConnect-0.1.10-beta-OEM-SPP-guard-test-Android4.2-4.4.apk"
    Run 'Sign pure V1 with existing CarConnect test key' {
        & $java --enable-native-access=ALL-UNNAMED -jar "$bt/lib/apksigner.jar" sign --ks "$root/.private/kitkat-probe-debug.jks" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --min-sdk-version 17 --v1-signing-enabled true --v2-signing-enabled false --v3-signing-enabled false --v4-signing-enabled false --out $apk "$work/aligned.apk"
    }
    Run 'API17 signature' { & $java --enable-native-access=ALL-UNNAMED -jar "$bt/lib/apksigner.jar" verify --verbose --print-certs --min-sdk-version 17 $apk }
    Run 'Alignment' { & "$bt/zipalign.exe" -c 4 $apk }
    Run 'Payload' { & $PythonPath "$PSScriptRoot/verify_apk.py" }
    Run 'Manifest' { & "$bt/aapt.exe" dump xmltree $apk AndroidManifest.xml }
    Write-Output "Output: $apk"
} finally { Pop-Location }
