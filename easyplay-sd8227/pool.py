"""Binary Android string-pool helpers (no payload assets)."""
import struct

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

