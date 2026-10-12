from pathlib import Path
import hashlib, importlib.util, struct, zipfile
ROOT=Path(__file__).resolve().parent.parent
s=importlib.util.spec_from_file_location('release_patch',ROOT/'easyplay-release/patch_apk.py')
p=importlib.util.module_from_spec(s);s.loader.exec_module(p)
allowed={'classes3.dex','AndroidManifest.xml','resources.arsc','assets/easyplay/about.html','assets/easyplay/releases.json'}
with zipfile.ZipFile(p.BASE) as base,zipfile.ZipFile(p.APK) as out:
    assert out.testzip() is None
    names={n for n in base.namelist() if not p.signing(n)}
    assert names=={n for n in out.namelist() if not p.signing(n)}
    changed={n for n in names if base.read(n)!=out.read(n)}
    assert changed==allowed,changed
    for name in ['classes.dex','classes2.dex','classes3.dex']:
        dex=out.read(name);assert dex[:8]==b'dex\n035\0'
        for offset in [64,80,88]:assert struct.unpack_from('<I',dex,offset)[0]<=65535
    libs=[n for n in names if n.startswith('lib/') and n.endswith('.so')]
    assert len(libs)==3 and all(n.startswith('lib/armeabi-v7a/') for n in libs)
    for n in libs:
        data=out.read(n);assert data[:5]==b'\x7fELF\x01' and struct.unpack_from('<H',data,18)[0]==40
    assert out.read('AndroidManifest.xml')==p.manifest(base.read('AndroidManifest.xml'))
    assert out.read('resources.arsc')==p.brand.binary_replace(base.read('resources.arsc'),{'CarConnect':'CarConnect BC03'})
    assert p.VERSION.encode() in out.read('assets/easyplay/about.html')
    for name in ['WheelEventClock','WheelInputRecovery','MediaKeyRecovery','Bc03Profiles','OemSocketOutput','OemConnectDeadline','firstFrameAutoRetry','sppRecoveryPending','StableAudioTrack','FullscreenRecovery']:
        assert name.encode() in out.read('classes3.dex'),name
digest=hashlib.sha256(p.APK.read_bytes()).hexdigest()
p.APK.with_suffix('.apk.sha256').write_text(digest+'  '+p.APK.name+'\n',encoding='utf-8',newline='\n')
print('ZIP CRC, unchanged classes1/2/native assets, DEX035, ARMv7 ELF32, version and wheel hooks verified; SHA256',digest)
