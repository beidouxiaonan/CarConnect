from pathlib import Path
import hashlib
import importlib.util
import struct
import zipfile

ROOT = Path(__file__).resolve().parent.parent
spec = importlib.util.spec_from_file_location('patch', ROOT / 'easyplay-bc03-unified/patch_apk.py')
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
        dex=out.read(name)
        assert dex[:8] == b'dex\n035\0'
        for offset in (64,80,88):
            assert struct.unpack_from('<I',dex,offset)[0]<=65535,(name,offset,'DEX reference limit')
    libs = [n for n in final if n.startswith('lib/') and n.endswith('.so')]
    assert len(libs) == 3 and all(n.startswith('lib/armeabi-v7a/') for n in libs)
    for name in libs:
        elf = out.read(name)
        assert elf[:5] == b'\x7fELF\x01' and struct.unpack_from('<H', elf, 18)[0] == 40
    assert out.read('AndroidManifest.xml') == p.manifest(base.read('AndroidManifest.xml'))
    assert p.VERSION.encode() in out.read('assets/easyplay/about.html')
    for marker in ['sppRecoveryPending','sppNativeRecovery','允许底层 SPP 恢复','P1：SPP 恢复未确认','原车 SPP 空闲稳定 300ms','已阻止重复清理','firstFrameAutoRetry','oemSppCleanup']:
        assert marker.encode() in out.read('classes3.dex'),marker
    for marker in [p.INPUTS['bc03-172/input.apk'],'BC03 1.7.2 未上报 SPP','8 秒内没有收到数据','OemSppGate','暂不清理 SPP']:
        assert marker.encode() in out.read('classes3.dex'),marker
    for marker in ['Bc03Profiles','OemSocketOutput','OemConnectDeadline','统一适配配置','阶段超时','S4 登记手机地址']:
        assert marker.encode() in out.read('classes3.dex'),marker
digest = hashlib.sha256(p.APK.read_bytes()).hexdigest()
p.APK.with_suffix('.apk.sha256').write_text(digest + '  ' + p.APK.name + '\n', encoding='utf-8')
print('ZIP CRC/DEX035/ARMv7 ELF32/manifest verified; original native, assets, classes1/2 preserved')
print('Changed payload:', sorted(changed))
print('SHA256', digest)
