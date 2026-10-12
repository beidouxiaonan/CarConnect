"""Small hooks on the exact released OEM 0.1.8 APK. Generated OEM smali stays private."""
from pathlib import Path
import hashlib, importlib.util, json, re, struct, sys, zipfile
ROOT=Path(__file__).resolve().parent.parent
WORK=ROOT/'.private/apk-analysis/easyplay/oem-first-frame'
BASE=ROOT/'artifacts/CarConnect-0.1.8-beta-OEM-SPP-native-recovery-test-Android4.2-4.4.apk'
BASE_HASH='75133d5340d7cc7c7fc6ad50ae4ad0b1b9e40b031390243d5937cbd1742cb856'
OLD_VERSION='Carplay-connect-0.1.8-beta-OEM-SPP-native-recovery-test'
VERSION='Carplay-connect-0.1.9-beta-OEM-first-frame-diagnostics-test'
CODE=51
APK=ROOT/'artifacts/CarConnect-0.1.9-beta-OEM-first-frame-diagnostics-test-Android4.2-4.4.apk'
CLASSES=['com/shilapi/xcertplay/'+x for x in ['patch/EnhancementPanel','patch/CarConnectInfo','legacy/LegacyConnectionScreen','legacy/LegacySessionService']]
spec=importlib.util.spec_from_file_location('brand',ROOT/'easyplay-oem4/patch_brand.py')
brand=importlib.util.module_from_spec(spec);spec.loader.exec_module(brand)
P='Lcom/shilapi/xcertplay/patch/'
S='Lcom/shilapi/xcertplay/legacy/LegacySessionService;'
STATUS_SIG='startSession$lambda$46$lambda$43$lambda$39('+S+'IZLjava/lang/String;Lcom/shilapi/xcertplay/orchestration/CarPlayStatus;)Lkotlin/Unit;'
def prepare():
    assert hashlib.sha256(BASE.read_bytes()).hexdigest()==BASE_HASH,'Unexpected OEM baseline'
    WORK.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(BASE) as z:(WORK/'base.dex').write_bytes(z.read('classes3.dex'))
def edit(text,sig,anchor,insert):
    pattern=r'(?ms)^\.method[^\n]* '+re.escape(sig)+r'\n.*?^\.end method'
    matches=list(re.finditer(pattern,text));assert len(matches)==1,sig
    m=matches[0];body=m.group();assert body.count(anchor)==1,(sig,anchor)
    return text[:m.start()]+body.replace(anchor,anchor+'\n\n'+insert,1)+text[m.end():]
def beginning(text,sig,insert):
    pattern=r'(?ms)(^\.method[^\n]* '+re.escape(sig)+r'\n\s*\.registers [0-9]+)'
    out,n=re.subn(pattern,lambda m:m[1]+'\n\n'+insert,text);assert n==1,sig
    return out
def patch():
    for name in CLASSES:
        text=(WORK/'original'/(name+'.smali')).read_text(encoding='utf-8')
        text=text.replace(OLD_VERSION,VERSION).replace('CarConnect 0.1.8 beta','CarConnect 0.1.9 beta')
        if name.endswith('/EnhancementPanel'):
            text=edit(text,'attach(Landroid/app/Activity;Landroid/widget/LinearLayout;)V',
                '    invoke-static {p0}, '+P+'EnhancementPrefs;->init(Landroid/content/Context;)V',
                '    invoke-static {p0}, '+P+'FirstFrameRecovery;->install(Landroid/app/Activity;)V')
            anchor='    invoke-static {p0, v0}, '+P+'SppCleanupSettings;->add(Landroid/app/Activity;Landroid/widget/LinearLayout;)V'
            text=edit(text,'show(Landroid/app/Activity;)V',anchor,
                '    invoke-static {p0, v0}, '+P+'FirstFrameSettings;->add(Landroid/app/Activity;Landroid/widget/LinearLayout;)V')
        if name.endswith('/LegacySessionService'):
            anchor='    if-ne v0, p1, :cond_40'
            text=edit(text,STATUS_SIG,anchor,'    invoke-static {p0, p1, p4}, '+P+'FirstFrameRecovery;->status('+S+'ILcom/shilapi/xcertplay/orchestration/CarPlayStatus;)V')
            text=beginning(text,'stopSession(ZZ)V','    invoke-static {p0, p2}, '+P+'FirstFrameRecovery;->stopped('+S+'Z)V')
            text=beginning(text,'onDestroy()V','    invoke-static {p0}, '+P+'FirstFrameRecovery;->destroyed('+S+')V')
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
                    assert struct.unpack_from('<I',data,pos+16)[0]==49;struct.pack_into('<I',data,pos+16,CODE);changed+=1
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
                data=data.decode().replace(OLD_VERSION,VERSION).replace('</p>','</p><p>首帧恢复与 SPP 观察测试：前台连续 60 秒无首帧，最多重连两次；SPP 冷却改为同任务等待，不反复发送命令。仍占用时停止并记录回执证据。</p>',1).encode()
            elif info.filename=='assets/easyplay/releases.json':
                old=json.loads(data);old.insert(0,{'versionCode':CODE,'versionName':VERSION,'publishedAt':'2026-10-09',
                    'notesHtml':'<p>原车无线无首帧自动恢复（前台60秒/最多2次），保留已选iPhone。清理冷却改为可取消等待，不重复发送VH/VF；增加原车SPP回执/缓存观察。保留占用校验，不保证所有固件自动释放。待实车验证。</p>'})
                data=json.dumps(old,ensure_ascii=False,indent=2).encode()
            dst.writestr(info,data)
if __name__=='__main__':{'prepare':prepare,'patch':patch,'repack':repack}[sys.argv[1]]()
