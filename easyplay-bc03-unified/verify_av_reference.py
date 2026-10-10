"""Verify actual AV integration against the user's requested released 0.1.5 APK."""
from pathlib import Path
import hashlib,subprocess,sys,zipfile
ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'.private/apk-analysis/easyplay/bc03-unified'
REFERENCE=ROOT/'artifacts/CarConnect-0.1.5-beta-AV-test-Android4.2-4.4.apk'
REFERENCE_SHA='46b756fe289e47b3d41bec1a4d79da3c4fe752f3ece6a797ffca812d7216d1e8'
assert len(sys.argv)==2,'Provide the final classes3.dex to compare'
assert hashlib.sha256(REFERENCE.read_bytes()).hexdigest()==REFERENCE_SHA,'Unexpected requested AV release'
WORK.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(REFERENCE) as z:
    assert z.testzip() is None
    (WORK/'av5-release.dex').write_bytes(z.read('classes3.dex'))
subprocess.run([str(ROOT/'.tooling/jdk25/bin/java.exe'),'-cp',str(ROOT/'.tooling/jadx/lib/jadx-1.5.6-all.jar'),
                str(ROOT/'easyplay-bc03-unified/tools/AvReferenceAudit.java'),str(WORK/'av5-release.dex'),str(Path(sys.argv[1]).resolve())],check=True)
