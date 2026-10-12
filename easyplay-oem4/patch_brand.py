from pathlib import Path
import json
import struct
import zipfile

VERSION='Carplay-connect-0.1.4-beta-OEM-test'
HOME='https://github.com/beidouxiaonan'

def patch_brand(work,original,write,edit_method,replace_method):
    name='com/shilapi/xcertplay/legacy/LegacyConnectionScreen.smali'
    text=original(name)
    def change(m):
        import re
        def constant(match):
            value=json.loads(match[2])
            if value.startswith('当前版本'):
                value='当前版本  '+VERSION
            else:
                value=value.replace('EasyPlay','CarConnect')
                if value in ('检查新版本','检查更新'):value='GitHub 发布与文档'
                if value in ('版本历史','版本记录'):value='GitHub 版本与说明'
                if value == '自动连接（无线模式重新打开应用会恢复）':value='自动连接（已保存手机，下次启动恢复）'
                if value == '选择已配对的 iPhone':value='选择 / 更换 iPhone（首次设置一次）'
            return match[1]+json.dumps(value,ensure_ascii=True)
        return re.sub(r'(?m)^(    const-string(?:/jumbo)? \w+, )(".*")$',constant,m)
    write(name,edit_method(text,'<init>(Landroid/content/Context;)V',change))
    name='com/shilapi/xcertplay/legacy/EasyPlayVersionSettings.smali'
    text=original(name)
    owner='Lcom/shilapi/xcertplay/legacy/EasyPlayVersionSettings;'
    for method in ['checkForUpdates','showVersionHistory','showUpdateSource','showAbout']:
        flags='private final' if method=='showUpdateSource' else 'public final'
        call='about' if method=='showAbout' else 'open'
        text=replace_method(text,method+'()V',f'''
.method {flags} {method}()V
    .registers 2
    iget-object v0, p0, {owner}->activity:Landroid/app/Activity;
    invoke-static {{v0}}, Lcom/shilapi/xcertplay/patch/CarConnectInfo;->{call}(Landroid/app/Activity;)V
    return-void
.end method
''')
    # All download actions use the same owner homepage, even for cached old update data.
    text=replace_method(text,'openDownload(Ljava/lang/String;)V',f'''
.method private final openDownload(Ljava/lang/String;)V
    .registers 3
    iget-object v0, p0, {owner}->activity:Landroid/app/Activity;
    invoke-static {{v0}}, Lcom/shilapi/xcertplay/patch/CarConnectInfo;->open(Landroid/app/Activity;)V
    return-void
.end method
''')
    text=replace_method(text,'updateUrl()Ljava/lang/String;',f'''
.method private final updateUrl()Ljava/lang/String;
    .registers 2
    const-string v0, "{HOME}"
    return-object v0
.end method
''')
    write(name,text)
    name='com/shilapi/xcertplay/legacy/LegacySessionService.smali'
    text=(work/'modified'/name).read_text(encoding='utf-8')
    # Only the report version string; package IDs and stored pairing identities stay compatible.
    text=text.replace(' 0.2.7-lynk-api19-preview36\\nAndroid ', ' '+VERSION+'\\nAndroid ')
    write(name,text)

def _len8(n):
    assert n<=32767
    return bytes([n]) if n<128 else bytes([(n>>8)|128,n&255])
def _len16(n):
    return struct.pack('<H',n) if n<32768 else struct.pack('<HH',(n>>16)|32768,n&65535)
def strings(chunk):
    count,styles,flags,start,styleStart=struct.unpack_from('<IIIII',chunk,8)
    header=struct.unpack_from('<H',chunk,2)[0]
    result=[]
    for index in range(count):
        off=start+struct.unpack_from('<I',chunk,header+index*4)[0]
        if flags&256:
            a=chunk[off];off+=2 if a&128 else 1
            n=chunk[off];off+=1
            if n&128:n=((n&127)<<8)|chunk[off];off+=1
            result.append(chunk[off:off+n].decode('utf-8'))
        else:
            n=struct.unpack_from('<H',chunk,off)[0];off+=2
            if n&32768:n=((n&32767)<<16)|struct.unpack_from('<H',chunk,off)[0];off+=2
            result.append(chunk[off:off+n*2].decode('utf-16le'))
    return result

