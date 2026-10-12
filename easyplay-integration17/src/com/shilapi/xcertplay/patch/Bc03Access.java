package com.shilapi.xcertplay.patch;

import android.content.Context;
import android.os.IBinder;
import android.os.Parcel;
import java.io.IOException;

/** Typed query access plus one explicitly enabled and schema-checked SPP disconnect. */
final class Bc03Access implements java.io.Closeable {
    static final String DESCRIPTOR = "com.bt.BTFeature";
    final VendorBluetoothStatus.Binding binding;
    final IBinder binder;
    final Bc03Schema schema;
    Bc03Access(Context context) throws Exception {
        OemTrace.install(context);OemTrace.event("BINDER bind BC03 begin");
        String version=context.getPackageManager().getPackageInfo("com.bt.bc03",0).versionName;
        Bc03Profiles.forVersion(version);
        schema=Bc03Schema.read(context.getPackageManager().getApplicationInfo("com.bt.bc03",0).sourceDir);
        binding=new VendorBluetoothStatus.Binding(context,"com.bt.bc03","com.bt.bc03.BTService");
        try {
            binder=binding.get();
            if(!DESCRIPTOR.equals(binder.getInterfaceDescriptor()))throw new IOException("BC03 Binder 不匹配");
            if(!schema.peerLayoutVerified)throw new IOException("BC03 手机信息格式未核对");
            schema.code("getLocalDeviceAddress");schema.code("isSppConnect");
            OemTrace.event("BINDER descriptor and peer schema verified; version="+version);
        } catch(Exception error) { OemTrace.error("BINDER bind/schema",error);binding.close(); throw error; }
    }
    boolean bool(String method) throws Exception {
        long start=System.nanoTime();
        try {
            boolean value=VendorBluetoothStatus.bool(binder,schema.code(method));
            long ms=(System.nanoTime()-start)/1000000L;
            if("isSppConnect".equals(method)){SppStateTrace.cache(value);OemTrace.state(value,ms);}
            else if(ms>=250)OemTrace.event("BINDER "+method+" slow elapsedMs="+ms);
            return value;
        }catch(Exception error){OemTrace.error("BINDER "+method,error);throw error;}
    }
    String string(String method) throws Exception {
        Parcel p=VendorBluetoothStatus.call(binder,DESCRIPTOR,schema.code(method),null,null);
        try {return p.readString();}finally{p.recycle();}
    }
    void disconnectSpp() throws Exception {
        // No arguments, void result. Do not substitute a module power/reset command.
        OemTrace.event("BINDER SppDisConnect request (return is not release confirmation)");
        Parcel reply=VendorBluetoothStatus.call(binder,DESCRIPTOR,schema.code("SppDisConnect"),null,null);
        reply.recycle();
        OemTrace.event("BINDER SppDisConnect returned");
    }
    Snapshot snapshot() throws Exception {
        if(!bool("isBlueToothPowerOn"))throw new IOException("原车蓝牙模块尚未就绪");
        if(!bool("isConnectHFP"))throw new IOException("请先在原车蓝牙页面连接 iPhone 的通话蓝牙");
        Parcel p=VendorBluetoothStatus.call(binder,DESCRIPTOR,schema.code("getConnectDevice"),null,null);
        String phone,name;
        try {
            if(p.readInt()==0)throw new IOException("原车未上报已连接手机");
            phone=OemProtocol.normalizeMac(p.readString());name=p.readString();
        } finally {p.recycle();}
        String local=OemProtocol.normalizeMac(string("getLocalDeviceAddress"));
        if(phone==null||local==null)throw new IOException("原车未上报有效手机/车机 MAC；不能使用 t3 地址代替");
        return new Snapshot(phone,local,name);
    }
    public void close(){binding.close();}
    static final class Snapshot {
        final String phone,local,name;
        Snapshot(String phone,String local,String name){this.phone=phone;this.local=local;this.name=name;}
    }
}
