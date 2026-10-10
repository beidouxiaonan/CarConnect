"""Two exact hooks on released OEM 0.1.9; generated OEM smali remains private."""
from pathlib import Path
import hashlib,importlib.util,json,re,struct,sys,zipfile
ROOT=Path(__file__).resolve().parent.parent
WORK=ROOT/'.private/apk-analysis/easyplay/oem-spp-guard'
BASE=ROOT/'artifacts/CarConnect-0.1.9-beta-OEM-first-frame-diagnostics-test-Android4.2-4.4.apk'
BASE_HASH='cddbcfc1ec88d201dd0e42505caea7c4ccedd90edae2fd43c9e859c922351452'
OLD_VERSION='Carplay-connect-0.1.9-beta-OEM-first-frame-diagnostics-test'
VERSION='Carplay-connect-0.1.10-beta-OEM-SPP-guard-test'
CODE=52
APK=ROOT/'artifacts/CarConnect-0.1.10-beta-OEM-SPP-guard-test-Android4.2-4.4.apk'
CLASSES=['com/shilapi/xcertplay/'+x for x in ['patch/EnhancementPanel','patch/CarConnectInfo','legacy/LegacyConnectionScreen','legacy/LegacySessionService']]
spec=importlib.util.spec_from_file_location('brand',ROOT/'easyplay-oem4/patch_brand.py')
brand=importlib.util.module_from_spec(spec);spec.loader.exec_module(brand)
P='Lcom/shilapi/xcertplay/patch/'
S='Lcom/shilapi/xcertplay/legacy/LegacySessionService;'
STATUS_SIG='startSession$lambda$46$lambda$43$lambda$39('+S+'IZLjava/lang/String;Lcom/shilapi/xcertplay/orchestration/CarPlayStatus;)Lkotlin/Unit;'
def prepare():
    assert hashlib.sha256(BASE.read_bytes()).hexdigest()==BASE_HASH,'Unexpected OEM 0.1.9 baseline'
    WORK.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(BASE) as z:(WORK/'base.dex').write_bytes(z.read('classes3.dex'))
def replace_method(text,sig,old,new):
    pattern=r'(?ms)^\.method[^\n]* '+re.escape(sig)+r'\n.*?^\.end method'
    matches=list(re.finditer(pattern,text));assert len(matches)==1,sig
    m=matches[0];body=m.group();assert body.count(old)==1,(sig,old)
    return text[:m.start()]+body.replace(old,new,1)+text[m.end():]
def patch():
    for name in CLASSES:
        text=(WORK/'original'/(name+'.smali')).read_text(encoding='utf-8')
        text=text.replace(OLD_VERSION,VERSION).replace('CarConnect 0.1.9 beta','CarConnect 0.1.10 beta')
        if name.endswith('/EnhancementPanel'):
            text=replace_method(text,'show(Landroid/app/Activity;)V',
                '    invoke-static {p0, v0}, '+P+'SppCleanupSettings;->add(Landroid/app/Activity;Landroid/widget/LinearLayout;)V',
                '    invoke-static {p0, v0}, '+P+'SppRecoverySettings;->add(Landroid/app/Activity;Landroid/widget/LinearLayout;)V')
        if name.endswith('/LegacySessionService'):
            text=replace_method(text,STATUS_SIG,
                '    invoke-virtual {v3, p0, p0}, '+S+'->stopSession(ZZ)V',
                '    invoke-static {v3}, '+P+'OemSppPause;->failed('+S+')V')
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
                    assert struct.unpack_from('<I',data,pos+16)[0]==51;struct.pack_into('<I',data,pos+16,CODE);changed+=1
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
                data=data.decode().replace(OLD_VERSION,VERSION).replace('</p>','</p><p>SPP 重试保护：空闲时正常连接；未确认释放则暂停本轮自动重试并保存标记，底层恢复独立开关默认关闭。保留已选手机。不能保证修复原车缓存。</p>',1).encode()
            elif info.filename=='assets/easyplay/releases.json':
                old=json.loads(data);old.insert(0,{'versionCode':CODE,'versionName':VERSION,'publishedAt':'2026-10-10',
                    'notesHtml':'<p>停止 SPP 清理失败后的循环断开，未完成标记跨进程保存；底层 VH 默认关闭并单独控制。确认空闲后正常连接，不跳过占用校验。保留 OEM 0.1.9 首帧恢复和手机记录。待实车验证。</p>'})
                data=json.dumps(old,ensure_ascii=False,indent=2).encode()
            dst.writestr(info,data)
if __name__=='__main__':{'prepare':prepare,'patch':patch,'repack':repack}[sys.argv[1]]()
