param([string]$PythonPath='D:/Program Files/Python3.13.13/python.exe')
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath("$PSScriptRoot/..")
$work="$root/.private/apk-analysis/easyplay/release-012"
$java="$root/.tooling/jdk25/bin/java.exe"
$javac="$root/.tooling/jdk25/bin/javac.exe"
$jar="$root/.tooling/jdk25/bin/jar.exe"
$android="$root/.tooling/platform/android-4.2.2/android.jar"
$bt="$root/.tooling/build-tools/android-15"
$jadx="$root/.tooling/jadx/lib/jadx-1.5.6-all.jar"
function Run([string]$stage,[scriptblock]$block) {
 $prev=$ErrorActionPreference;$ErrorActionPreference='Continue'
 try {$lines=& $block 2>&1;$code=$LASTEXITCODE}finally{$ErrorActionPreference=$prev}
 foreach($line in $lines){Write-Output "$line"}
 if($code -ne 0){throw "$stage failed: $code"}
}
Push-Location $root
try {
 Run 'Verify exact released unified baseline' { & $PythonPath "$PSScriptRoot/patch_apk.py" prepare }
 foreach($folder in @('original','modified','build')) {
  $target=[IO.Path]::GetFullPath("$work/$folder")
  if([IO.Path]::GetDirectoryName($target) -ne [IO.Path]::GetFullPath($work) -or -not $target.StartsWith($root+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Unsafe generated path'}
  if(Test-Path -LiteralPath $target){Remove-Item -LiteralPath $target -Recurse -Force}
 }
 New-Item -ItemType Directory -Force "$work/build/classes","$work/build/tests","$work/build/fakes","$work/build/dex","$work/build/tools"|Out-Null
 $selected=@('Lcom/shilapi/xcertplay/legacy/LegacyWheelDispatcher;','Lcom/shilapi/xcertplay/legacy/LegacyMediaKeyGate;',
  'Lcom/shilapi/xcertplay/legacy/LegacyMediaControl$PlatformBackend;','Lcom/shilapi/xcertplay/legacy/LegacyActivity;',
  'Lcom/shilapi/xcertplay/legacy/LegacyConnectionScreen;','Lcom/shilapi/xcertplay/legacy/LegacySessionService;',
  'Lcom/shilapi/xcertplay/patch/EnhancementPanel;','Lcom/shilapi/xcertplay/patch/CarConnectInfo;')
 Run 'Disassemble only eight intended classes' { & $java -cp $jadx "$root/easyplay-oem4/tools/DexTool.java" dis "$work/base.dex" "$work/original" @selected }
 $sources=@(Get-ChildItem "$PSScriptRoot/src" -Recurse -Filter '*.java'|ForEach-Object FullName)
 Run 'Compile wheel and foreground helpers against API17' { & $javac --release 8 -Xlint:-options -encoding UTF-8 -cp $android -d "$work/build/classes" @sources }
 $fakes=@(Get-ChildItem "$PSScriptRoot/test-stubs" -Recurse -Filter '*.java'|ForEach-Object FullName)
 Run 'Compile isolated host Android fakes' { & $javac --release 8 -Xlint:-options -d "$work/build/fakes" @fakes }
 $cp="$work/build/fakes;$work/build/classes;$root/.tooling/test-libs/junit.jar;$root/.tooling/test-libs/hamcrest.jar;$android"
 $tests=@(Get-ChildItem "$PSScriptRoot/test" -Recurse -Filter '*.java'|ForEach-Object FullName)
 Run 'Compile wheel and lifecycle regressions' { & $javac --release 8 -encoding UTF-8 -cp $cp -d "$work/build/tests" @tests }
 Run 'Continuous next/previous, duplicate, long press, lifecycle and close tests' { & $java -cp "$cp;$work/build/tests" org.junit.runner.JUnitCore com.shilapi.xcertplay.patch.WheelRecoveryTest com.shilapi.xcertplay.patch.MediaKeyRecoveryTest }
 Run 'Helper jar (no fakes)' { & $jar --create --file "$work/build/helper.jar" -C "$work/build/classes" . }
 Run 'D8 API17' { & $java -cp "$bt/lib/d8.jar" com.android.tools.r8.D8 --release --min-api 17 --lib $android --output "$work/build/dex" "$work/build/helper.jar" }
 Run 'Preserve original mapping and add exact lifecycle hooks' { & $PythonPath "$PSScriptRoot/patch_apk.py" patch }
 Run 'Assemble expected hooks' { & $java -cp $jadx "$root/easyplay-oem4/tools/DexTool.java" asm "$work/modified" "$work/overlay.dex" }
 Run 'Merge helpers and original classes' { & $java -Xmx1g -cp $jadx "$root/easyplay-oem4/tools/DexTool.java" merge "$work/patched.dex" "$work/base.dex" "$work/overlay.dex" "$work/build/dex/classes.dex" }
 Run 'Compile canonical audit' { & $javac --release 8 -cp $jadx -d "$work/build/tools" "$root/easyplay-bc03-unified/tools/Audit.java" "$PSScriptRoot/tools/ReleaseAudit.java" }
 Run 'Audit every class and retained AV/SPP paths' { & $java -Xmx1g -cp "$jadx;$work/build/tools" ReleaseAudit "$work/base.dex" "$work/patched.dex" "$work/overlay.dex" }
 Run 'Repack' { & $PythonPath "$PSScriptRoot/patch_apk.py" repack }
 Run 'Align' { & "$bt/zipalign.exe" -f 4 "$work/unsigned.apk" "$work/aligned.apk" }
 $apk="$root/artifacts/CarConnect-0.1.12-BC03-Unified-AV-Wheel-Android4.2plus-ARMv7.apk"
 Run 'Sign V1 with same CarConnect certificate' { & $java --enable-native-access=ALL-UNNAMED -jar "$bt/lib/apksigner.jar" sign --ks "$root/.private/kitkat-probe-debug.jks" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --min-sdk-version 17 --v1-signing-enabled true --v2-signing-enabled false --v3-signing-enabled false --v4-signing-enabled false --out $apk "$work/aligned.apk" }
 Run 'Verify API17 signature' { & $java --enable-native-access=ALL-UNNAMED -jar "$bt/lib/apksigner.jar" verify --verbose --print-certs --min-sdk-version 17 $apk }
 Run 'Alignment' { & "$bt/zipalign.exe" -c 4 $apk }
 Run 'Verify payload and architecture' { & $PythonPath "$PSScriptRoot/verify_apk.py" }
 Run 'Read final manifest' { & "$bt/aapt.exe" dump xmltree $apk AndroidManifest.xml }
 Write-Output "Output: $apk"
}finally{Pop-Location}
