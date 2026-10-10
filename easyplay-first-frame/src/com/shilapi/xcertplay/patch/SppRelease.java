package com.shilapi.xcertplay.patch;
import java.io.IOException;
/** A disconnect request is not success: only a settled OEM status allows VF. */
public final class SppRelease {
    static final long WAIT=5000000000L, SETTLE=300000000L, NATIVE_WAIT=12000000000L, COOLDOWN=45000000000L;
    public interface Port { boolean connected() throws Exception; void disconnect() throws Exception; }
    public interface NativePort extends Port { void disconnectAll() throws Exception; }
    public interface Monitor { void check() throws Exception; void log(String value); }
    public interface Time { long now(); void pause() throws Exception; }
    private boolean attempted; private long lastAttempt;
    public synchronized boolean ensureFree(boolean enabled,boolean owned,Port port,Monitor monitor,Time time) throws Exception {
        monitor.check();
        if(owned)throw new IOException("CarConnect 自己的数据通道正在使用，跳过 SPP 清理");
        if(!port.connected()){monitor.log("C1：未发现原车 SPP 占用，不发送断开请求");return false;}
        if(!enabled)throw new IOException("原车已有 SPP 连接；自动清理开关未开启，请先关闭其他投屏连接或开启清理");
        long elapsed=time.now()-lastAttempt;
        if(attempted&&elapsed>=0&&elapsed<COOLDOWN) {
            monitor.log("C0：清理冷却尚余约 "+((COOLDOWN-elapsed+999999999L)/1000000000L)+" 秒；保留本次任务等待，不重复发送 VH/VF");
            boolean free=false;long freeAt=0;
            while(time.now()-lastAttempt>=0&&time.now()-lastAttempt<COOLDOWN) {
                monitor.check();long now=time.now();boolean connected=port.connected();monitor.check();
                if(connected)free=false;
                else if(!free){free=true;freeAt=now;}
                if(free&&now-freeAt>=SETTLE){monitor.log("C0：冷却期间通道已稳定释放，直接继续，不发送清理请求");return false;}
                time.pause();
            }
            monitor.check();
            monitor.log("C0：清理冷却已结束，重新检查通道；不把等待记作连接失败");
        }
        monitor.check();
        if(!port.connected()){monitor.log("C1：原车 SPP 已自行释放，不发送断开请求");return false;}
        monitor.check();
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
                monitor.log(nativeSent?"C5：底层清理后 SPP 未连接状态稳定 300ms，继续连接":"C3：原车 SPP 未连接状态稳定 300ms，清理完成");return true;
            }
            if(now-overall>=WAIT+NATIVE_WAIT+SETTLE)
                throw new IOException("SPP 清理总等待超时：停止本次连接，未发送 VF");
            if(now-started>=wait) {
                if(!nativeSent && port instanceof NativePort) {
                    monitor.check();
                    // Do not disrupt a late release that is already settling.
                    if(!port.connected()) {
                        if(!clearing){clearing=true;clearSince=time.now();}
                        started=time.now();wait=SETTLE+300000000L;continue;
                    }
                    monitor.check();
                    monitor.log("C4：原服务清理未释放，发送一次已核对的无索引 VH，释放原车模块管理的 SPP");
                    ((NativePort)port).disconnectAll();
                    nativeSent=true;clearing=false;started=time.now();wait=NATIVE_WAIT;
                    monitor.log("C4：底层 VH 已写入，等待释放回执；不等于清理成功");
                    continue;
                }
                throw new IOException(nativeSent?"SPP 底层清理超时：状态仍占用或被重新占用；停止本次连接，未发送 VF"
                    :"SPP 清理超时：原车仍占用或状态未稳定；停止本次连接，未发送 VF");
            }
            time.pause();
        }
    }
}
