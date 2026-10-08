import sys, struct, unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import startup, pool
from profile import payload

def xml(data):
    at=struct.unpack_from('<H',data,2)[0]; names=None; roots=[]; stack=[]
    while at<len(data):
        kind,header,size=struct.unpack_from('<HHI',data,at)
        assert size>=header and size>0
        chunk=data[at:at+size]
        if kind==1: names=pool.strings(chunk)
        elif kind==0x102:
            node={'tag':names[struct.unpack_from('<I',chunk,20)[0]],'attrs':{},'children':[]}
            start,span,count=struct.unpack_from('<HHH',chunk,24)
            for i in range(count):
                p=16+start+i*span; key=names[struct.unpack_from('<I',chunk,p+4)[0]]
                value=struct.unpack_from('<I',chunk,p+16)[0]
                if chunk[p+15]==3: value=names[value]
                node['attrs'][key]=value
            if stack: stack[-1]['children'].append(node)
            else: roots.append(node)
            stack.append(node)
        elif kind==0x103:
            assert stack.pop()['tag']==names[struct.unpack_from('<I',chunk,20)[0]]
        at+=size
    assert at==len(data) and not stack and len(roots)==1
    return roots[0]

class ManifestTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.before=xml(payload(startup.BASE)['AndroidManifest.xml'])
        cls.after=xml(startup.manifest(payload(startup.BASE)['AndroidManifest.xml']))
        cls.app=next(n for n in cls.after['children'] if n['tag']=='application')
    def test_launcher_is_unique_and_old_component_stays_registered(self):
        launcher=[]
        for activity in self.app['children']:
            for f in activity['children']:
                if f['tag']=='intent-filter' and any(n['attrs'].get('name')=='android.intent.category.LAUNCHER' for n in f['children']):
                    launcher.append(activity['attrs']['name'])
        self.assertEqual(launcher,[startup.ENTRY])
        self.assertEqual(self.app['attrs']['name'],startup.APP)
        self.assertTrue(any(n['attrs'].get('name')=='com.shilapi.xcertplay.legacy.LegacyActivity' for n in self.app['children']))
    def test_fallback_software_rendering_does_not_disable_carplay_texture(self):
        self.assertNotEqual(self.app['attrs']['hardwareAccelerated'],0)
        entry=next(n for n in self.app['children'] if n['attrs'].get('name')==startup.ENTRY)
        self.assertEqual(entry['attrs']['hardwareAccelerated'],0)
        original=next(n for n in self.app['children'] if n['attrs'].get('name')=='com.shilapi.xcertplay.legacy.LegacyActivity')
        self.assertNotIn('hardwareAccelerated',original['attrs'])
    def test_existing_services_permissions_and_sdk_are_untouched(self):
        self.assertEqual(self.after['attrs']['versionCode'],48)
        self.assertEqual(self.after['attrs']['package'],self.before['attrs']['package'])
        self.assertEqual([n for n in self.after['children'] if n['tag']!='application'],[n for n in self.before['children'] if n['tag']!='application'])
        oldapp=next(n for n in self.before['children'] if n['tag']=='application')
        self.assertEqual([n for n in self.app['children'] if n['tag']!='activity'],[n for n in oldapp['children'] if n['tag']!='activity'])

if __name__=='__main__': unittest.main()
