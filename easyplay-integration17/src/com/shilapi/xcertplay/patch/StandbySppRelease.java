package com.shilapi.xcertplay.patch;
/** The port exposes exactly one indexed disconnect. No reset, native VH, VF or reconnect API. */
final class StandbySppRelease {
    interface Port extends SppIdleRecovery.Port {
        void prepareRequest()throws Exception;
        void checkLease()throws Exception;
        void disconnectOne()throws Exception;
    }
    static void run(Port p,SppIdleRecovery.Clock guard)throws Exception{
        guard.check();p.validate();guard.check();
        boolean busy=p.connected();guard.check();
        if(busy){
            p.validate();p.checkLease();guard.check();p.prepareRequest();
            guard.check();p.validate();p.checkLease();guard.check();p.disconnectOne();guard.check();
        }
        SppIdleRecovery.confirm(p,guard);
    }
}
