"""Small overlay on the exact released 0.1.12 APK. Vendor inputs remain private."""
from pathlib import Path
import hashlib, importlib.util, json, re, struct, sys, zipfile
ROOT=Path(__file__).resolve().parent.parent
WORK=ROOT/'.private/apk-analysis/easyplay/integration-017'
BASE=ROOT/'artifacts/CarConnect-0.1.12-BC03-Unified-AV-Wheel-Android4.2plus-ARMv7.apk'
BASE_SHA='59e975fc42645a428f75c389fe70e4ce12634f08a650c26ec95258bbf9724c10'
OLD='CarConnect-0.1.12-BC03-Unified-AV-Wheel-Android4.2plus-ARMv7'
VERSION='CarConnect-0.1.17-BC03-Unified-FirstFrame30s-AllLogs-Android4.2plus-ARMv7'
APK=ROOT/'artifacts'/(VERSION+'.apk')
CODE=60
CLASSES=['patch/EnhancementPanel','patch/CarConnectInfo','patch/AvDiagnostics','patch/OemSppPause',
         'legacy/LegacyConnectionScreen','legacy/LegacySessionService']
P='Lcom/shilapi/xcertplay/patch/'
spec=importlib.util.spec_from_file_location('brand',ROOT/'easyplay-oem4/patch_brand.py')
brand=importlib.util.module_from_spec(spec);spec.loader.exec_module(brand)

def prepare():
    assert hashlib.sha256(BASE.read_bytes()).hexdigest()==BASE_SHA
    WORK.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(BASE) as z:(WORK/'base.dex').write_bytes(z.read('classes3.dex'))

def patch():
    for name in CLASSES:
        text=(WORK/'original/com/shilapi/xcertplay'/(name+'.smali')).read_text('utf-8')
        # All label updates are decoded constant strings; never arbitrary substitutions in code.
        def label(m):
            value=json.loads(m[2]).replace(OLD,VERSION).replace('CarConnect 0.1.12 · BC03/AV/Wheel','CarConnect 0.1.17 · BC03/30s/AllLogs')
            value=value.replace('CarConnect 0.1.5 AV 测试','CarConnect 0.1.17 音频与系统栏诊断')
            if name=='patch/OemSppPause' and value.startswith('SPP 状态未释放'):
                value='SPP 恢复未确认，本轮自动连接已暂停；原车恢复正常后可只读复查，或在无线设置中点击“允许一次恢复并连接”。仍失败请导出全部日志。已选 iPhone 保留。'
            if name=='patch/CarConnectInfo' and value.startswith(VERSION+'\n\n'):
                value=VERSION+'\n\nAndroid 4.2+/API17，ARMv7 32位测试包。保留 0.1.12 音频、全屏、方向盘和已核对的 BC03 1.7.2 / 1.7.9 / 1.3.6，含 1.3.6 AT+ 样本。无线控制启动后，前台且 Surface 有效时连续等待首帧 30 秒，未显示首帧才有限重连，最多两次；首帧显示即停止恢复。保留旧 socket 双向关闭、关闭后的协议数据隔离、恢复进度与互斥、具体取消原因。保留一次恢复并连接、全部日志和指定导出文件夹。包含可关闭的灭屏退出/亮屏复查恢复。适配依赖准确的 APK 和 gocsdk 指纹，不承诺全部同版本固件。\n\n休眠恢复默认关闭；开启后灭屏会停止 CarPlay 音乐。覆盖安装保留已选 iPhone。全部应用日志默认保存到 Download/CarConnect，可设置绝对文件夹路径，无需分享应用；选定目录失败会明确提示。此包仍需实车验证。热点接入不能替代蓝牙初始化。K2001N 有线重启、SD8227 专用启动问题不在本次修复范围。\n\n参考 EasyPlay / DiPlay / xcertplay，保留许可证和第三方声明。仅供学习参考，禁止商业用途。项目主页：https://github.com/beidouxiaonan'
            return m[1]+json.dumps(value,ensure_ascii=True)
        text=re.sub(r'(?m)^(    const-string(?:/jumbo)? \w+, )(".*")$',label,text)
        if name=='patch/EnhancementPanel':
            # The diagnostics layout is created as v0, with Activity p0. Same signature as existing hooks.
            marker='    invoke-static {p0, v0}, '+P+'AvDiagnostics;->append(Landroid/app/Activity;Landroid/widget/LinearLayout;)V'
            assert text.count(marker)==1
            text=text.replace(marker,marker+'\n\n    invoke-static {p0, v0}, '+P+'ConnectionDiagnostics;->add(Landroid/app/Activity;Landroid/widget/LinearLayout;)V')
        if name=='legacy/LegacySessionService':
            marker='    invoke-super {p0}, Landroid/app/Service;->onCreate()V'
            assert text.count(marker)==1
            text=text.replace(marker,marker+'\n\n    invoke-static {p0}, '+P+'StandbyRecovery;->created(Lcom/shilapi/xcertplay/legacy/LegacySessionService;)V')
            marker='.method public onDestroy()V'
            pos=text.index(marker);end=text.index('\n',text.index('    .registers ',pos))
            text=text[:end]+ '\n\n    invoke-static {p0}, '+P+'StandbyRecovery;->destroyed(Lcom/shilapi/xcertplay/legacy/LegacySessionService;)V'+text[end:]
        target=WORK/'modified/com/shilapi/xcertplay'/(name+'.smali')
        target.parent.mkdir(parents=True,exist_ok=True);target.write_text(text,'utf-8',newline='\n')

