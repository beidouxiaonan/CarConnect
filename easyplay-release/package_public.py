from pathlib import Path
import hashlib, shutil, zipfile
ROOT=Path(__file__).resolve().parents[1]
MODULE=ROOT/'easyplay-release'
OUT=ROOT/'artifacts/CarConnect-0.1.12-BC03-Unified-AV-Wheel-patch-source.zip'
FILES=[]
for module in [MODULE,ROOT/'easyplay-bc03-unified']:
    FILES.extend(p for p in module.rglob('*') if p.is_file() and p.suffix in ['.java','.py','.ps1','.md'] and '__pycache__' not in p.parts)
FILES.extend(ROOT/n for n in ['LICENSE','easyplay-oem4/CREDITS.md','easyplay-oem4/tools/DexTool.java','easyplay-oem4/patch_brand.py','easyplay-oem4/src/com/shilapi/xcertplay/patch/OemProtocol.java','easyplay-oem4/stubs/com/shilapi/xcertplay/transport/BluetoothRfcommDuplexStream.java','easyplay-oem4/test-stubs/android/content/Context.java'])
FILES=sorted(set(FILES))
with zipfile.ZipFile(OUT,'w',zipfile.ZIP_DEFLATED) as z:
    for p in FILES:
        name=p.relative_to(ROOT).as_posix()
        assert not any(s in name.split('/') for s in ['.private','build','original','modified','__pycache__'])
        assert p.suffix in ['.java','.py','.ps1','.md'] or p.name=='LICENSE'
        z.writestr(name,p.read_bytes().replace(b'\r\n',b'\n'))
with zipfile.ZipFile(OUT) as z:
    assert z.testzip() is None and len(z.namelist())==len(FILES)
    assert not any(n.lower().endswith(('.apk','.dex','.so','.jks','.txt','.pem','.key')) for n in z.namelist())
digest=hashlib.sha256(OUT.read_bytes()).hexdigest()
OUT.with_suffix('.zip.sha256').write_text(digest+'  '+OUT.name+'\n',encoding='utf-8',newline='\n')
pub=ROOT/'artifacts/CarConnect-0.1.12-publication';pub.mkdir(exist_ok=True)
docs=[('README.md','CARCONNECT-README.zh-CN.md'),('USAGE.zh-CN.md','USAGE.zh-CN.md'),('FEATURES.zh-CN.md','FEATURES.zh-CN.md'),('COMPATIBILITY.zh-CN.md','COMPATIBILITY.zh-CN.md'),('BUILD.zh-CN.md','BUILD.zh-CN.md'),('VERIFICATION.zh-CN.md','VERIFICATION.zh-CN.md')]
for source,target in docs:
    text=(MODULE/source).read_text(encoding='utf-8')
    for a,b in docs:text=text.replace(']('+a+')',']('+b+')')
    (pub/target).write_text(text,encoding='utf-8',newline='\n')
for p in [OUT,OUT.with_suffix('.zip.sha256'),ROOT/'artifacts/CarConnect-0.1.12-BC03-Unified-AV-Wheel-Android4.2plus-ARMv7.apk.sha256']:
    shutil.copyfile(p,pub/p.name)
print('Public authored source:',len(FILES),'files;',OUT.stat().st_size,'bytes; SHA256',digest)
