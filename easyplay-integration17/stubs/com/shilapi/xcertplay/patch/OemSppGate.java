package com.shilapi.xcertplay.patch;

import java.io.IOException;

/** A missing OEM flag permits a bounded data test, never a successful iAP2 result. */
public final class OemSppGate {
    static final long STATUS_WAIT_NANOS=15000000000L;
    static final long PROBE_WAIT_NANOS=8000000000L;
    private static final long RETRY_WAIT_NANOS=45000000000L;
    private OemSppGate() {}
    static boolean mayAttach(boolean spp,long elapsed,boolean verified172) {
        return spp || (verified172 && elapsed>=STATUS_WAIT_NANOS);
    }
    static boolean probeExpired(boolean probing,long received,long elapsed) {
        return probing && received==0 && elapsed>=PROBE_WAIT_NANOS;
    }
    /** Preserve the 1.7.2 VF window before any cleanup request, including a manual rearm. */
    static boolean prepare(SppRelease release,boolean enabled,boolean owned,SppRelease.Port port,
            final SppRelease.Monitor monitor,final SppRelease.Time time,final RetryWindow retry,
            SppRelease.Control control)throws Exception {
        return release.ensureFree(enabled,owned,port,new SppRelease.Monitor(){
            public void check()throws Exception {
                monitor.check();
                long remaining=retry.remainingMillis(time.now());
                if(remaining>0)throw new IOException("原车 VF 请求冷却中，约 "+((remaining+999)/1000)+" 秒后再试；暂不清理 SPP，手机记录已保留");
            }
            public void log(String value){monitor.log(value);}
        },time,control);
    }
    static final class RetryWindow {
        private long lastSent;
        private long token;
        private boolean pending;
        synchronized long sent(long now){lastSent=now;pending=true;return ++token;}
        synchronized long remainingMillis(long now){
            if(!pending)return 0;
            long elapsed=now-lastSent;
            if(elapsed>=RETRY_WAIT_NANOS){pending=false;return 0;}
            return (RETRY_WAIT_NANOS-Math.max(0,elapsed)+999999L)/1000000L;
        }
        synchronized void received(long attempt){if(attempt==token)pending=false;}
    }
}
