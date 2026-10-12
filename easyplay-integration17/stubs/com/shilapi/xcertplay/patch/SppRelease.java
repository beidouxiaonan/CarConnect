package com.shilapi.xcertplay.patch;
import java.io.IOException;
/** A disconnect request is not success: only a settled OEM status allows VF. */
public final class SppRelease {
    static final long WAIT=5000000000L, SETTLE=300000000L, NATIVE_WAIT=12000000000L, COOLDOWN=45000000000L;
    public interface Port { boolean connected() throws Exception; void disconnect() throws Exception; }
    public interface NativePort extends Port { void disconnectAll() throws Exception; }
    public interface Monitor { void check() throws Exception; void log(String value); }
    public interface Time { long now(); void pause() throws Exception; }
    public interface Control {
        boolean pending(); boolean nativeEnabled();
        void beforeRequest() throws Exception; void released() throws Exception;
    }
    public static final class BlockedException extends IOException {
        public BlockedException(String message){super(message);}
    }
    private boolean attempted; private long lastAttempt;
    private boolean idle(Port port,Monitor monitor,Time time)throws Exception {
        long started=time.now();
        while(time.now()-started<SETTLE){
            monitor.check();time.pause();monitor.check();
            if(port.connected())return false;
        }
        monitor.check();return true;
    }
    public synchronized boolean ensureFree(boolean enabled,boolean owned,Port port,Monitor monitor,Time time,Control control) throws Exception {
        monitor.check();
        if(owned)throw new IOException("CarConnect 自己的数据通道正在使用，跳过 SPP 清理");
        if(!port.connected()&&idle(port,monitor,time)){
            control.released();monitor.log("C1：原车 SPP 空闲稳定 300ms；正常连接，不发送断开请求");return false;
        }
        if(control.pending())throw new BlockedException("上次 SPP 恢复未确认；已阻止重复清理，未发送断开/VH/VF。请先恢复原车蓝牙或手动允许一次清理");
        if(!enabled)throw new BlockedException("原车已有 SPP 连接；清理开关未开启，暂停本轮自动重试，未发送 VF");
        long elapsed=time.now()-lastAttempt;
        if(attempted&&elapsed>=0&&elapsed<COOLDOWN) {
            monitor.log("C0：清理冷却尚余约 "+((COOLDOWN-elapsed+999999999L)/1000000000L)+" 秒；保留本次任务等待，不重复发送 VH/VF");
            boolean free=false;long freeAt=0;
            while(time.now()-lastAttempt>=0&&time.now()-lastAttempt<COOLDOWN) {
                monitor.check();long now=time.now();boolean connected=port.connected();monitor.check();
                if(connected)free=false;
                else if(!free){free=true;freeAt=now;}
                if(free&&now-freeAt>=SETTLE){control.released();monitor.log("C0：冷却期间通道已稳定释放，直接继续，不发送清理请求");return false;}
                time.pause();
            }
            monitor.check();
            monitor.log("C0：清理冷却已结束，重新检查通道；不把等待记作连接失败");
        }
        monitor.check();
        if(!port.connected()&&idle(port,monitor,time)){control.released();monitor.log("C1：原车 SPP 已稳定释放，不发送断开请求");return false;}
        monitor.check();
        if(control.pending())throw new BlockedException("已有未完成 SPP 清理，未发送新命令");
        control.beforeRequest();monitor.check();
        attempted=true;lastAttempt=time.now();
        monitor.log("C1：检测到原车 SPP 占用，按已开启的开关请求释放");
        port.disconnect();
        monitor.log("C2：原车 SppDisConnect 请求已返回，等待状态释放；不等于清理成功");
        long started=time.now(),overall=started,clearSince=0,wait=WAIT;boolean clearing=false,nativeSent=false;
        while(true) {
            monitor.check();
            long now=time.now();boolean connected=port.connected();
            monitor.check();
            if(connected)clearing=false;
            else if(!clearing){clearing=true;clearSince=now;}
            if(clearing&&now-clearSince>=SETTLE){
                control.released();monitor.check();
                monitor.log(nativeSent?"C5：底层清理后 SPP 未连接状态稳定 300ms，继续连接":"C3：原车 SPP 未连接状态稳定 300ms，清理完成");return true;
            }
            if(now-overall>=WAIT+NATIVE_WAIT+SETTLE)
                throw new BlockedException("SPP 清理总等待超时：暂停本轮自动重试，未发送 VF");
            if(now-started>=wait) {
                if(!nativeSent && port instanceof NativePort) {
                    monitor.check();
                    // Do not disrupt a late release that is already settling.
                    if(!port.connected()) {
                        if(!clearing){clearing=true;clearSince=time.now();}
                        started=time.now();wait=SETTLE+300000000L;continue;
                    }
                    monitor.check();
                    if(!control.nativeEnabled())throw new BlockedException("原接口未释放 SPP；底层恢复未开启，暂停本轮自动重试，未发送 VH/VF");
                    monitor.log("C4：原服务清理未释放，发送一次已核对的无索引 VH，释放原车模块管理的 SPP");
                    ((NativePort)port).disconnectAll();
                    nativeSent=true;clearing=false;started=time.now();wait=NATIVE_WAIT;
                    monitor.log("C4：底层 VH 已写入，等待释放回执；不等于清理成功");
                    continue;
                }
                throw new BlockedException(nativeSent?"SPP 底层清理超时：状态仍占用或被重新占用；暂停本轮自动重试，未发送 VF"
                    :"SPP 清理超时：原车仍占用或状态未稳定；暂停本轮自动重试，未发送 VF");
            }
            time.pause();
        }
    }
}
