from pathlib import Path
import hashlib, importlib.util, struct, subprocess, zipfile
ROOT=Path(__file__).resolve().parent.parent
s=importlib.util.spec_from_file_location('patch',Path(__file__).with_name('patch_apk.py'));p=importlib.util.module_from_spec(s);s.loader.exec_module(p)
with zipfile.ZipFile(p.BASE) as old,zipfile.ZipFile(p.APK) as new:
 assert new.testzip() is None
 names={n for n in old.namelist() if not p.signing(n)}
 assert names=={n for n in new.namelist() if not p.signing(n)}
 changed={n for n in names if old.read(n)!=new.read(n)}
 assert changed=={'classes3.dex','AndroidManifest.xml','assets/easyplay/about.html','assets/easyplay/releases.json'},changed
 for name in ('classes.dex','classes2.dex','classes3.dex'):
  data=new.read(name);assert data[:8]==b'dex\n035\0'
  for offset in (64,80,88):assert struct.unpack_from('<I',data,offset)[0]<=65535
 libs=[n for n in names if n.startswith('lib/') and n.endswith('.so')]
 assert len(libs)==3 and all(n.startswith('lib/armeabi-v7a/') for n in libs)
 for name in libs:
  data=new.read(name);assert data[:5]==b'\x7fELF\x01' and struct.unpack_from('<H',data,18)[0]==40
 assert new.read('AndroidManifest.xml')==p.manifest(old.read('AndroidManifest.xml'))
 tree=subprocess.check_output([str(ROOT/'.tooling/build-tools/android-15/aapt.exe'),'dump','xmltree',str(p.APK),'AndroidManifest.xml']).decode('utf-8')
 block=tree[tree.index('E: provider'):];assert 'OemDiagnosticProvider' in block and 'android:exported(0x01010010)=(type 0x12)0x0' in block
 assert 'android:grantUriPermissions(0x0101001b)=(type 0x12)0xffffffff' in block
 # Existing nodes/permissions must remain identical, except version and appended provider.
 def chunks(data):
  at=8;values=None;result=[]
  while at<len(data):
   kind,header,size=struct.unpack_from('<HHI',data,at);c=data[at:at+size];at+=size
   if kind==1:values=p.brand.strings(c)
   elif kind==0x102:
    tag=values[struct.unpack_from('<I',c,20)[0]];start,span,count=struct.unpack_from('<HHH',c,24);attrs=[]
    for i in range(count):
     pos=16+start+i*span;ni=struct.unpack_from('<I',c,pos+4)[0];typ=c[pos+15];v=struct.unpack_from('<I',c,pos+16)[0]
     attrs.append((values[ni],typ,values[v] if typ==3 else v))
    result.append((tag,attrs))
  return result
 orig=chunks(old.read('AndroidManifest.xml'));current=chunks(new.read('AndroidManifest.xml'))
 manifest_attrs={n:v for n,t,v in current[0][1]}
 assert manifest_attrs['versionCode']==p.CODE and manifest_attrs['versionName']==p.VERSION
 assert manifest_attrs['package']=='com.shihab.diplay.legacy'
 sdk=[attrs for tag,attrs in current if tag=='uses-sdk'];assert len(sdk)==1
 assert dict((n,v) for n,t,v in sdk[0])['minSdkVersion']==17
 assert current[-1][0]=='provider'
 current=current[:-1]
 permissions=[node for node in current if node[0]=='uses-permission' and ('name',3,'android.permission.WRITE_EXTERNAL_STORAGE') in node[1]]
 assert permissions==[('uses-permission',[('name',3,'android.permission.WRITE_EXTERNAL_STORAGE'),('maxSdkVersion',16,22)])]
 permissions+= [node for node in current if node[0]=='uses-permission' and ('name',3,'android.permission.WAKE_LOCK') in node[1]]
 assert len(permissions)==2
 current=[node for node in current if node not in permissions]
 orig=[(tag,[(n,t,p.VERSION if n=='versionName' else p.CODE if n=='versionCode' else v) for n,t,v in attrs]) for tag,attrs in orig]
 assert current==orig,'Existing manifest semantics changed'
digest=hashlib.sha256(p.APK.read_bytes()).hexdigest();p.APK.with_suffix('.apk.sha256').write_text(digest+'  '+p.APK.name+'\n','utf-8')
print('Verified API17/ARMv7/DEX035, unchanged native/assets/AV payload, private report provider and original manifest semantics; SHA256',digest)
