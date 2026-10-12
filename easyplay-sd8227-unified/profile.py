"""Classic ZIP and V1 payload audit utilities for the unified SD8227 variant."""
from pathlib import Path
import hashlib,struct,zipfile
LIBS={'lib/armeabi-v7a/'+name for name in ('libdiplay_crypto.so','libdiplay_lwip.so','libdiplay_opus.so')}

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
