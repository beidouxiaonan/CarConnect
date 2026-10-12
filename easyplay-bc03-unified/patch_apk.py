"""BC03 1.7.2 integration on released OEM 0.1.10; generated OEM smali remains private."""
from pathlib import Path
import hashlib,importlib.util,json,struct,sys,zipfile
ROOT=Path(__file__).resolve().parent.parent
WORK=ROOT/'.private/apk-analysis/easyplay/bc03-unified'
BASE=ROOT/'artifacts/CarConnect-0.1.10-beta-OEM-SPP-guard-test-Android4.2-4.4.apk'
BASE_HASH='c024cf21fb925fbf465ceeeb4031592559e0bbb5d4b738e1f0de1f8b702af8f7'
OLD_VERSION='Carplay-connect-0.1.10-beta-OEM-SPP-guard-test'
VERSION='Carplay-connect-0.1.11-beta-BC03-unified-test'
CODE=54
APK=ROOT/'artifacts/CarConnect-0.1.11-beta-BC03-unified-test-Android4.2-4.4.apk'
AV_BASE=ROOT/'artifacts/CarConnect-0.1.8-beta-BC03-1.7.2-SPP-native-recovery-test.apk'
AV_HASH='3824782100cf0d14545c300ba8c4ad749966454e33fd6628d3093ff060e7f2d5'
INPUTS={
    'bc03-172/input.apk':'32b25ec4eacada6bad179e4015f1c9ae8f9ce4df4fee9ee8baf6e99a781ede58',
    'bc03-179/input.apk':'079e6446c475eaf7d067853cf525feedbf9a9619408db6df83d873c200616326',
    'bc03/input.apk':'40c9cd2efd64a573303cb8bd1cf9061ab0f8f43cf161bddf4fb483f617e69349',
    'bc03-172/gocsdk':'3e8f5687b69623087145177358c2104aeae50f2eb943db8711af983e395ced60'
}
CLASSES=['com/shilapi/xcertplay/'+x for x in ['patch/EnhancementPanel','patch/CarConnectInfo','legacy/LegacyConnectionScreen','legacy/LegacySessionService']]
spec=importlib.util.spec_from_file_location('brand',ROOT/'easyplay-oem4/patch_brand.py')
brand=importlib.util.module_from_spec(spec);spec.loader.exec_module(brand)
def prepare():
    assert hashlib.sha256(BASE.read_bytes()).hexdigest()==BASE_HASH,'Unexpected OEM 0.1.10 baseline'
    for name,digest in INPUTS.items():
        path=ROOT/'.private/vendor-analysis'/name
        assert hashlib.sha256(path.read_bytes()).hexdigest()==digest,'Missing or unverified input: '+name
    WORK.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(BASE) as z:(WORK/'base.dex').write_bytes(z.read('classes3.dex'))
    assert hashlib.sha256(AV_BASE.read_bytes()).hexdigest()==AV_HASH,'Unexpected 1.7.2 AV reference release'
    with zipfile.ZipFile(AV_BASE) as z:(WORK/'av-base.dex').write_bytes(z.read('classes3.dex'))
def patch():
    for name in CLASSES:
        text=(WORK/'original'/(name+'.smali')).read_text(encoding='utf-8')
        text=text.replace(OLD_VERSION,VERSION).replace('CarConnect 0.1.10 beta','CarConnect 0.1.11 beta (BC03 unified)')
        text=text.replace('BC03 1.7.9 / 1.3.6','BC03 1.7.2 / 1.7.9 / 1.3.6')
        target=WORK/'modified'/(name+'.smali');target.parent.mkdir(parents=True,exist_ok=True)
        target.write_text(text,encoding='utf-8',newline='\n')
def manifest(data):
    data=bytearray(brand.binary_replace(data,{OLD_VERSION:VERSION}));at=struct.unpack_from('<H',data,2)[0];names=None;changed=0
    while at<len(data):
        kind,header,size=struct.unpack_from('<HHI',data,at)
        if kind==1:names=brand.strings(data[at:at+size])
        elif kind==0x102:
            start,span,count=struct.unpack_from('<HHH',data,at+24)
            for i in range(count):
                pos=at+16+start+i*span
                if names[struct.unpack_from('<I',data,pos+4)[0]]=='versionCode':
                    assert struct.unpack_from('<I',data,pos+16)[0]==52;struct.pack_into('<I',data,pos+16,CODE);changed+=1
        at+=size
    assert changed==1;return bytes(data)
def signing(name):
    n=name.upper();return n.startswith('META-INF/') and (n.endswith(('.RSA','.DSA','.EC','.SF')) or n=='META-INF/MANIFEST.MF')
def repack():
    with zipfile.ZipFile(BASE) as src,zipfile.ZipFile(WORK/'unsigned.apk','w') as dst:
        for info in src.infolist():
            if signing(info.filename):continue
            data=src.read(info.filename)
            if info.filename=='classes3.dex':data=(WORK/'patched.dex').read_bytes()
            elif info.filename=='AndroidManifest.xml':data=manifest(data)
            elif info.filename=='assets/easyplay/about.html':
                data=data.decode().replace(OLD_VERSION,VERSION).replace('BC03 1.7.9 / 1.3.6','BC03 1.7.2 / 1.7.9 / 1.3.6')
                data=data.replace('</p>','</p><p>BC03 统一适配：核对 1.7.2 / 1.7.9 / 1.3.6 的版本、APK 和 gocsdk 指纹，自动选择接入流程。修正本地 socket 发送排空等待；其他固件未验证。K2001N 有线重启未解决，请用无线。尚待实车验证。</p>',1).encode()
            elif info.filename=='assets/easyplay/releases.json':
                old=json.loads(data);old.insert(0,{'versionCode':CODE,'versionName':VERSION,'publishedAt':'2026-10-10',
                    'notesHtml':'<p>BC03 统一包：1.7.2 / 1.7.9 / 1.3.6 按精确文件及接口分流；修正 socket flush 排空等待和分阶段超时。保留清理保护、首帧恢复、手机记录、音频与全屏优化。其他版本尚未核对，实车待验证。</p>'})
                data=json.dumps(old,ensure_ascii=False,indent=2).encode()
            dst.writestr(info,data)
if __name__=='__main__':{'prepare':prepare,'patch':patch,'repack':repack}[sys.argv[1]]()
