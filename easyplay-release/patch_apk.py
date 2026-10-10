"""Authored hooks over the verified 0.1.11 payload; generated OEM code stays private."""
from pathlib import Path
import hashlib, importlib.util, json, re, struct, sys, zipfile

ROOT = Path(__file__).resolve().parent.parent
WORK = ROOT / '.private/apk-analysis/easyplay/release-012'
BASE = ROOT / 'artifacts/CarConnect-0.1.11-beta-BC03-unified-test-Android4.2-4.4.apk'
BASE_HASH = 'c8fcfc38635b3a2cce6600004f8df59f50c2f922c7cf1283caa462f65e318b61'
OLD_VERSION = 'Carplay-connect-0.1.11-beta-BC03-unified-test'
VERSION = 'CarConnect-0.1.12-BC03-Unified-AV-Wheel-Android4.2plus-ARMv7'
TITLE = 'CarConnect 0.1.12 · BC03/AV/Wheel'
CODE = 55
APK = ROOT / 'artifacts' / (VERSION + '.apk')
CLASSES = ['com/shilapi/xcertplay/' + n for n in [
    'legacy/LegacyWheelDispatcher', 'legacy/LegacyMediaKeyGate',
    'legacy/LegacyMediaControl$PlatformBackend', 'legacy/LegacyActivity',
    'legacy/LegacyConnectionScreen', 'legacy/LegacySessionService',
    'patch/EnhancementPanel', 'patch/CarConnectInfo']]
spec = importlib.util.spec_from_file_location('brand', ROOT / 'easyplay-oem4/patch_brand.py')
brand = importlib.util.module_from_spec(spec); spec.loader.exec_module(brand)
P = 'Lcom/shilapi/xcertplay/patch/'

def prepare():
    assert hashlib.sha256(BASE.read_bytes()).hexdigest() == BASE_HASH
    WORK.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(BASE) as z: (WORK / 'base.dex').write_bytes(z.read('classes3.dex'))

def method(text, name):
    found = list(re.finditer(r'(?ms)^\.method [^\n]* ' + re.escape(name) + r'\([^\n]*\n.*?^\.end method', text))
    assert len(found) == 1, name
    return found[0]

def hook_begin(text, name, hook):
    m = method(text, name); body = m.group()
    body = re.sub(r'(    \.registers \d+\n)', r'\1\n' + hook + '\n', body, count=1)
    return text[:m.start()] + body + text[m.end():]

def patch_class(name, text):
    text = text.replace(OLD_VERSION, VERSION).replace('CarConnect 0.1.11 beta (BC03 unified)', TITLE)
    if name.endswith('CarConnectInfo'):
        about = VERSION + '\n\nAndroid 4.2+/API17，ARMv7 32位正式发布。BC03 已核对的 1.7.2 / 1.7.9 / 1.3.6 统一接入；包含音频缓冲、独立导航/媒体音量、标准全屏及方向盘事件时间修正。\n\n沿用包名和证书，覆盖安装保留已选 iPhone 和设置。方向盘改动已通过主机回归，实际车机仍需验证；K2001N 有线重启和 SD8227 专用启动不在本版修复范围。\n\n基于用户提供的 EasyPlay 0.2.7(36) 修改，参考 DiPlay / xcertplay。保留原许可证和第三方声明。仅供学习参考，禁止商业用途。\n\n项目主页：https://github.com/beidouxiaonan'
        pattern = r'(?m)^(    const-string(?:/jumbo)? \w+, )"' + re.escape(VERSION) + r'\\n\\nAndroid [^\n]+"$'
        text, count = re.subn(pattern, lambda m: m[1] + json.dumps(about, ensure_ascii=True), text)
        assert count == 1, count
    if name.endswith('LegacyWheelDispatcher') or name.endswith('LegacyMediaKeyGate'):
        key_method = 'handleAndroid' if name.endswith('LegacyWheelDispatcher') else 'accept'
        m = method(text, key_method)
        preserved = m.group().replace(' ' + key_method + '(', ' ' + key_method + 'Original(', 1)
        wrapper = f'''\n\n.method public final {key_method}(Landroid/view/KeyEvent;)Z
    .registers 3
    invoke-static {{p0, p1}}, {P}WheelInputRecovery;->normalize(Ljava/lang/Object;Landroid/view/KeyEvent;)Landroid/view/KeyEvent;
    move-result-object p1
    invoke-virtual {{p0, p1}}, L{name};->{key_method}Original(Landroid/view/KeyEvent;)Z
    move-result v0
    return v0
.end method'''
        text = text[:m.start()] + preserved + wrapper + text[m.end():]
        text = hook_begin(text, 'close' if key_method == 'handleAndroid' else 'clear',
                          f'    invoke-static {{p0}}, {P}WheelInputRecovery;->clear(Ljava/lang/Object;)V')
    elif name.endswith('LegacyMediaControl$PlatformBackend'):
        m = method(text, 'activate'); body = m.group()
        hook = f'''    invoke-direct {{p0}}, L{name};->getAudio()Landroid/media/AudioManager;
    move-result-object v0
    iget-object v1, p0, L{name};->component:Landroid/content/ComponentName;
    iget-object v2, p0, L{name};->remote:Landroid/media/RemoteControlClient;
    invoke-static {{p0, v0, v1, v2}}, {P}MediaKeyRecovery;->start(Ljava/lang/Object;Landroid/media/AudioManager;Landroid/content/ComponentName;Landroid/media/RemoteControlClient;)V
'''
        assert body.count('    return-void') == 1
        body = body.replace('    return-void', hook + '\n    return-void')
        text = text[:m.start()] + body + text[m.end():]
        text = hook_begin(text, 'close', f'    invoke-static {{p0}}, {P}MediaKeyRecovery;->stop(Ljava/lang/Object;)V')
    elif name.endswith('LegacyActivity'):
        pattern = r'(?m)^    invoke-static (\{[^\n]+\}), ' + re.escape(P) + r'FullscreenRecovery;->(resumed|paused|focused|carPlay)(\([^\n]+)'
        count = 0
        def add(m):
            nonlocal count
            count += 1
            return m.group() + f'\n\n    invoke-static {m[1]}, {P}MediaKeyRecovery;->{m[2]}{m[3]}'
        text = re.sub(pattern, add, text)
        assert count == 5, count
    return text

