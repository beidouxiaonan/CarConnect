"""Incremental startup probe: fixed V1 APK in, preserved pairing data and assets out."""
from pathlib import Path
import hashlib, json, re, struct, sys, zipfile
import pool
from profile import payload, signing_block_present, signature_entry, LIBS

ROOT = Path(__file__).resolve().parent.parent
WORK = ROOT / '.private/apk-analysis/easyplay/sd8227-startup-build'
BASE = ROOT / 'artifacts/CarConnect-0.1.4-SD8227-V1-test.apk'
BASE_HASH = '6328988e7b4ae458b576b8cf233bb8d3c3b627d5c7fac476b243d02e46582055'
OUTPUT = ROOT / 'artifacts/CarConnect-0.1.7-SD8227-multidex-test.apk'
VERSION = 'Carplay-connect-0.1.7-beta-SD8227-multidex-test'
ACTIVITY = 'com/shilapi/xcertplay/legacy/LegacyActivity.smali'
OLD = 'Lcom/shilapi/xcertplay/legacy/LegacyActivity;'
LOG = 'Lio/github/beidouxiaonan/carconnect/startup/StartupLog;'
ENTRY = 'io.github.beidouxiaonan.carconnect.startup.EntryActivity'
APP = 'io.github.beidouxiaonan.carconnect.startup.StartupApplication'

def require_base():
    assert hashlib.sha256(BASE.read_bytes()).hexdigest() == BASE_HASH, 'Expected published V1 SD8227 base'

def prepare():
    require_base(); WORK.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(BASE) as z:
        for name in ['classes.dex', 'classes3.dex']:
            (WORK / ('base-' + name)).write_bytes(z.read(name))

def rename_method(text, signature, renamed):
    pattern = r'^\.method [^\n]*' + re.escape(signature) + r'\n.*?^\.end method'
    matches = list(re.finditer(pattern, text, re.M | re.S)); assert len(matches) == 1, signature
    m = matches[0]; before = m[0]
    after = re.sub(r'^\.method [^\n]+\n', '.method private final ' + renamed + '\n', before, count=1)
    return text[:m.start()] + after + text[m.end():]

def patch():
    dex_path = Path('androidx/multidex/MultiDex.smali')
    multidex = (WORK / 'original' / dex_path).read_text(encoding='utf-8')
    old_call = 'invoke-static {p0, p2, p1}, Landroidx/multidex/MultiDex$V19;->install(Ljava/lang/ClassLoader;Ljava/util/List;Ljava/io/File;)V'
    new_call = 'invoke-static {p0, p2, p1}, Lio/github/beidouxiaonan/carconnect/startup/LegacyDexInstaller;->install(Ljava/lang/ClassLoader;Ljava/util/List;Ljava/io/File;)V'
    assert multidex.count(old_call) == 1 and 'LegacyDexInstaller' not in multidex
    target = WORK / 'modified-primary' / dex_path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(multidex.replace(old_call, new_call), encoding='utf-8', newline='\n')
    text = (WORK / 'original' / ACTIVITY).read_text(encoding='utf-8')
    assert 'sdOriginalOnCreate' not in text and 'sdStartupFailed' not in text
    assert not re.search(r'^\.method [^\n]*onResume\(\)V$', text, re.M)
    text = text.replace('# instance fields', '# instance fields\n.field private sdStartupFailed:Z', 1)
    assert '.field private sdStartupFailed:Z' in text
    text = rename_method(text, 'onCreate(Landroid/os/Bundle;)V', 'sdOriginalOnCreate(Landroid/os/Bundle;)V')
    text = rename_method(text, 'onStart()V', 'sdOriginalOnStart()V')
    text += f'''
.method protected onCreate(Landroid/os/Bundle;)V
    .registers 4
    :sd_create_begin
    const-string v1, "MAIN_CREATE_BEGIN"
    invoke-static {{p0, v1}}, {LOG}->stage(Landroid/content/Context;Ljava/lang/String;)V
    invoke-direct {{p0, p1}}, {OLD}->sdOriginalOnCreate(Landroid/os/Bundle;)V
    const-string v1, "MAIN_CREATE_OK"
    invoke-static {{p0, v1}}, {LOG}->stage(Landroid/content/Context;Ljava/lang/String;)V
    :sd_create_end
    return-void
    .catch Ljava/lang/Throwable; {{:sd_create_begin .. :sd_create_end}} :sd_create_failed
    :sd_create_failed
    move-exception v0
    const/4 v1, 0x1
    iput-boolean v1, p0, {OLD}->sdStartupFailed:Z
    invoke-super {{p0, p1}}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V
    invoke-static {{p0, v0}}, {LOG}->activityFailed(Landroid/app/Activity;Ljava/lang/Throwable;)V
    return-void
.end method

.method protected onStart()V
    .registers 3
    iget-boolean v0, p0, {OLD}->sdStartupFailed:Z
    if-eqz v0, :sd_start_begin
    invoke-super {{p0}}, Landroid/app/Activity;->onStart()V
    return-void
    :sd_start_begin
    const-string v1, "MAIN_START_BEGIN (includes service binding)"
    invoke-static {{p0, v1}}, {LOG}->stage(Landroid/content/Context;Ljava/lang/String;)V
    invoke-direct {{p0}}, {OLD}->sdOriginalOnStart()V
    const-string v1, "MAIN_START_OK"
    invoke-static {{p0, v1}}, {LOG}->stage(Landroid/content/Context;Ljava/lang/String;)V
    :sd_start_end
    return-void
    .catch Ljava/lang/Throwable; {{:sd_start_begin .. :sd_start_end}} :sd_start_failed
    :sd_start_failed
    move-exception v0
    const/4 v1, 0x1
    iput-boolean v1, p0, {OLD}->sdStartupFailed:Z
    invoke-static {{p0, v0}}, {LOG}->activityFailed(Landroid/app/Activity;Ljava/lang/Throwable;)V
    return-void
.end method

.method protected onResume()V
    .registers 2
    invoke-super {{p0}}, Landroid/app/Activity;->onResume()V
    iget-boolean v0, p0, {OLD}->sdStartupFailed:Z
    if-nez v0, :sd_resume_end
    invoke-static {{p0}}, {LOG}->ready(Landroid/content/Context;)V
    :sd_resume_end
    return-void
.end method
'''
    out = WORK / 'modified' / ACTIVITY; out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(text, encoding='utf-8')

