"""Repackage the published 0.1.4 payload as a classic ZIP for a V1-only test.

This is an installation-format experiment, not a native-library port. No DEX,
manifest, resources, saved-phone behavior, or authentication asset is patched.
"""
from pathlib import Path
import argparse
import hashlib
import json
import struct
import zipfile

BASE_SHA256 = 'dfccdeb5172d61840d5323a1857e3fe280d8b6c29ef30fe3ab6b937cb0871312'
LIBS = {'lib/armeabi-v7a/' + name for name in (
    'libdiplay_crypto.so', 'libdiplay_lwip.so', 'libdiplay_opus.so')}


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def signature_entry(name):
    upper = name.upper()
    return upper.startswith('META-INF/') and (
        upper.endswith(('.SF', '.RSA', '.DSA', '.EC')) or
        upper == 'META-INF/MANIFEST.MF')


def payload(path):
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)):
            raise ValueError('Duplicate ZIP entries')
        if archive.testzip() is not None:
            raise ValueError('ZIP CRC check failed')
        for info in archive.infolist():
            if info.flag_bits & 1 or info.compress_type not in (0, 8):
                raise ValueError('Encrypted or unsupported compression: ' + info.filename)
        return {name: archive.read(name) for name in names if not signature_entry(name)}


def require_base(path):
    if digest(path) != BASE_SHA256:
        raise ValueError('Input must be the published CarConnect 0.1.4 OEM-test APK')


def repack(base, output):
    require_base(base)
    entries = payload(base)
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(base) as source, zipfile.ZipFile(output, 'w', allowZip64=False) as target:
        for old in source.infolist():
            if old.filename not in entries:
                continue
            info = zipfile.ZipInfo(old.filename, (2026, 10, 6, 0, 0, 0))
            info.compress_type = old.compress_type
            info.create_system = 0
            # Rebuild local/central headers and remove old alignment extras;
            # zipalign runs after this step and before signing.
            info.extra = b''
            info.comment = b''
            target.writestr(info, entries[old.filename])
    if payload(output) != entries:
        raise ValueError('Repacking changed the application payload')


def signing_block_present(path):
    data = Path(path).read_bytes()
    end = data.rfind(b'PK\x05\x06', max(0, len(data) - 65557))
    if end < 0 or end + 22 > len(data):
        raise ValueError('Missing ZIP end record')
    count, directory_size, directory_at, comment_len = struct.unpack_from('<HIIH', data, end + 10)
    if end + 22 + comment_len != len(data):
        raise ValueError('Trailing or truncated ZIP data')
    if count == 65535 or directory_at == 0xffffffff or directory_size == 0xffffffff:
        raise ValueError('ZIP64 is outside the legacy profile')
    if directory_at + directory_size != end:
        raise ValueError('Unexpected central-directory layout')
    return data[max(0, directory_at - 16):directory_at] == b'APK Sig Block 42'


def audit(base, output):
    require_base(base)
    before, after = payload(base), payload(output)
    if before != after:
        raise ValueError('Payload differs from the published 0.1.4 APK')
    if signing_block_present(output):
        raise ValueError('V2/V3 signing block must be absent')
    native = {name for name in after if name.startswith('lib/') and name.endswith('.so')}
    if native != LIBS:
        raise ValueError('Unexpected ABI/native-library set')
    for name in native:
        elf = after[name]
        if elf[:5] != b'\x7fELF\x01' or elf[5] != 1 or struct.unpack_from('<H', elf, 18)[0] != 40:
            raise ValueError('Expected ELF32 little-endian ARM: ' + name)
    dex = [name for name in after if name.endswith('.dex')]
    if any(after[name][:8] != b'dex\n035\x00' for name in dex):
        raise ValueError('Unexpected DEX version')
    with zipfile.ZipFile(output) as archive:
        sf = [name for name in archive.namelist() if name.upper().endswith('.SF')]
        if len(sf) != 1 or 'META-INF/MANIFEST.MF' not in archive.namelist():
            raise ValueError('Expected one V1 signer')
        if b'X-Android-APK-Signed:' in archive.read(sf[0]):
            raise ValueError('V1 signature still declares newer signing schemes')
        if any(info.extract_version > 20 for info in archive.infolist()):
            raise ValueError('Unexpected ZIP extraction version')
    result = {
        'profile': 'sd8227-v1-test', 'baseSha256': BASE_SHA256,
        'apkSha256': digest(output), 'apkBytes': output.stat().st_size,
        'identicalPayloadEntries': len(after), 'identicalDexFiles': dex,
        'identicalNativeLibraries': sorted(native),
        'manifestAndResourcesByteIdentical': True,
        'newerApkSigningBlockAbsent': True, 'classicZipCrcVerified': True,
        'versionCode': 41, 'minSdk': 17, 'hardwareInstallation': 'pending',
    }
    output.with_suffix('.apk.sha256').write_text(
        result['apkSha256'] + '  ' + output.name + '\n', encoding='ascii')
    output.with_suffix('.audit.json').write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('command', choices=('repack', 'audit'))
    parser.add_argument('base', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    if args.base.resolve() == args.output.resolve():
        raise ValueError('Do not overwrite the baseline APK')
    globals()[args.command](args.base, args.output)
