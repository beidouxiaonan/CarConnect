package com.shilapi.xcertplay.patch;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Reads installed AIDL metadata without executing or loading OEM classes. */
public final class Bc03Schema {
    private static final String API="Lcom/bt/BTFeature;", STUB="Lcom/bt/BTFeature$Stub;";
    private static final String DEVICE="Lcom/bt/BTDevice;";
    private static final String[] GETTERS={"isBlueToothPowerOn","isConnectDevice","isConnectHFP","isConnectA2DP","getLocalDeviceName"};
    private final Map<String,Integer> codes;
    public final boolean peerLayoutVerified;
    private Bc03Schema(Map<String,Integer> codes,boolean peer) { this.codes=codes; peerLayoutVerified=peer; }
    public int code(String name) throws IOException {
        Integer value=codes.get(name); if(value==null)throw new IOException("未核对接口 "+name); return value;
    }
    public static Bc03Schema read(String apk) throws IOException {
        Map<String,String> signatures=new HashMap<String,String>();
        Map<String,Object> constants=new HashMap<String,Object>();
        boolean peer=false;
        ZipFile zip=new ZipFile(apk);
        try {
            Enumeration<? extends ZipEntry> entries=zip.entries();
            int total=0;
            while(entries.hasMoreElements()) {
                ZipEntry entry=entries.nextElement();
                if(!entry.getName().matches("classes([2-9]|[1-9][0-9]+)?\\.dex"))continue;
                InputStream in=zip.getInputStream(entry);
                byte[] bytes;
                try {
                    ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] block=new byte[8192]; int n;
                    while((n=in.read(block))!=-1) {
                        total+=n; if(total>64*1024*1024)throw new IOException("OEM DEX 超出读取上限"); out.write(block,0,n);
                    }
                    bytes=out.toByteArray();
                } finally { in.close(); }
                Dex dex=new Dex(bytes); dex.inspect(signatures,constants); peer|=dex.peer;
            }
        } finally { zip.close(); }
        return validate(signatures,constants,peer);
    }
    static Bc03Schema validate(Map<String,String> signatures,Map<String,Object> constants,boolean peer) throws IOException {
        if(!"com.bt.BTFeature".equals(constants.get("DESCRIPTOR")))throw new IOException("未核对 BC03 Binder 描述符");
        Map<String,Integer> codes=new HashMap<String,Integer>(); Set<Integer> used=new HashSet<Integer>();
        for(String name:GETTERS) {
            String result=name.equals("getLocalDeviceName")?"()Ljava/lang/String;":"()Z";
            add(codes,used,signatures,constants,name,result);
        }
        if(peer) add(codes,used,signatures,constants,"getConnectDevice","()"+DEVICE);
        for(String name:new String[]{"getLocalDeviceAddress","isSppConnect"}) {
            if(signatures.containsKey(name)) add(codes,used,signatures,constants,name,
                name.equals("getLocalDeviceAddress")?"()Ljava/lang/String;":"()Z");
        }
        if(signatures.containsKey("SppDisConnect")) add(codes,used,signatures,constants,"SppDisConnect","()V");
        return new Bc03Schema(codes,peer);
    }
    private static void add(Map<String,Integer> codes,Set<Integer> used,Map<String,String> signatures,
                            Map<String,Object> constants,String name,String signature) throws IOException {
        Object value=constants.get("TRANSACTION_"+name);
        if(!signature.equals(signatures.get(name))||!(value instanceof Integer))throw new IOException("接口签名/编号不匹配："+name);
        int code=(Integer)value;
        if(code<1||code>0xffffff||!used.add(code))throw new IOException("接口编号无效或重复："+name);
        codes.put(name,code);
    }
    private static final class Dex {
        final byte[] b; final String[] strings,types; final int fields,methods,protos,defs,count;
        boolean peer;
        Dex(byte[] bytes) throws IOException {
            b=bytes; check(0,112);
            if(b[0]!='d'||b[1]!='e'||b[2]!='x'||b[3]!=10||b[7]!=0||u32(40)!=0x12345678)
                throw new IOException("无法读取 OEM DEX 格式");
            if(u32(32)!=b.length)throw new IOException("OEM DEX 长度不符");
            table(u32(60),u32(56),4); strings=new String[u32(56)];
            for(int i=0;i<strings.length;i++) {
                Cursor c=new Cursor(u32(u32(60)+i*4)); c.uleb(); int start=c.p;
                while(c.byteValue()!=0) {} strings[i]=new String(b,start,c.p-start-1,"UTF-8");
            }
            table(u32(68),u32(64),4); types=new String[u32(64)];
            for(int i=0;i<types.length;i++)types[i]=string(u32(u32(68)+4*i));
            protos=u32(76); table(protos,u32(72),12);
            fields=u32(84); table(fields,u32(80),8); methods=u32(92); table(methods,u32(88),8);
            defs=u32(100); count=u32(96); table(defs,count,32);
        }
        void inspect(Map<String,String> signatures,Map<String,Object> constants) throws IOException {
            for(int i=0;i<count;i++) {
                int off=defs+i*32; String name=type(u32(off));
                if(!API.equals(name)&&!STUB.equals(name)&&!DEVICE.equals(name))continue;
                int data=u32(off+24); if(data==0)continue;
                Cursor c=new Cursor(data); int statics=c.uleb(),instances=c.uleb(),direct=c.uleb(),virtual=c.uleb();
                Cursor values=u32(off+28)==0?null:new Cursor(u32(off+28));
                int valuesCount=values==null?0:values.uleb(),field=0;
                for(int f=0;f<statics;f++) {
                    field+=c.uleb(); c.uleb(); Object value=f<valuesCount?values.encoded():Integer.valueOf(0);
                    checkField(field);
                    if(STUB.equals(name)) constants.put(fieldName(field),value);
                }
                for(int f=0;f<instances;f++){ c.uleb(); c.uleb(); }
                for(int group=0;group<2;group++) {
                    int method=0;
                    for(int m=0;m<(group==0?direct:virtual);m++) {
                        method+=c.uleb(); int access=c.uleb(),code=c.uleb(); checkMethod(method);
                        if(API.equals(name)&&(access&8)==0)signatures.put(methodName(method),signature(method));
                        if(DEVICE.equals(name)&&"<init>".equals(methodName(method))&&"(Landroid/os/Parcel;)V".equals(signature(method)))
                            peer=verifyPeer(code);
                    }
                }
            }
        }
        boolean verifyPeer(int code) throws IOException {
            if(code==0)return false;
            check(code,16); int registers=u16(code),ins=u16(code+2),units=u32(code+12),start=code+16;
            table(start,units,2); if(ins!=2||units<15)return false;
            // Require Object.<init>, then exactly readString -> result -> mAddress, mName.
            int unit=0;
            if((u16(start)&255)!=0x70)return false;
            int constructor=u16(start+2); checkMethod(constructor);
            if(!"Ljava/lang/Object;".equals(methodOwner(constructor))||!"<init>".equals(methodName(constructor))
                ||!"()V".equals(signature(constructor)))return false;
            unit+=3;
            for(String field:new String[]{"mAddress","mName"}) {
                int at=start+unit*2,invoke=u16(at),method=u16(at+2),args=u16(at+4);
                checkMethod(method);
                if(invoke!=0x106e||args!=registers-1||!"Landroid/os/Parcel;".equals(methodOwner(method))
                    ||!"readString".equals(methodName(method))||!"()Ljava/lang/String;".equals(signature(method)))return false;
                int result=u16(at+6),put=u16(at+8),f=u16(at+10); checkField(f);
                if((result&255)!=0x0c||(put&255)!=0x5b||((put>>8)&15)!=(result>>8)
                    ||(put>>12)!=registers-2||!field.equals(fieldName(f))
                    ||!DEVICE.equals(type(u16(fields+f*8)))||!"Ljava/lang/String;".equals(type(u16(fields+f*8+2))))return false;
                unit+=6;
            }
            return true;
        }
        String signature(int method) throws IOException {
            int p=protos+u16(methods+method*8+2)*12; check(p,12);
            StringBuilder s=new StringBuilder("("); int parameters=u32(p+8);
            if(parameters!=0) { int n=u32(parameters); table(parameters+4,n,2); for(int i=0;i<n;i++)s.append(type(u16(parameters+4+2*i))); }
            return s.append(')').append(type(u32(p+4))).toString();
        }
        String methodOwner(int i)throws IOException { return type(u16(methods+i*8)); }
        String methodName(int i)throws IOException { return string(u32(methods+i*8+4)); }
        String fieldName(int i)throws IOException { return string(u32(fields+i*8+4)); }
        void checkMethod(int i)throws IOException { if(i<0||i>=u32(88))throw new IOException("DEX method 越界"); }
        void checkField(int i)throws IOException { if(i<0||i>=u32(80))throw new IOException("DEX field 越界"); }
        String type(int i)throws IOException { if(i<0||i>=types.length)throw new IOException("DEX type 越界"); return types[i]; }
        String string(int i)throws IOException { if(i<0||i>=strings.length)throw new IOException("DEX string 越界"); return strings[i]; }
        int u16(int at)throws IOException { check(at,2); return(b[at]&255)|((b[at+1]&255)<<8); }
        int u32(int at)throws IOException { check(at,4); int v=u16(at)|(u16(at+2)<<16); if(v<0)throw new IOException("DEX 数值越界"); return v; }
        void check(int at,int length)throws IOException { if(at<0||length<0||at>b.length-length)throw new IOException("DEX 数据截断"); }
        void table(int at,int n,int size)throws IOException { if(n<0||n>b.length/size)throw new IOException("DEX 表越界"); check(at,n*size); }
        final class Cursor {
            int p; Cursor(int p){this.p=p;}
            int byteValue()throws IOException {check(p,1);return b[p++]&255;}
            int uleb()throws IOException {
                int value=0; for(int i=0;i<5;i++){int v=byteValue(); if(i==4&&(v&0xf8)!=0)throw new IOException("DEX ULEB 越界"); value|=(v&127)<<(i*7);if(v<128)return value;}
                throw new IOException("DEX ULEB 无效");
            }
            Object encoded()throws IOException {
                int tag=byteValue(),type=tag&31,n=(tag>>5)+1;
                if(type==0x1e)return null;
                if(type==0x1f)return (tag>>5)!=0;
                if(type==0x1c||type==0x1d||n>8)throw new IOException("DEX 常量格式不支持");
                long value=0;for(int i=0;i<n;i++)value|=(long)byteValue()<<(8*i);
                if(type==0x17)return string((int)value);
                if(type==4){if(n>4)throw new IOException("DEX int 无效");return Integer.valueOf((int)((value<<(64-n*8))>>(64-n*8)));}
                return Long.valueOf(value);
            }
        }
    }
}
