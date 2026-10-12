package com.shilapi.xcertplay.patch;

/** A missing OEM flag permits a bounded data test, never a successful iAP2 result. */
public final class OemSppGate {
    static final long STATUS_WAIT_NANOS=15000000000L;
    static final long PROBE_WAIT_NANOS=8000000000L;
    private static final long RETRY_WAIT_NANOS=45000000000L;
    private OemSppGate() {}
    static boolean mayAttach(boolean spp,long elapsed,boolean verified172) {
        return spp || (verified172 && elapsed>=STATUS_WAIT_NANOS);
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