def manifest(data):
    header_size = struct.unpack_from('<H', data, 2)[0]
    chunks = []; at = header_size
    while at < len(data):
        kind, header, size = struct.unpack_from('<HHI', data, at)
        assert size >= header and size > 0
        chunks.append((kind, data[at:at + size])); at += size
    names = pool.strings(next(c for k, c in chunks if k == 1))
    assert names.count('androidx.multidex.MultiDexApplication') == 1
    assert names.count('com.shilapi.xcertplay.legacy.LegacyActivity') == 1
    # Keep the original component registered for existing launcher shortcuts.
    replacements = {'androidx.multidex.MultiDexApplication': APP,
                    'Carplay-connect-0.1.4-beta-OEM-test': VERSION}
    entry_index = len(names)
    # Add one string without changing the indices of existing XML attributes.
    def extend_pool(chunk):
        n, styles, flags, start, style_start = struct.unpack_from('<IIIII', chunk, 8)
        assert styles == 0
        values = [replacements.get(x, x) for x in names] + [ENTRY]
        data_bytes = bytearray(); offsets = []
        for value in values:
            offsets.append(len(data_bytes)); utf16 = value.encode('utf-16le')
            if flags & 256:
                encoded = value.encode('utf-8'); data_bytes += pool._len8(len(utf16)//2) + pool._len8(len(encoded)) + encoded + b'\0'
            else: data_bytes += pool._len16(len(utf16)//2) + utf16 + b'\0\0'
        while len(data_bytes) % 4: data_bytes += b'\0'
        h = struct.unpack_from('<H', chunk, 2)[0]; result = bytearray(chunk[:h])
        new_start = h + len(values)*4
        struct.pack_into('<I', result, 4, new_start + len(data_bytes))
        struct.pack_into('<IIIII', result, 8, len(values), 0, flags & ~1, new_start, 0)
        return bytes(result) + struct.pack('<'+'I'*len(values), *offsets) + data_bytes
    def tag(chunk):
        return names[struct.unpack_from('<I', chunk, 20)[0]]
    def attrs(chunk):
        start, span, count = struct.unpack_from('<HHH', chunk, 24)
        return [(16 + start + i*span, names[struct.unpack_from('<I', chunk, 16 + start + i*span + 4)[0]]) for i in range(count)]
    result = []; stack = []; launcher_filter = []; original_activity = None
    capture = False; depth = 0; version_count = 0; app_count = 0; hardware_attr = None
    for kind, raw in chunks:
        chunk = bytearray(raw)
        if kind == 1: chunk = extend_pool(raw)
        if kind == 0x102:
            name = tag(raw); stack.append(name)
            if name == 'manifest':
                for pos, key in attrs(raw):
                    if key == 'versionCode':
                        assert struct.unpack_from('<I', raw, pos+16)[0] == 41
                        struct.pack_into('<I', chunk, pos+16, 48); version_count += 1
            if name == 'application':
                app_count += 1
                for pos,key in attrs(raw):
                    if key == 'hardwareAccelerated': hardware_attr = raw[pos:pos+20]
            if name == 'activity':
                for pos, key in attrs(raw):
                    if key == 'name' and names[struct.unpack_from('<I', raw, pos+16)[0]] == 'com.shilapi.xcertplay.legacy.LegacyActivity':
                        original_activity = bytes(raw)
            if name == 'intent-filter' and stack[-2] == 'activity' and original_activity is not None and not launcher_filter:
                capture = True; depth = len(stack)
        if capture:
            launcher_filter.append((kind, bytes(chunk)))
            if kind == 0x103 and len(stack) == depth: capture = False
        elif kind == 0x103 and tag(raw) == 'application':
            assert original_activity is not None and launcher_filter
            new_activity = bytearray(original_activity)
            for pos, key in attrs(original_activity):
                if key == 'name':
                    struct.pack_into('<I', new_activity, pos+8, entry_index)
                    struct.pack_into('<I', new_activity, pos+16, entry_index)
            assert hardware_attr is not None
            # Disable GL only on the fallback entry; CarPlay's TextureView stays accelerated.
            start,span,count = struct.unpack_from('<HHH',new_activity,24)
            assert span == 20 and len(new_activity) == 16+start+span*count
            attr = bytearray(hardware_attr); struct.pack_into('<I',attr,16,0)
            attributes = [bytes(new_activity[p:p+20]) for p,k in attrs(original_activity)] + [bytes(attr)]
            resource_map = next(c for k,c in chunks if k == 0x180)
            resource_ids = struct.unpack_from('<'+'I'*((len(resource_map)-8)//4),resource_map,8)
            attributes.sort(key=lambda a: resource_ids[struct.unpack_from('<I',a,4)[0]])
            new_activity = new_activity[:16+start] + b''.join(attributes)
            struct.pack_into('<I',new_activity,4,len(new_activity))
            struct.pack_into('<H',new_activity,28,count+1)
            result.append(bytes(new_activity)); result.extend(c for k,c in launcher_filter)
            # The same android namespace/name node closes the new Activity.
            end = next(c for k,c in chunks if k == 0x103 and tag(c) == 'activity')
            result.append(end); result.append(bytes(chunk))
        else: result.append(bytes(chunk))
        if kind == 0x103: stack.pop()
    assert version_count == app_count == 1 and not stack
    output = bytearray(data[:header_size]) + b''.join(result)
    struct.pack_into('<I', output, 4, len(output)); return bytes(output)

def repack():
    require_base()
    with zipfile.ZipFile(BASE) as src, zipfile.ZipFile(WORK/'unsigned.apk', 'w', allowZip64=False) as dst:
        for old in src.infolist():
            if signature_entry(old.filename): continue
            data = src.read(old.filename)
            if old.filename == 'classes.dex': data = (WORK/'patched-classes.dex').read_bytes()
            if old.filename == 'classes3.dex': data = (WORK/'patched-classes3.dex').read_bytes()
            if old.filename == 'AndroidManifest.xml': data = manifest(data)
            info = zipfile.ZipInfo(old.filename, (2026,10,7,0,0,0)); info.compress_type = old.compress_type
            dst.writestr(info, data)

def audit():
    require_base(); before, after = payload(BASE), payload(OUTPUT)
    assert before.keys() == after.keys()
    changed = {n for n in before if before[n] != after[n]}
    assert changed == {'AndroidManifest.xml', 'classes.dex', 'classes3.dex'}, changed
    assert after['AndroidManifest.xml'] == manifest(before['AndroidManifest.xml'])
    assert not signing_block_present(OUTPUT)
    assert {n for n in after if n.startswith('lib/')} == LIBS
    assert all(after[n][:8] == b'dex\n035\0' for n in after if n.endswith('.dex'))
    with zipfile.ZipFile(OUTPUT) as z:
        assert all(i.extract_version <= 20 for i in z.infolist())
        sf = [n for n in z.namelist() if n.endswith('.SF')]; assert len(sf) == 1
        assert b'X-Android-APK-Signed:' not in z.read(sf[0])
    sha = hashlib.sha256(OUTPUT.read_bytes()).hexdigest()
    OUTPUT.with_suffix('.apk.sha256').write_text(sha+'  '+OUTPUT.name+'\n',encoding='ascii')
    result = {'profile':'sd8227-multidex-test','baseSha256':BASE_HASH,'apkSha256':sha,
        'apkBytes':OUTPUT.stat().st_size,'versionCode':48,'minSdk':17,
        'changedEntries':sorted(changed),'unchangedAssetsAndNativeLibraries':True,
        'v1Only':True,'hardwareLaunch':'pending','rootCause':'device report: missing three-argument makeDexElements; adapter awaiting hardware retest'}
    OUTPUT.with_suffix('.audit.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(result,ensure_ascii=False,indent=2))

if __name__ == '__main__': globals()[sys.argv[1]]()
