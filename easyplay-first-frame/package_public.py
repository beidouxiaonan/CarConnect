from pathlib import Path
import hashlib,shutil,zipfile
ROOT=Path(__file__).resolve().parent.parent
module=ROOT/'easyplay-first-frame'
output=ROOT/'artifacts/CarConnect-0.1.9-beta-OEM-first-frame-diagnostics-patch-source.zip'
files=[]
for folder in ['src','stubs','test','test-stubs','tools']:files.extend((module/folder).rglob('*.java'))
files.extend(module/n for n in ['build.ps1','patch_apk.py','verify_apk.py','package_public.py','README.md','ANALYSIS.zh-CN.md','BUILD.zh-CN.md','VERIFICATION.zh-CN.md'])
files.extend(ROOT/n for n in ['LICENSE','easyplay-oem4/CREDITS.md','easyplay-oem4/tools/DexTool.java','easyplay-oem4/patch_brand.py','easyplay-oem4/src/com/shilapi/xcertplay/patch/OemProtocol.java','easyplay-oem4/stubs/com/shilapi/xcertplay/transport/BluetoothRfcommDuplexStream.java','easyplay-oem4/test-stubs/android/content/Context.java'])
with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as z:
 for p in sorted(files):
  name=p.relative_to(ROOT).as_posix()
  assert p.is_file() and not any(x in name.split('/') for x in ['.private','build','original','modified','__pycache__'])
  assert p.suffix in ['.java','.py','.ps1','.md'] or p.name=='LICENSE'
  z.write(p,name)
with zipfile.ZipFile(output) as z:assert z.testzip() is None and len(z.namelist())==len(files)
digest=hashlib.sha256(output.read_bytes()).hexdigest()
output.with_suffix('.zip.sha256').write_text(digest+'  '+output.name+'\n',encoding='utf-8')
pub=ROOT/'artifacts/CarConnect-OEM-first-frame-publication';pub.mkdir(exist_ok=True)
docs=[('README.md','OEM-RECOVERY-USAGE.zh-CN.md'),('ANALYSIS.zh-CN.md','OEM-RECOVERY-ANALYSIS.zh-CN.md'),('BUILD.zh-CN.md','OEM-RECOVERY-BUILD.zh-CN.md'),('VERIFICATION.zh-CN.md','OEM-RECOVERY-VERIFICATION.zh-CN.md')]
for a,b in docs:
 text=(module/a).read_text(encoding='utf-8')
 for source,target in docs:text=text.replace(']('+source+')',']('+target+')')
 (pub/b).write_text(text,encoding='utf-8')
(pub/'README.md').write_text('[下载测试 APK 和补丁源码](https://github.com/beidouxiaonan/CarConnect/releases/tag/v0.1.9-beta-oem-first-frame-diagnostics-test)\n\n'+(pub/'OEM-RECOVERY-USAGE.zh-CN.md').read_text(encoding='utf-8'),encoding='utf-8')
for p in [output,output.with_suffix('.zip.sha256'),ROOT/'artifacts/CarConnect-0.1.9-beta-OEM-first-frame-diagnostics-test-Android4.2-4.4.apk.sha256']:shutil.copyfile(p,pub/p.name)
print('Public authored source:',len(files),'files;',output.stat().st_size,'bytes; SHA256',digest)