def pool_replace(chunk,mapping):
    values=strings(chunk)
    changed=[mapping.get(s,s) for s in values]
    if values==changed:return chunk
    count,styles,flags,start,styleStart=struct.unpack_from('<IIIII',chunk,8)
    header=struct.unpack_from('<H',chunk,2)[0]
    assert styles==0, 'Brand pools must not contain spans'
    payload=bytearray(); offsets=[]
    for value in changed:
        offsets.append(len(payload));utf16=value.encode('utf-16le')
        if flags&256:
            encoded=value.encode('utf-8');payload+=_len8(len(utf16)//2)+_len8(len(encoded))+encoded+b'\0'
        else:payload+=_len16(len(utf16)//2)+utf16+b'\0\0'
    while len(payload)%4:payload+=b'\0'
    out=bytearray(chunk[:header]);newStart=header+count*4
    struct.pack_into('<I',out,4,newStart+len(payload))
    struct.pack_into('<III',out,16,flags&~1,newStart,0)
    return bytes(out)+struct.pack('<'+'I'*count,*offsets)+payload

def binary_replace(data,mapping,manifest=False):
    parent=bytearray(data[:struct.unpack_from('<H',data,2)[0]])
    at=len(parent);names=None;resource_ids=[]
    while at<len(data):
        kind,header,size=struct.unpack_from('<HHI',data,at);chunk=data[at:at+size]
        assert size>=header and size>0
        if kind==1:
            names=strings(chunk);chunk=pool_replace(chunk,mapping)
        elif manifest and kind==0x180:
            count=(size-header)//4
            resource_ids=list(struct.unpack_from('<%dI'%count,data,at+header))
        elif manifest and kind==0x102:
            chunk=bytearray(chunk)
            attrStart,attrSize,attrCount=struct.unpack_from('<HHH',chunk,24)
            for index in range(attrCount):
                pos=16+attrStart+index*attrSize
                nameIndex=struct.unpack_from('<I',chunk,pos+4)[0]
                if names[nameIndex]=='versionCode':
                    assert struct.unpack_from('<I',chunk,pos+16)[0]==36
                    struct.pack_into('<I',chunk,pos+16,41)
                elif nameIndex<len(resource_ids) and resource_ids[nameIndex]==0x0101020c:
                    assert struct.unpack_from('<I',chunk,pos+16)[0]==19
                    struct.pack_into('<I',chunk,pos+16,17)
            chunk=bytes(chunk)
        parent+=chunk;at+=size
    struct.pack_into('<I',parent,4,len(parent));return bytes(parent)

def payload(original_apk):
    with zipfile.ZipFile(original_apk) as z:
        replacements={
            'resources.arsc':binary_replace(z.read('resources.arsc'),{'EasyPlay':'CarConnect'}),
            'AndroidManifest.xml':binary_replace(z.read('AndroidManifest.xml'),{'0.2.7-lynk-api19-preview36':VERSION},True),
        }
    about=f'''<h2>CarConnect</h2><p>{VERSION}</p><p>Android 4.2~4.4 车机 CarPlay 测试版（minSdk 17）。</p>
<p>保留触摸队列优化、视频恢复、30/60fps 请求上限与独立导航/媒体音量。</p>
<p>支持 BC03 1.7.9 / 1.3.6 / gocsdk 原车蓝牙数据通道。首次选择并记住 iPhone，之后启动优先恢复已保存手机；连接诊断移到独立入口。默认关闭原车通道，无需 Root。用户已反馈其车机无线可连接；本次自动恢复改动尚待车机验证。</p>
<p><a href="{HOME}">项目主页、版本与使用说明</a></p>
<p>基于用户提供的 EasyPlay 0.2.7(36) APK 修改，参考 DiPlay / xcertplay。保留原项目许可证与第三方声明。仅供学习参考，禁止商业用途。</p>'''
    replacements['assets/easyplay/about.html']=about.encode('utf-8')
    replacements['assets/easyplay/releases.json']=json.dumps([{'versionCode':41,'versionName':VERSION,'publishedAt':'2026-10-06','notesHtml':f'<p>无线流程简化：记住原车已连接 iPhone，启动时直接从原车服务恢复选择；等待时保留记录，更换手机需明确选择。沿用 Android 4.2~4.4 兼容、视频/触摸优化与独立音量，诊断移到单独入口。本次恢复流程尚待车机验证。</p><p><a href="{HOME}">GitHub 主页与版本说明</a></p>'}],ensure_ascii=False,indent=2).encode('utf-8')
    return replacements

def repack(work,original_apk):
    changes=payload(original_apk);changes['classes3.dex']=(work/'patched-classes3.dex').read_bytes()
    def signing(name):
        upper=name.upper()
        return upper.startswith('META-INF/') and (upper.endswith(('.RSA','.DSA','.EC','.SF')) or upper=='META-INF/MANIFEST.MF')
    with zipfile.ZipFile(original_apk) as src,zipfile.ZipFile(work/'unsigned.apk','w') as dst:
        for info in src.infolist():
            if not signing(info.filename):dst.writestr(info,changes.get(info.filename,src.read(info.filename)))
    print('CarConnect APK branding: manifest, app label, about/history assets and classes3.dex updated')
