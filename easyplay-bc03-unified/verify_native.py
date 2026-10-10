"""Verify the exact supplied BC03 1.7.2 daemon and its managed-SPP VH mapping."""
from pathlib import Path
import hashlib,struct
p=Path(__file__).resolve().parents[1]/'.private/vendor-analysis/bc03-172/gocsdk'
b=p.read_bytes()
assert hashlib.sha256(b).hexdigest()=='3e8f5687b69623087145177358c2104aeae50f2eb943db8711af983e395ced60'
u=lambda at:struct.unpack_from('<I',b,at)[0]
assert [u(at) for at in (0xf22a8,0xf22ac,0xf22b0)]==[0xded0d,0xded10,0xded13]
assert [b[v:v+3] for v in (0xded0d,0xded10,0xded13)]==[b'VF\0',b'VG\0',b'VH\0']
assert [u(at) for at in (0xf1f1c,0xf1f20,0xf1f24)]==[0xa0595,0xa0765,0xa0869]
assert b[0xa0886:0xa0898]==bytes.fromhex('24784cb920460134c3f7fffae4b2092cf8d1')
assert b[0xa088e:0xa0892]==bytes.fromhex('c3f7fffa')
assert b[0xa08bc:0xa08c0]==bytes.fromhex('c3f7e8fa')
# Registration reads only the missing bytes of the 12-byte MAC. Later protocol
# bytes stay queued; removing output-drain polling does not change that boundary.
assert b[0xf9a4:0xf9a8]==bytes.fromhex('c3f10c02')
assert b[0xf9ae:0xf9b2]==bytes.fromhex('fcf760e9')
assert b[0xf9c0:0xf9c2]==bytes.fromhex('0b28')
print('Verified exact daemon hash, VH mapping and 12-byte socket greeting read boundary')
