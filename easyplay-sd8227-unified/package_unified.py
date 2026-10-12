"""Publish authored bootstrap + current feature patch sources, excluding private inputs."""
from pathlib import Path
import hashlib,zipfile
ROOT=Path(__file__).resolve().parent.parent
OUT=ROOT/'artifacts/CarConnect-0.1.18-SD8227-Unified-BC03-FirstFrame30s-AllLogs-patch-source.zip'
files=[]
for folder in ('easyplay-sd8227-unified','easyplay-integration17'):
    for p in (ROOT/folder).rglob('*'):
        if p.is_file() and p.suffix in ('.java','.py','.ps1','.md') and not any(x in p.parts for x in ('build','__pycache__')):
            files.append(p)
files += [ROOT/'easyplay-oem4/patch_brand.py',ROOT/'easyplay-oem4/tools/DexTool.java',ROOT/'easyplay-bc03-unified/tools/Audit.java',ROOT/'LICENSE']
with zipfile.ZipFile(OUT,'w',zipfile.ZIP_DEFLATED) as archive:
    for p in sorted(set(files)):
        relative=p.relative_to(ROOT);assert '.private' not in relative.parts
        archive.writestr(relative.as_posix(),p.read_bytes())
digest=hashlib.sha256(OUT.read_bytes()).hexdigest()
OUT.with_suffix('.zip.sha256').write_text(digest+'  '+OUT.name+'\n','utf-8',newline='\n')
print(OUT.name,digest,'files='+str(len(set(files))))
