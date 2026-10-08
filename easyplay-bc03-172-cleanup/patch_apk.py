"""Native SPP release recovery and BC03 1.7.2 bounded-probe patch on the exact released CarConnect BC03 1.7.2 SPP 0.1.6 APK."""
from pathlib import Path
import hashlib
import importlib.util
import json
import struct
import sys
import zipfile

ROOT = Path(__file__).resolve().parent.parent
WORK = ROOT / '.private/apk-analysis/easyplay/bc03-172-cleanup'
BASE = ROOT / 'artifacts/CarConnect-0.1.6-beta-BC03-1.7.2-SPP-test.apk'
BASE_HASH = '58e31391441d603fd81bc3a20719a07de6c66ac139a7d708371a374ab61bd46c'
OLD_VERSION = 'Carplay-connect-0.1.6-beta-BC03-172-SPP-test'
VERSION = 'Carplay-connect-0.1.8-beta-BC03-172-SPP-native-recovery-test'
CODE = 50
APK = ROOT / 'artifacts/CarConnect-0.1.8-beta-BC03-1.7.2-SPP-native-recovery-test.apk'
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
        text = text.replace(OLD_VERSION, VERSION).replace('CarConnect 0.1.6 beta', 'CarConnect 0.1.8 beta')
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
                    assert struct.unpack_from('<I', data, pos + 16)[0] == 45
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
                data = data.replace('</p>', '</p><p>BC03 1.7.2 SPP 底层恢复与接入测试：启动只读状态，连接前原清理无效时使用已核对的底层释放，保留手机与开关。尚待实车验证。</p>', 1).encode()
            elif info.filename == 'assets/easyplay/releases.json':
                old = json.loads(data)
                old.insert(0, {'versionCode': CODE, 'versionName': VERSION, 'publishedAt': '2026-10-08',
                    'notesHtml': '<p>基于 BC03 1.7.2 SPP 0.1.6，同步 OEM 0.1.8 的启动只读检查及无索引 VH 底层释放回退。原清理 5 秒无效时尝试一次 native，最多再等 12 秒；只有状态稳定 300ms 才继续。保留 S1~S6、原 VF token/冷却、手机记录和清理开关；不增加权限、不重置模块、不清除配对。</p>'})
                data = json.dumps(old, ensure_ascii=False, indent=2).encode()
            dst.writestr(info, data)


if __name__ == '__main__':
    {'prepare': prepare, 'patch': patch, 'repack': repack}[sys.argv[1]]()
