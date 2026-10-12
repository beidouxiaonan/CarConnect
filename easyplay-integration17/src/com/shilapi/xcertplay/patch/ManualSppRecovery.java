package com.shilapi.xcertplay.patch;
/** A user permit belongs to this call only; the durable pending journal is never erased to arm it. */
final class ManualSppRecovery {
    private static final java.util.concurrent.atomic.AtomicReference<String> passivePhone=new java.util.concurrent.atomic.AtomicReference<String>();
    static void handoff(String phone){passivePhone.set(phone);}
    static boolean consumeHandoff(String phone){String expected=passivePhone.getAndSet(null);return expected!=null&&expected.equals(phone);}
    static boolean run(SppRelease release,SppRelease.Port port,SppRelease.Monitor monitor,
            SppRelease.Time time,final SppRelease.Control journal)throws Exception {
        return release.ensureFree(true,false,port,monitor,time,new SppRelease.Control(){
            public boolean pending(){return false;}
            public boolean nativeEnabled(){return journal.nativeEnabled();}
            public void beforeRequest()throws Exception{journal.beforeRequest();}
            // The caller must perform the final phone validation + 600ms idle check before clearing.
            public void released()throws Exception{monitor.check();}
        });
    }
}