def patch():
    for name in CLASSES:
        original = (WORK / 'original' / (name + '.smali')).read_text(encoding='utf-8')
        target = WORK / 'modified' / (name + '.smali'); target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(patch_class(name, original), encoding='utf-8', newline='\n')

def manifest(data):
    data = bytearray(brand.binary_replace(data, {OLD_VERSION: VERSION, 'CarConnect': 'CarConnect BC03'}))
    at = struct.unpack_from('<H', data, 2)[0]; names = None; changed = 0
    while at < len(data):
        kind, header, size = struct.unpack_from('<HHI', data, at)
        if kind == 1: names = brand.strings(data[at:at+size])
        elif kind == 0x102:
            start, span, count = struct.unpack_from('<HHH', data, at+24)
            for i in range(count):
                pos = at+16+start+i*span
                if names[struct.unpack_from('<I', data, pos+4)[0]] == 'versionCode':
                    assert struct.unpack_from('<I', data, pos+16)[0] == 54
                    struct.pack_into('<I', data, pos+16, CODE); changed += 1
        at += size
    assert changed == 1
    return bytes(data)

def signing(name):
    n = name.upper()
    return n.startswith('META-INF/') and (n.endswith(('.RSA', '.DSA', '.EC', '.SF')) or n == 'META-INF/MANIFEST.MF')

def repack():
    with zipfile.ZipFile(BASE) as src, zipfile.ZipFile(WORK/'unsigned.apk', 'w') as dst:
        for info in src.infolist():
            if signing(info.filename): continue
            data = src.read(info.filename)
            if info.filename == 'classes3.dex': data = (WORK/'patched.dex').read_bytes()
            elif info.filename == 'AndroidManifest.xml': data = manifest(data)
            elif info.filename == 'resources.arsc': data = brand.binary_replace(data, {'CarConnect': 'CarConnect BC03'})
            elif info.filename == 'assets/easyplay/about.html':
                data = data.decode().replace(OLD_VERSION, VERSION).replace('</p>',
                    '</p><p>0.1.12 正式发布：BC03 已核对版本统一接入、音频与全屏优化、方向盘按键时间去重修正及前台媒体入口维护。最低 Android 4.2/API17，ARMv7 32位，纯 V1。正式发布状态不代表所有车机已实测；K2001N 有线重启和 SD8227 专用启动仍属已知限制。</p>', 1).encode()
            elif info.filename == 'assets/easyplay/releases.json':
                old = json.loads(data)
                old.insert(0, {'versionCode': CODE, 'versionName': VERSION, 'publishedAt': '2026-10-11',
                    'notesHtml': '<p>BC03 统一接入、音画与全屏优化；修正重复按键时间导致的长期过滤，旧安卓 CarPlay 前台维护媒体按键入口。适配范围及已知限制见 GitHub 正式版说明。</p>'})
                data = json.dumps(old, ensure_ascii=False, indent=2).encode()
            dst.writestr(info, data)

if __name__ == '__main__': {'prepare': prepare, 'patch': patch, 'repack': repack}[sys.argv[1]]()