def pool(values,utf8):
    payload=bytearray();offsets=[]
    for value in values:
        offsets.append(len(payload));u16=value.encode('utf-16le')
        if utf8:
            u8=value.encode();payload+=brand._len8(len(u16)//2)+brand._len8(len(u8))+u8+b'\0'
        else:payload+=brand._len16(len(u16)//2)+u16+b'\0\0'
    payload+=b'\0'*(-len(payload)%4);start=28+len(values)*4
    return struct.pack('<HHIIIIII',1,28,start+len(payload),len(values),0,256 if utf8 else 0,start,0)+struct.pack('<'+'I'*len(offsets),*offsets)+payload

def manifest(data):
    data=brand.binary_replace(data,{OLD:VERSION})
    chunks=[];at=8;names=None;ids=None;pool_i=map_i=None
    while at<len(data):
        kind,header,size=struct.unpack_from('<HHI',data,at);chunk=bytearray(data[at:at+size]);at+=size
        if kind==1:pool_i=len(chunks);names=brand.strings(chunk);utf8=bool(struct.unpack_from('<I',chunk,16)[0]&256)
        if kind==0x180:map_i=len(chunks);ids=list(struct.unpack_from('<'+'I'*((size-header)//4),chunk,header))
        if kind==0x102:
            start,span,count=struct.unpack_from('<HHH',chunk,24)
            for i in range(count):
                pos=16+start+i*span
                if names[struct.unpack_from('<I',chunk,pos+4)[0]]=='versionCode':
                    assert struct.unpack_from('<I',chunk,pos+16)[0]==55;struct.pack_into('<I',chunk,pos+16,CODE)
        chunks.append(bytes(chunk))
    assert names is not None and ids is not None
    def index(value):
        if value not in names:names.append(value)
        return names.index(value)
    # IDs verified against the bundled API17 android.R.attr constants.
    attrs={'name':16842755,'exported':16842768,'authorities':16842776,'grantUriPermissions':16842779}
    provider=index('provider');ns=index('http://schemas.android.com/apk/res/android')
    class_i=index('com.shilapi.xcertplay.patch.OemDiagnosticProvider')
    authority_i=index('com.shihab.diplay.legacy.carconnect.diagnostics')
    pairs=[('name',3,class_i),('exported',18,0),('authorities',3,authority_i),('grantUriPermissions',18,0xffffffff)]
    body=bytearray()
    for name,typ,value in pairs:
        ni=index(name)
        if len(ids)<=ni:ids.extend([0]*(ni+1-len(ids)))
        assert ids[ni] in (0,attrs[name]);ids[ni]=attrs[name]
        body+=struct.pack('<IIIHBBI',ns,ni,value if typ==3 else 0xffffffff,8,0,typ,value)
    start=struct.pack('<HHIII',0x102,16,36+len(body),0,0xffffffff)+struct.pack('<IIHHHHHH',0xffffffff,provider,20,20,4,0,0,0)+body
    end=struct.pack('<HHIIIII',0x103,16,24,0,0xffffffff,0xffffffff,provider)
    # API17-22 need install-time storage permission for a user-accessible Downloads file.
    # API23+ use app-specific external storage; do not introduce a runtime permission flow.
    permission=index('uses-permission');permission_value=index('android.permission.WRITE_EXTERNAL_STORAGE')
    permission_body=bytearray()
    for name,typ,value in [('name',3,permission_value),('maxSdkVersion',16,22)]:
        ni=index(name);rid=16842755 if name=='name' else 16843377
        if len(ids)<=ni:ids.extend([0]*(ni+1-len(ids)))
        assert ids[ni] in (0,rid);ids[ni]=rid
        permission_body+=struct.pack('<IIIHBBI',ns,ni,value if typ==3 else 0xffffffff,8,0,typ,value)
    ps=struct.pack('<HHIII',0x102,16,36+len(permission_body),0,0xffffffff)+struct.pack('<IIHHHHHH',0xffffffff,permission,20,20,2,0,0,0)+permission_body
    pe=struct.pack('<HHIIIII',0x103,16,24,0,0xffffffff,0xffffffff,permission)
    wakelock=index('android.permission.WAKE_LOCK')
    wb=struct.pack('<IIIHBBI',ns,index('name'),wakelock,8,0,3,wakelock)
    ws=struct.pack('<HHIII',0x102,16,36+len(wb),0,0xffffffff)+struct.pack('<IIHHHHHH',0xffffffff,permission,20,20,1,0,0,0)+wb
    chunks[pool_i]=pool(names,utf8);chunks[map_i]=struct.pack('<HHI',0x180,8,8+len(ids)*4)+struct.pack('<'+'I'*len(ids),*ids)
    insert=[i for i,c in enumerate(chunks) if struct.unpack_from('<H',c)[0]==0x103 and names[struct.unpack_from('<I',c,20)[0]]=='application']
    assert len(insert)==1
    chunks[insert[0]:insert[0]]=[start,end]
    insert=[i for i,c in enumerate(chunks) if struct.unpack_from('<H',c)[0]==0x102 and names[struct.unpack_from('<I',c,20)[0]]=='application']
    assert len(insert)==1
    chunks[insert[0]:insert[0]]=[ps,pe,ws,pe]
    out=data[:8]+b''.join(chunks);out=bytearray(out);struct.pack_into('<I',out,4,len(out));return bytes(out)

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
                data=data.decode().replace(OLD,VERSION).replace('</p>','</p><p>0.1.17 BC03 统一适配与 30 秒首帧等待：将原车无线首帧等待从 60 秒缩短为 30 秒，最多重连两次；保留旧 socket 双向关闭与关闭后数据隔离。手动恢复显示进度并阻止新连接抢占，记录具体取消原因。保留一次恢复和全部日志指定文件夹导出。包含可关闭的休眠复查。兼容已核对的 BC03 样本，含所提供的 1.3.6 AT+；保留音频、全屏和方向盘。仍需实车验证。</p>',1).encode()
            elif info.filename=='assets/easyplay/releases.json':
                entries=json.loads(data);entries.insert(0,{'versionCode':CODE,'versionName':VERSION,'publishedAt':'2026-10-12','notesHtml':'<p>兼容已核对 BC03 1.7.2 / 1.7.9 / 1.3.6 样本，原车无线首帧等待 30 秒，最多重连两次。保留允许一次恢复并连接、恢复来源记录、全部应用日志导出和自定义保存文件夹。保留休眠、音频、全屏和方向盘。测试包，尚未实车验证。</p>'});data=json.dumps(entries,ensure_ascii=False,indent=2).encode()
            dst.writestr(info,data)
if __name__=='__main__':{'prepare':prepare,'patch':patch,'repack':repack}[sys.argv[1]]()
