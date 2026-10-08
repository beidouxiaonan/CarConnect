"""Publish only authored bootstrap sources and documentation; no private inputs."""
from pathlib import Path
import hashlib
import shutil
import zipfile

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'artifacts' / 'CarConnect-SD8227-multidex-publication'
OUT.mkdir(parents=True, exist_ok=True)
names = ['profile.py', 'build.ps1', 'collect-install.ps1', 'package_public.py',
         'README.md', 'VERIFICATION.zh-CN.md', 'STARTUP.zh-CN.md', 'CREDITS.md',
         'build-startup.ps1', 'collect-startup.ps1', 'startup.py', 'pool.py',
         'tools/DexTool.java', 'tools/StartupAudit.java',
         'test/LegacyDexInstallerTest.java', 'test/StartupStateTest.java', 'test/test_manifest.py']
names += ['src/io/github/beidouxiaonan/carconnect/startup/' + n for n in
    ['StartupApplication.java','EntryActivity.java','StartupLog.java','StartupState.java','LegacyDexInstaller.java']]
files = {f'easyplay-sd8227/{name}': ROOT / 'easyplay-sd8227' / name for name in names}
for name in ['README.md', 'VERIFICATION.zh-CN.md', 'STARTUP.zh-CN.md']:
    shutil.copyfile(ROOT / 'easyplay-sd8227' / name, OUT / name)
    files[name] = OUT / name
shutil.copyfile(ROOT / 'LICENSE', OUT / 'LICENSE')
shutil.copyfile(ROOT / 'easyplay-sd8227/CREDITS.md', OUT / 'CREDITS.md')
for name in ['LICENSE', 'CREDITS.md']:
    files[name] = OUT / name
source = ROOT / 'artifacts/CarConnect-0.1.7-SD8227-multidex-patch-source.zip'
with zipfile.ZipFile(source, 'w', zipfile.ZIP_DEFLATED, allowZip64=False) as archive:
    for name, path in sorted(files.items()):
        assert path.is_file() and not any(x in name.split('/') for x in ['.private','build','original','modified','__pycache__'])
        assert path.suffix in ['.java','.py','.ps1','.md'] or path.name == 'LICENSE'
        archive.write(path, name)
with zipfile.ZipFile(source) as archive:
    if archive.testzip() is not None or set(archive.namelist()) != set(files):
        raise ValueError('Public source allowlist/CRC mismatch')
sha = hashlib.sha256(source.read_bytes()).hexdigest()
source.with_suffix('.zip.sha256').write_text(sha + '  ' + source.name + '\n', encoding='ascii')
for path in [source, source.with_suffix('.zip.sha256'),
    ROOT/'artifacts/CarConnect-0.1.7-SD8227-multidex-test.apk.sha256',
    ROOT/'artifacts/CarConnect-0.1.7-SD8227-multidex-test.audit.json']:
    shutil.copyfile(path,OUT/path.name)
print(f'Public source: {len(files)} allowed files, SHA256={sha}')
