"""Opt-in SPP cleanup patch on the exact released CarConnect OEM 0.1.4 APK."""
from pathlib import Path
import hashlib
import importlib.util
import json
import struct
import sys
import zipfile

ROOT = Path(__file__).resolve().parent.parent
WORK = ROOT / '.private/apk-analysis/easyplay/spp-cleanup'
BASE = ROOT / 'artifacts/CarConnect-0.1.4-beta-OEM-test-Android4.2.apk'
BASE_HASH = 'dfccdeb5172d61840d5323a1857e3fe280d8b6c29ef30fe3ab6b937cb0871312'
OLD_VERSION = 'Carplay-connect-0.1.4-beta-OEM-test'
VERSION = 'Carplay-connect-0.1.8-beta-OEM-SPP-native-recovery-test'
CODE = 49
APK = ROOT / 'artifacts/CarConnect-0.1.8-beta-OEM-SPP-native-recovery-test-Android4.2-4.4.apk'
CLASSES = ['com/shilapi/xcertplay/' + x for x in [
    'patch/EnhancementPanel',
    'patch/CarConnectInfo', 'legacy/LegacyConnectionScreen', 'legacy/LegacySessionService']]
spec = importlib.util.spec_from_file_location('brand', ROOT / 'easyplay-oem4/patch_brand.py')
brand = importlib.util.module_from_spec(spec)
spec.loader.exec_module(brand)


def prepare():
    assert hashlib.sha256(BASE.read_bytes()).hexdigest() == BASE_HASH, 'Unexpected released BC03 baseline'
    WORK.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(BASE) as z:
        (WORK / 'base.dex').write_bytes(z.read('classes3.dex'))


def patch():
    for name in CLASSES:
        text = (WORK / 'original' / (name + '.smali')).read_text(encoding='utf-8')
        text = text.replace(OLD_VERSION, VERSION).replace('CarConnect 0.1.4 beta', 'CarConnect 0.1.8 beta')
        if name.endswith('/EnhancementPanel'):
            anchors = [
                ('    invoke-static {p0}, Lcom/shilapi/xcertplay/patch/EnhancementPrefs;->init(Landroid/content/Context;)V',
                 '    invoke-static {p0}, Lcom/shilapi/xcertplay/patch/OemTransport;->startup(Landroid/content/Context;)V'),
                ('    invoke-virtual {v2, v0}, Landroid/widget/ScrollView;->addView(Landroid/view/View;)V',
                 '    invoke-static {p0, v0}, Lcom/shilapi/xcertplay/patch/SppCleanupSettings;->add(Landroid/app/Activity;Landroid/widget/LinearLayout;)V')]
            # The first init belongs to attach(); add startup once, not when showing dialogs.
            assert text.count(anchors[0][0]) == 2
            text = text.replace(anchors[0][0], anchors[0][0]+'\n\n'+anchors[0][1], 1)
            assert text.count(anchors[1][0]) == 1  # show(); diagnostics uses v1 for its ScrollView
            text = text.replace(anchors[1][0], anchors[1][0]+'\n\n'+anchors[1][1], 1)
        path = WORK / 'modified' / (name + '.smali')
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding='utf-8')


def manifest(data):
    data = bytearray(brand.binary_replace(data, {OLD_VERSION: VERSION}))
    at = struct.unpack_from('<H', data, 2)[0]
    names = None
    changed = 0
    while at < len(data):
        kind, header, size = struct.unpack_from('<HHI', data, at)
        if kind == 1:
            names = brand.strings(data[at:at + size])
        elif kind == 0x102:
            start, span, count = struct.unpack_from('<HHH', data, at + 24)
            for i in range(count):
                pos = at + 16 + start + i * span
                if names[struct.unpack_from('<I', data, pos + 4)[0]] == 'versionCode':
                    assert struct.unpack_from('<I', data, pos + 16)[0] == 41
                    struct.pack_into('<I', data, pos + 16, CODE)
                    changed += 1
        at += size
    assert changed == 1
    return bytes(data)


def signing(name):
    n = name.upper()
    return n.startswith('META-INF/') and (n.endswith(('.RSA', '.DSA', '.EC', '.SF')) or n == 'META-INF/MANIFEST.MF')


def repack():
    with zipfile.ZipFile(BASE) as src, zipfile.ZipFile(WORK / 'unsigned.apk', 'w') as dst:
        for info in src.infolist():
            if signing(info.filename):
                continue
            data = src.read(info.filename)
            if info.filename == 'classes3.dex':
                data = (WORK / 'patched.dex').read_bytes()
            elif info.filename == 'AndroidManifest.xml':
                data = manifest(data)
            elif info.filename == 'assets/easyplay/about.html':
                data = data.decode().replace(OLD_VERSION, VERSION)
                data = data.replace('</p>', '</p><p>OEM SPP 底层恢复测试：启动只检查状态；连接前原服务未释放时发送一次已核对的无索引 VH，等待状态稳定后再连接。尚待实车验证。</p>', 1).encode()
            elif info.filename == 'assets/easyplay/releases.json':
                old = json.loads(data)
                old.insert(0, {'versionCode': CODE, 'versionName': VERSION, 'publishedAt': '2026-10-08',
                    'notesHtml': '<p>启动改为只读检查，清理延后到连接请求。原服务 5 秒未释放时发送一次无索引 VH，最多再等 12 秒，稳定 300ms 后连接。未知指纹或仍占用时停止；45 秒限制重复清理。使用原清理开关并保存，不重启整个蓝牙模块。</p>'})
                data = json.dumps(old, ensure_ascii=False, indent=2).encode()
            dst.writestr(info, data)


if __name__ == '__main__':
    {'prepare': prepare, 'patch': patch, 'repack': repack}[sys.argv[1]]()
