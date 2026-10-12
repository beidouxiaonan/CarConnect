from pathlib import Path
import hashlib, zipfile
ROOT=Path(__file__).resolve().parent.parent
OUT=ROOT/'artifacts/CarConnect-0.1.17-BC03-Unified-FirstFrame30s-AllLogs-patch-source.zip'
files=list((ROOT/'easyplay-integration17/src').rglob('*.java'))+list((ROOT/'easyplay-integration17/stubs').rglob('*.java'))+list((ROOT/'easyplay-integration17/test').rglob('*.java'))+list((ROOT/'easyplay-integration17/tools').rglob('*.java'))
files += list((ROOT/'easyplay-integration17/first-frame-test').rglob('*.java'))+list((ROOT/'easyplay-integration17/first-frame-test-stubs').rglob('*.java'))
files += [ROOT/'easyplay-integration17'/n for n in ('README.zh-CN.md','build.ps1','patch_apk.py','verify_apk.py','package_public.py')]
files += [ROOT/'easyplay-oem4/patch_brand.py',ROOT/'easyplay-oem4/tools/DexTool.java',ROOT/'easyplay-bc03-unified/tools/Audit.java']
if (ROOT/'LICENSE').is_file():files.append(ROOT/'LICENSE')
with zipfile.ZipFile(OUT,'w',zipfile.ZIP_DEFLATED) as archive:
 for file in sorted(files):
  assert file.is_file() and '.private' not in file.parts
  archive.writestr(file.relative_to(ROOT).as_posix(),file.read_bytes())
digest=hashlib.sha256(OUT.read_bytes()).hexdigest();OUT.with_suffix('.zip.sha256').write_text(digest+'  '+OUT.name+'\n','utf-8')
print(OUT.name,digest)
