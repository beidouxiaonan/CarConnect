from pathlib import Path
import hashlib
import importlib.util
import struct
import zipfile

ROOT = Path(__file__).resolve().parent.parent
spec = importlib.util.spec_from_file_location('patch', ROOT / 'easyplay-bc03-172-cleanup/patch_apk.py')
p = importlib.util.module_from_spec(spec)
spec.loader.exec_module(p)
allowed = {'classes3.dex', 'AndroidManifest.xml', 'assets/easyplay/about.html', 'assets/easyplay/releases.json'}
with zipfile.ZipFile(p.BASE) as base, zipfile.ZipFile(p.APK) as out:
    assert out.testzip() is None
    original = {x for x in base.namelist() if not p.signing(x)}
    final = {x for x in out.namelist() if not p.signing(x)}
    assert original == final, 'Payload entries changed'
    changed = {x for x in original if base.read(x) != out.read(x)}
    assert changed == allowed, changed
    for name in ['classes.dex', 'classes2.dex', 'classes3.dex']:
        assert out.read(name)[:8] == b'dex\n035\0'
    libs = [n for n in final if n.startswith('lib/') and n.endswith('.so')]
    assert len(libs) == 3 and all(n.startswith('lib/armeabi-v7a/') for n in libs)
    for name in libs:
        elf = out.read(name)
        assert elf[:5] == b'\x7fELF\x01' and struct.unpack_from('<H', elf, 18)[0] == 40
    assert out.read('AndroidManifest.xml') == p.manifest(base.read('AndroidManifest.xml'))
    assert p.VERSION.encode() in out.read('assets/easyplay/about.html')
    for marker in ['oemSppCleanup','SppDisConnect','C3：原车 SPP 未连接状态稳定 300ms','连接前自动清理原车 SPP（含底层恢复）','S3：BC03 1.7.2 未上报 SPP','S6：SPP 状态缺失且 8 秒内没有收到数据','暂不清理 SPP','C4：底层 VH 已写入','C5：底层清理后 SPP 未连接状态稳定 300ms','启动不发送断开命令']:
        assert marker.encode() in out.read('classes3.dex'),marker
digest = hashlib.sha256(p.APK.read_bytes()).hexdigest()
p.APK.with_suffix('.apk.sha256').write_text(digest + '  ' + p.APK.name + '\n', encoding='utf-8')
print('ZIP CRC/DEX035/ARMv7 ELF32/manifest verified; original native, assets, classes1/2 preserved')
print('Changed payload:', sorted(changed))
print('SHA256', digest)
