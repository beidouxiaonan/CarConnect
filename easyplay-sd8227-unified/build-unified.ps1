param([string]$PythonPath='D:/Program Files/Python3.13.13/python.exe')
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath("$PSScriptRoot/..")
$work="$root/.private/apk-analysis/easyplay/sd8227-unified-018"
$java="$root/.tooling/jdk25/bin/java.exe";$javac="$root/.tooling/jdk25/bin/javac.exe";$jar="$root/.tooling/jdk25/bin/jar.exe"
$bt="$root/.tooling/build-tools/android-15";$android="$root/.tooling/platform/android-4.2.2/android.jar";$jadx="$root/.tooling/jadx/lib/jadx-1.5.6-all.jar"
function Run([string]$stage,[scriptblock]$block){
 $prev=$ErrorActionPreference;$ErrorActionPreference='Continue'
 try{$lines=& $block 2>&1;$code=$LASTEXITCODE}finally{$ErrorActionPreference=$prev}
 foreach($line in $lines){Write-Output "$line"}
 if($code -ne 0){throw "$stage failed: $code"}
}
Push-Location $root
try {
 foreach($folder in @('build','original','modified','original-labels','modified-labels','modified-primary')){
  $target=[IO.Path]::GetFullPath("$work/$folder")
  if([IO.Path]::GetDirectoryName($target) -ne [IO.Path]::GetFullPath($work) -or -not $target.StartsWith($root+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Unsafe generated build path'}
  if(Test-Path -LiteralPath $target){Remove-Item -LiteralPath $target -Recurse -Force}
 }
 New-Item -ItemType Directory -Force "$work/build/bootstrap","$work/build/bootstrap-dex","$work/build/tests","$work/build/stubs","$work/build/features","$work/build/feature-dex","$work/build/tools"|Out-Null
 Run 'Verify exact released 0.1.17 feature baseline' { & $PythonPath "$PSScriptRoot/startup.py" prepare }
 Run 'Compile independent DEX audits' { & $javac --release 8 -cp $jadx -d "$work/build/tools" "$PSScriptRoot/tools/StartupAudit.java" "$PSScriptRoot/tools/UnifiedDex.java" "$PSScriptRoot/tools/UnifiedAudit.java" }
 Run 'Primary-only fallback report provider' { & $java -cp "$jadx;$work/build/tools" UnifiedDex $work }
 $src=@(Get-ChildItem "$PSScriptRoot/src" -Recurse -Filter '*.java'|ForEach-Object FullName)
 Run 'Compile SD8227 bootstrap API17' { & $javac --release 8 -encoding UTF-8 -cp $android -d "$work/build/bootstrap" @src }
 $tests=@(Get-ChildItem "$PSScriptRoot/test" -Filter '*.java'|ForEach-Object FullName)
 Run 'Compile runtime-signature and startup recovery tests' { & $javac --release 8 -encoding UTF-8 -cp "$work/build/bootstrap;$root/.tooling/test-libs/junit.jar" -d "$work/build/tests" @tests }
 Run 'Runtime Dalvik signatures, atomic append and recovery budget' { & $java -cp "$work/build/tests;$work/build/bootstrap;$root/.tooling/test-libs/junit.jar;$root/.tooling/test-libs/hamcrest.jar" org.junit.runner.JUnitCore StartupStateTest LegacyDexInstallerTest }
 Run 'Bootstrap jar' { & $jar --create --file "$work/build/bootstrap.jar" -C "$work/build/bootstrap" . }
 Run 'Compile bootstrap DEX035' { & $java -cp "$bt/lib/d8.jar" com.android.tools.r8.D8 --release --min-api 17 --lib $android --output "$work/build/bootstrap-dex" "$work/build/bootstrap.jar" }
 $stubs=@(Get-ChildItem "$root/easyplay-integration17/stubs" -Recurse -Filter '*.java'|ForEach-Object FullName)
 $stubs+=@(Get-ChildItem "$PSScriptRoot/feature-stubs" -Recurse -Filter '*.java'|ForEach-Object FullName)
 Run 'Compile API17 feature dependency stubs' { & $javac --release 8 -encoding UTF-8 -cp $android -d "$work/build/stubs" @stubs }
 Run 'Compile combined startup and all-log export' { & $javac --release 8 -encoding UTF-8 -cp "$work/build/bootstrap;$work/build/stubs;$android" -d "$work/build/features" "$PSScriptRoot/src-feature/com/shilapi/xcertplay/patch/AllLogsExport.java" }
 Run 'Feature jar' { & $jar --create --file "$work/build/feature.jar" -C "$work/build/features" . }
 Run 'Compile-only feature dependency jar' { & $jar --create --file "$work/build/stubs.jar" -C "$work/build/stubs" . }
 Run 'Feature DEX035' { & $java -cp "$bt/lib/d8.jar" com.android.tools.r8.D8 --release --min-api 17 --lib $android --classpath "$work/build/bootstrap.jar" --classpath "$work/build/stubs.jar" --output "$work/build/feature-dex" "$work/build/feature.jar" }
 Copy-Item -LiteralPath "$work/build/feature-dex/classes.dex" -Destination "$work/feature.dex"
 $labels=@('EnhancementPanel','CarConnectInfo','AvDiagnostics','OemSppPause','OemTransport','SppRecoverySettings')|ForEach-Object {"Lcom/shilapi/xcertplay/patch/$_;"}
 $labels+=@('Lcom/shilapi/xcertplay/legacy/LegacyConnectionScreen;','Lcom/shilapi/xcertplay/legacy/LegacySessionService;')
 Run 'Disassemble current feature version labels' { & $java -cp $jadx "$PSScriptRoot/tools/DexTool.java" dis "$work/source-classes3.dex" "$work/original-labels" @labels }
 Run 'Label overlay only' { & $PythonPath "$PSScriptRoot/startup.py" labels }
 Run 'Assemble label overlay' { & $java -cp $jadx "$PSScriptRoot/tools/DexTool.java" asm "$work/modified-labels" "$work/label-overlay.dex" }
 Run 'Current features and startup-log export' { & $java -cp $jadx "$PSScriptRoot/tools/DexTool.java" merge "$work/base-classes3.dex" "$work/source-classes3.dex" "$work/label-overlay.dex" "$work/feature.dex" }
 Run 'Disassemble current AV/wheel activity' { & $java -cp $jadx "$PSScriptRoot/tools/DexTool.java" dis "$work/base-classes3.dex" "$work/original" 'Lcom/shilapi/xcertplay/legacy/LegacyActivity;' }
 Run 'Disassemble primary MultiDex' { & $java -cp $jadx "$PSScriptRoot/tools/DexTool.java" dis "$work/base-classes.dex" "$work/original" 'Landroidx/multidex/MultiDex;' }
 Run 'Preserve original create/start/resume in startup wrapper' { & $PythonPath "$PSScriptRoot/startup.py" patch }
 Run 'Assemble activity wrapper' { & $java -cp $jadx "$PSScriptRoot/tools/DexTool.java" asm "$work/modified" "$work/overlay.dex" }
 Run 'Assemble exact one-call MultiDex change' { & $java -cp $jadx "$PSScriptRoot/tools/DexTool.java" asm "$work/modified-primary" "$work/primary-overlay.dex" }
 Run 'Merge primary bootstrap and migrated provider' { & $java -Xmx1g -cp $jadx "$PSScriptRoot/tools/DexTool.java" merge "$work/patched-classes.dex" "$work/base-classes.dex" "$work/primary-overlay.dex" "$work/build/bootstrap-dex/classes.dex" }
 Run 'Merge activity wrapper with current functionality' { & $java -Xmx1g -cp $jadx "$PSScriptRoot/tools/DexTool.java" merge "$work/patched-classes3.dex" "$work/base-classes3.dex" "$work/overlay.dex" }
 Run 'Exact lifecycle/MultiDex preservation audit' { & $java -Xmx1g -cp "$jadx;$work/build/tools" StartupAudit $work }
 Run 'Every 0.1.17 feature and primary fallback dependency audit' { & $java -Xmx1g -cp "$jadx;$work/build/tools" UnifiedAudit $work }
 Run 'Classic ZIP and startup launcher manifest' { & $PythonPath "$PSScriptRoot/startup.py" repack }
 Run 'Independent manifest regressions' { & $PythonPath "$PSScriptRoot/test/test_manifest.py" }
 Run 'ZIP alignment' { & "$bt/zipalign.exe" -f 4 "$work/unsigned.apk" "$work/aligned.apk" }
 $apk="$root/artifacts/CarConnect-0.1.18-SD8227-Unified-BC03-FirstFrame30s-AllLogs-Android4.2plus-ARMv7.apk"
 Run 'Sign pure V1 existing certificate' { & $java --enable-native-access=ALL-UNNAMED -jar "$bt/lib/apksigner.jar" sign --ks "$root/.private/kitkat-probe-debug.jks" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --min-sdk-version 17 --v1-signing-enabled true --v2-signing-enabled false --v3-signing-enabled false --v4-signing-enabled false --out $apk "$work/aligned.apk" }
 foreach($api in @(17,19,24)){Run "Signature API$api" { & $java --enable-native-access=ALL-UNNAMED -jar "$bt/lib/apksigner.jar" verify --verbose --min-sdk-version $api --max-sdk-version $api $apk }}
 Run 'Same-certificate verification' { & $java --enable-native-access=ALL-UNNAMED -jar "$bt/lib/apksigner.jar" verify --print-certs $apk }
 Run 'Final alignment' { & "$bt/zipalign.exe" -c 4 $apk }
 Run 'Payload, format, upgraded manifest and 30s policy' { & $PythonPath "$PSScriptRoot/startup.py" audit }
 Write-Output "Output: $apk"
}finally{Pop-Location}
