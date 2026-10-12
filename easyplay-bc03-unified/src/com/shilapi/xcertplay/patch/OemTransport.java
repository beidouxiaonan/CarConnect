package com.shilapi.xcertplay.patch;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.shilapi.xcertplay.orchestration.CarPlayController;
import com.shilapi.xcertplay.transport.BluetoothRfcommDuplexStream;
import java.io.*;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Opt-in bridge to an existing OEM daemon, with no root or system modifications. */
public final class OemTransport {
    private static final ThreadLocal<Run> RUN=new ThreadLocal<Run>();
    private static final AtomicBoolean opening=new AtomicBoolean();
    private static final ArrayDeque<String> notes=new ArrayDeque<String>();
    private static volatile Owner current;
    private static final OemSppGate.RetryWindow retry=new OemSppGate.RetryWindow();
    private static final AtomicBoolean startupChecked=new AtomicBoolean();
    private static final SppRelease release=new SppRelease();
    private static final SppRelease.Time clock=new SppRelease.Time(){
        public long now(){return System.nanoTime();}
        public void pause()throws Exception {Thread.sleep(150);}
    };
    private static Bc03Profiles.Profile verifyProfile(Context context)throws Exception {
        String version=context.getPackageManager().getPackageInfo("com.bt.bc03",0).versionName;
        Bc03Profiles.forVersion(version);
        String apk=digest(context.getPackageManager().getApplicationInfo("com.bt.bc03",0).sourceDir,"BC03 APK");
        String daemon=digest("/system/bin/gocsdk","gocsdk");
        return Bc03Profiles.verified(version,apk,daemon);
    }
    private static boolean cleanupEnabled(Context context){
        return EnhancementPrefs.getOemBluetooth(context)&&SppCleanupPrefs.enabled(context);
    }
    public static void rearmCleanup(Context context)throws IOException {
        // Share the opening CAS with open(): an explicit reset cannot race an in-flight dispatch.
        if(!opening.compareAndSet(false,true))throw new IOException("连接任务仍在运行；请先关闭自动连接并等待任务退出");
        try{
            if(current!=null&&!current.closed.get())throw new IOException("CarConnect 通道正在使用，不允许清理");
            if(!SppRecoveryPrefs.released(context))throw new IOException("无法保存清理状态，未解除暂停");
            note("P2：用户允许再次清理一次；不自动发命令、不启动新会话");
        }finally{opening.set(false);}
    }
    /** Called by the existing Activity setup. Runs outside the UI thread once per process. */
    public static void startup(final Context context){
        SppStateTrace.install(context);
        if(!EnhancementPrefs.getOemBluetooth(context)||!startupChecked.compareAndSet(false,true))return;
        final Context app=context.getApplicationContext()==null?context:context.getApplicationContext();
        Thread worker=new Thread(new Runnable(){public void run(){
            Bc03Access bc=null;
            try {
                if(current!=null&&!current.closed.get()){note("启动 SPP 检查跳过：CarConnect 自己的通道正在使用");return;}
                verifyProfile(app);
                bc=new Bc03Access(app);
                if(!bc.bool("isBlueToothPowerOn")){note("启动 SPP 检查：原车蓝牙未就绪，连接前再检查");return;}
                note(bc.bool("isSppConnect")?"启动检查：原车 SPP 当前占用；清理延后到连接请求，启动不发送断开命令"
                    :"启动检查：原车 SPP 当前空闲；启动不发送断开命令");
            }catch(Exception error){note("启动 SPP 状态检查未完成："+error.getMessage());}
            finally{if(bc!=null)bc.close();}
        }},"carconnect-spp-startup-check");worker.setDaemon(true);worker.start();
    }
    private static SppRelease.Port port(final Bc03Access bc,final Run r,final Bc03Access.Snapshot expected){
        return new SppRelease.NativePort(){
            public boolean connected()throws Exception{boolean value=bc.bool("isSppConnect");SppStateTrace.cache(value);return value;}
            public void disconnect()throws Exception {
                r.check();
                if(!cleanupEnabled(r.context)||current!=null&&!current.closed.get())
                    throw new IOException("原车 SPP 清理已取消或本应用通道正在使用");
                Bc03Access.Snapshot now=bc.snapshot();
                if(!expected.phone.equals(now.phone)||!expected.local.equals(now.local)||!RememberedPhone.matches(r.context,now.phone))
                    throw new IOException("等待清理期间手机或模块地址已变化，未发送断开");
                r.check();
                if(!cleanupEnabled(r.context))throw new IOException("原车 SPP 清理开关已关闭，未发送断开");
                SppStateTrace.request();bc.disconnectSpp();
            }
            public void disconnectAll()throws Exception {
                r.check();
                if(!SppRecoveryPrefs.nativeEnabled(r.context))throw new SppRelease.BlockedException("底层恢复选项已关闭，未发送 VH");
                if(!cleanupEnabled(r.context)||current!=null&&!current.closed.get())
                    throw new IOException("SPP 底层清理已取消或本应用通道正在使用");
                verifyProfile(r.context);
                Bc03Access.Snapshot now=bc.snapshot();
                if(!expected.phone.equals(now.phone)||!expected.local.equals(now.local)||!RememberedPhone.matches(r.context,now.phone))
                    throw new IOException("底层清理前手机或模块地址已变化，未发送 VH");
                r.check();
                if(!cleanupEnabled(r.context)||current!=null&&!current.closed.get())throw new IOException("底层清理已取消，未发送 VH");
                if(!SppRecoveryPrefs.nativeEnabled(r.context))throw new SppRelease.BlockedException("底层恢复选项已关闭，未发送 VH");
                writeCommand(SppNativeCommand.releaseAll());
            }
        };
    }
    private static final class Run {
        final Context context; final CarPlayController controller; final int generation; final boolean enabled;
        Bc03Access.Snapshot target; boolean cleanupRequested;
        Run(Context c,CarPlayController o,int g){context=c;controller=o;generation=g;enabled=EnhancementPrefs.getOemBluetooth(c);}
        void check() throws IOException {
            if(Thread.currentThread().isInterrupted()||CarPlayController.access$isStaleWirelessRun(controller,generation))
                throw new IOException("原车蓝牙连接已取消");
        }
        void log(String value){note(value);CarPlayController.access$debugLog(controller,"OEM Bluetooth: "+value);}
    }
    public static void begin(Context context,CarPlayController owner,int generation){OemSppPause.forget(owner);RUN.set(new Run(context,owner,generation));}
    public static void end(){RUN.remove();}
    public static boolean enabled(){Run r=RUN.get();return r!=null&&r.enabled;}
    public static boolean adapterEnabled(BluetoothAdapter adapter){return enabled()||adapter.isEnabled();}
    private static Run run() throws IOException {
        Run r=RUN.get();if(r==null||!r.enabled)throw new IOException("原车蓝牙测试未启用");r.check();return r;
    }
    public static BluetoothDevice select(BluetoothAdapter adapter) throws Exception {
        Run r=run();
        Bc03Access bc=new Bc03Access(r.context);
        try {r.target=bc.snapshot();}finally{bc.close();}
        if(!RememberedPhone.matches(r.context,r.target.phone))
            throw new IOException("原车当前手机与已记住的 iPhone 不同，请连接原手机或明确更换");
        r.check();r.log("已选择 BC03 已连接手机，使用原车模块 MAC");
        // This is an address wrapper only. Never open a standard-stack RFCOMM socket for it.
        return adapter.getRemoteDevice(r.target.phone);
    }
    public static String localMac() throws IOException {
        Run r=run();if(r.target==null)throw new IOException("尚未选择原车手机");return r.target.local;
    }
    public static BluetoothRfcommDuplexStream open(String phone) throws Exception {
        final Run r=run();
        if(!opening.compareAndSet(false,true))throw new IOException("原车蓝牙连接仍在处理中");
        Bc03Access bc=null;Owner owner=null;boolean transferred=false,commandSent=false;
        try {
            if(current!=null&&!current.closed.get())throw new IOException("CarConnect 已持有原车数据通道，请先断开");
            Bc03Profiles.Profile profile=verifyProfile(r.context);
            r.check();r.log("BC03 与 gocsdk 指纹核对通过；统一适配配置："+profile.version);
            bc=new Bc03Access(r.context);
            Bc03Access.Snapshot target=bc.snapshot();
            if(!RememberedPhone.matches(r.context,target.phone))
                throw new IOException("手机选择已变化，停止本次原车连接");
            if(r.target==null||!target.phone.equals(OemProtocol.normalizeMac(phone))||!target.local.equals(r.target.local))
                throw new IOException("原车连接手机或模块地址已变化，请重新连接");
            final boolean clean=cleanupEnabled(r.context);
            OemSppGate.prepare(release,clean,false,port(bc,r,target),new SppRelease.Monitor(){
                public void check()throws Exception {
                    r.check();
                    if(!RememberedPhone.matches(r.context,target.phone))
                        throw new IOException("等待 SPP 清理期间已保存手机改变，停止旧任务");
                    if(!EnhancementPrefs.getOemBluetooth(r.context)||(clean&&!cleanupEnabled(r.context)))
                        throw new IOException("原车 SPP 清理已取消或开关已关闭");
                }
                public void log(String value){r.log(value);}
            },clock,retry,new SppRelease.Control(){
                public boolean pending(){return SppRecoveryPrefs.pending(r.context);}
                public boolean nativeEnabled(){return SppRecoveryPrefs.nativeEnabled(r.context);}
                public void beforeRequest()throws Exception {
                    r.check();
                    if(!SppRecoveryPrefs.begin(r.context))throw new SppRelease.BlockedException("未完成标记保存失败，未发送清理请求");
                    r.cleanupRequested=true;
                }
                public void released()throws Exception {
                    r.check();
                    if(SppRecoveryPrefs.pending(r.context)&&!SppRecoveryPrefs.released(r.context))
                        throw new SppRelease.BlockedException("释放状态保存失败，停止接入，未发送 VF");
                }
            });
            // Revalidate the phone and local module after any destructive SPP operation.
            Bc03Access.Snapshot after=bc.snapshot();
            if(!target.phone.equals(after.phone)||!target.local.equals(after.local)||!RememberedPhone.matches(r.context,after.phone))
                throw new IOException("SPP 清理期间原车手机或地址已变化，停止本次连接");
            if(bc.bool("isSppConnect"))throw new SppRelease.BlockedException("SPP 在清理后再次被占用；暂停本轮重试，未发送 VF");
            File serial=new File("/dev/goc_serial");String real=serial.getCanonicalPath();
            if(!real.matches("/dev/pts/[0-9]+"))throw new IOException("goc_serial 未指向已核对的原车虚拟串口："+real);
            long remaining=retry.remainingMillis(System.nanoTime());
            if(remaining>0)throw new IOException("原车 VF 请求冷却中，约 "+((remaining+999)/1000)+" 秒后再试；未发送新命令");
            // Connect before sending any command, without a MAC greeting: no Bluetooth connection yet.
            final LocalSocket socket=new LocalSocket();owner=new Owner(socket);current=owner;
            owner.phase("S1 连接本地 socket",5);
            final Owner watched=owner;
            Thread watchdog=new Thread(new Runnable(){public void run(){
                while(!watched.established&&!watched.closed.get()) {
                    try {r.check();String expired=watched.deadline.expired(System.nanoTime());if(expired!=null)throw new IOException(expired);Thread.sleep(100);}
                    catch(Exception error){watched.abort(error.getMessage());return;}
                }
            }},"carconnect-oem-connect-watchdog");watchdog.setDaemon(true);watchdog.start();
            try {socket.connect(new LocalSocketAddress("/dev/socket/goc_spp",LocalSocketAddress.Namespace.FILESYSTEM));}
            catch(IOException error){throw new IOException("goc_spp 不可访问（端口未开放或系统拒绝）："+error.getMessage(),error);}
            r.check();r.log("S1：goc_spp 端口可访问，尚未发送手机地址；此时没有验证 SPP / iAP2");
            boolean probeWithoutStatus=profile.probeWithoutStatus;
            owner.phase("S2 写入 VF",5);
            // MODE_WRITE_ONLY does not create or truncate. Never consume BC03's UART responses.
            try {
                owner.attemptToken=retry.sent(System.nanoTime());
                writeCommand(OemProtocol.connectCommand(target.phone));commandSent=true;
            } catch(IOException error){throw new IOException("goc_serial 写入失败（普通 APK 权限受限）："+error.getMessage(),error);}
            r.log("S2：已写入 VF + iAP2 UUID（51 字节）；写入成功不代表原车接受请求");
            owner.phase("S3 等待 SPP 状态",18);
            long requested=System.nanoTime();
            boolean spp=false;
            while(true) {
                r.check();owner.check();
                spp=bc.bool("isSppConnect");
                if(OemSppGate.mayAttach(spp,System.nanoTime()-requested,probeWithoutStatus))break;
                if(System.nanoTime()-requested>=OemSppGate.STATUS_WAIT_NANOS)
                    throw new IOException("VF 请求后未收到 SPP 连接状态；尚未接入 iAP2");
                Thread.sleep(150);
            }
            r.check();owner.check();
            owner.phase("S3 复核已连接手机",5);
            if(!target.phone.equals(bc.snapshot().phone))throw new IOException("等待期间原车手机已变化，停止接入");
            owner.probing=!spp;
            if(!spp)r.log("S3：BC03 1.7.2 未上报 SPP；开始一次限时数据验证，不判定为已连接");
            else r.log("S3：BC03 已上报 SPP，接入数据通道");
            owner.phase("S4 登记手机地址",5);
            OutputStream direct=new OemSocketOutput(socket.getOutputStream(),owner);
            direct.write(OemProtocol.socketHello(target.phone));
            direct.flush();
            r.log("S4：已登记 goc_spp 手机地址（12 字节）；下面的收发计数仅为协议数据");
            owner.phase("S5 交接 iAP2 字节流",5);
            owner.check();r.check();
            if(owner.probing)startProbeDeadline(owner);
            InputStream input=new CountedInput(socket.getInputStream(),owner);
            OutputStream output=new CountedOutput(direct,owner);
            BluetoothRfcommDuplexStream stream=new BluetoothRfcommDuplexStream(input,output,owner);
            owner.established=true;transferred=true;
            r.log("S5：字节流交给现有 iAP2 握手；握手通过前不代表 CarPlay 已连接");
            return stream;
        } catch(Exception error) {
            if(owner!=null&&owner.closed.get()&&owner.failure!=null)error=new IOException(owner.failure,error);
            if(!Thread.currentThread().isInterrupted()&&!CarPlayController.access$isStaleWirelessRun(r.controller,r.generation)
                &&(error instanceof SppRelease.BlockedException||(r.cleanupRequested&&SppRecoveryPrefs.pending(r.context))))
                OemSppPause.mark(r.controller,error.getMessage());
            r.log("失败："+error.getClass().getSimpleName()+" "+error.getMessage());
            r.log(SppStateTrace.summary());
            if(commandSent&&!transferred)r.log("请求可能仍在原车处理；等待 VF 冷却结束后再试。清理未确认则暂停本轮重试。");
            throw error;
        } finally {
            if(bc!=null)bc.close();
            if(owner!=null&&!transferred)owner.close();
            opening.set(false);
        }
    }
    private static void startProbeDeadline(final Owner owner) {
        final long started=System.nanoTime();
        Thread guard=new Thread(new Runnable(){public void run(){
            while(!owner.closed.get()&&owner.rx.get()==0) {
                if(OemSppGate.probeExpired(owner.probing,owner.rx.get(),System.nanoTime()-started)) {
                    note("S6：SPP 状态缺失且 8 秒内没有收到数据；关闭本应用客户端，等待冷却后重试");
                    owner.abort("原车数据验证超时（SPP 未上报，8 秒无接收数据）；iAP2 未连接");return;
                }
                try{Thread.sleep(100);}catch(InterruptedException cancelled){Thread.currentThread().interrupt();return;}
            }
        }},"carconnect-oem-data-probe");guard.setDaemon(true);guard.start();
    }
    private static void writeCommand(byte[] bytes)throws IOException {
        File serial=new File("/dev/goc_serial");String real=serial.getCanonicalPath();
        if(!real.matches("/dev/pts/[0-9]+"))throw new IOException("goc_serial 未指向已核对的原车虚拟串口");
        ParcelFileDescriptor fd=ParcelFileDescriptor.open(serial,ParcelFileDescriptor.MODE_WRITE_ONLY);
        OutputStream out=new ParcelFileDescriptor.AutoCloseOutputStream(fd);
        try {out.write(bytes);out.flush();}finally{out.close();}
    }
    private static String digest(String path,String label) throws Exception {
        MessageDigest hash=MessageDigest.getInstance("SHA-256");InputStream in=new FileInputStream(path);
        try {byte[] buf=new byte[8192];int n;long size=0;while((n=in.read(buf))!=-1){size+=n;if(size>16*1024*1024)throw new IOException(label+" 大小未核对");hash.update(buf,0,n);}}
        finally {in.close();}
        StringBuilder actual=new StringBuilder();for(byte b:hash.digest())actual.append(String.format(java.util.Locale.US,"%02x",b&255));
        return actual.toString();
    }
    private static synchronized void note(String value){if(notes.size()==24)notes.removeFirst();notes.addLast(value);Log.i("CarConnectOEM",value);}
    public static synchronized String summary(){
        StringBuilder s=new StringBuilder("CarConnect 0.1.11 BC03 统一适配测试：已核对 1.7.2 / 1.7.9 / 1.3.6；按版本及文件指纹选择连接流程。1.7.2 保留限时数据验证；底层 VH 默认关闭。需先在原车蓝牙连接已配对 iPhone。\n");
        Owner o=current;if(o!=null)s.append(o.closed.get()?"通道已关闭":"通道已打开").append("，收到 ").append(o.rx.get()).append(" 字节，发送 ").append(o.tx.get()).append(" 字节。\n");
        for(String n:notes)s.append(n).append('\n');s.append(SppStateTrace.summary()).append('\n');return s.toString();
    }
    private static final class Owner implements OemSocketOutput.Guard {
        final LocalSocket socket;final AtomicBoolean closed=new AtomicBoolean();
        final AtomicLong rx=new AtomicLong(),tx=new AtomicLong();
        final OemConnectDeadline deadline=new OemConnectDeadline(System.nanoTime());
        volatile boolean established,probing;volatile String failure;
        long attemptToken;
        Owner(LocalSocket s){socket=s;}
        public void check()throws IOException{if(closed.get())throw new IOException(failure==null?"原车数据通道已关闭":failure);}
        void phase(String stage,long seconds)throws IOException {check();deadline.enter(stage,System.nanoTime(),seconds*1000000000L);note("T："+stage);check();}
        synchronized void abort(String reason){if(failure==null)failure=reason;note("通道关闭原因："+failure);try{close();}catch(IOException ignored){}}
        public void close()throws IOException{
            if(!closed.compareAndSet(false,true))return;
            try{socket.close();}finally{
                if(rx.get()>0)retry.received(attemptToken);
                note("关闭本应用 goc_spp 客户端；收到 "+rx.get()+" / 发送 "+tx.get()+" 字节（不含 VF / 地址登记）");
            }
        }
    }
    private static final class CountedInput extends FilterInputStream {
        final Owner owner;CountedInput(InputStream in,Owner o){super(in);owner=o;}
        void received(int n){if(n>0&&owner.rx.getAndAdd(n)==0)note("S6：收到首批协议数据；仍由原有 iAP2 握手校验内容");}
        public int read(byte[] b,int off,int len)throws IOException{
            try{int n=in.read(b,off,len);received(n);return n;}catch(IOException error){owner.check();throw error;}
        }
        public int read()throws IOException{
            try{int n=in.read();if(n>=0)received(1);return n;}catch(IOException error){owner.check();throw error;}
        }
    }
    private static final class CountedOutput extends FilterOutputStream {
        final Owner owner;CountedOutput(OutputStream out,Owner o){super(out);owner=o;}
        public void write(byte[] b,int off,int len)throws IOException{owner.check();out.write(b,off,len);owner.tx.addAndGet(len);}
        public void write(int b)throws IOException{owner.check();out.write(b);owner.tx.incrementAndGet();}
    }
}
