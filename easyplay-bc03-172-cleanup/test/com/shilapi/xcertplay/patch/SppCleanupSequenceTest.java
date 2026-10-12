package com.shilapi.xcertplay.patch;
import java.io.IOException;
import org.junit.Test;import static org.junit.Assert.*;
public class SppCleanupSequenceTest {
    private static class F implements SppRelease.Port,SppRelease.Monitor,SppRelease.Time {
        long tick,clearAt=300000000L;int queries,commands;boolean never;
        public boolean connected(){queries++;return commands==0||never||tick<clearAt;}
        public void disconnect(){commands++;}
        public void check(){}
        public void log(String value){}
        public long now(){return tick;}
        public void pause(){tick+=150000000L;}
    }
    private static boolean prepare(F f,OemSppGate.RetryWindow retry)throws Exception{
        return SppCleanupSequence.prepare(new SppRelease(),true,false,f,f,f,retry);
    }
    @Test public void pendingVfPreventsEvenCheckingOrDisconnectingSpp()throws Exception {
        F f=new F();OemSppGate.RetryWindow r=new OemSppGate.RetryWindow();r.sent(f.tick);
        try{prepare(f,r);fail();}catch(IOException e){assertTrue(e.getMessage().contains("暂不清理"));}
        assertEquals(0,f.commands);assertEquals(0,f.queries);
    }
    @Test public void staleStreamCloseCannotAllowCleanupOfNewVf()throws Exception {
        F f=new F();OemSppGate.RetryWindow r=new OemSppGate.RetryWindow();long old=r.sent(0);f.tick=100;r.sent(f.tick);r.received(old);
        try{prepare(f,r);fail();}catch(IOException e){assertTrue(e.getMessage().contains("冷却"));}
        assertEquals(0,f.commands);assertEquals(0,f.queries);
    }
    @Test public void expiredVfAllowsCleanupAndRetains172BoundedProbeRules()throws Exception {
        F f=new F();OemSppGate.RetryWindow r=new OemSppGate.RetryWindow();r.sent(0);f.tick=45000000000L;f.clearAt=f.tick+300000000L;
        assertTrue(prepare(f,r));assertEquals(1,f.commands);assertEquals(45600000000L,f.tick);
        assertFalse(OemSppGate.mayAttach(false,14999999999L,true));
        assertTrue(OemSppGate.mayAttach(false,15000000000L,true));
        assertFalse(OemSppGate.mayAttach(false,15000000000L,false));
    }
    @Test public void failedCleanupDoesNotCreateVfAttemptOrPassToProbe()throws Exception {
        F f=new F();f.never=true;OemSppGate.RetryWindow r=new OemSppGate.RetryWindow();
        try{prepare(f,r);fail();}catch(IOException e){assertTrue(e.getMessage().contains("未发送 VF"));}
        assertEquals(1,f.commands);assertEquals(0,r.remainingMillis(f.tick));
    }
    private static final class N extends F implements SppRelease.NativePort {
        int nativeCommands;OemSppGate.RetryWindow newlyPending;
        public boolean connected(){queries++;return never||nativeCommands==0||tick<clearAt;}
        public void disconnect(){super.disconnect();if(newlyPending!=null)newlyPending.sent(tick);}
        public void disconnectAll(){nativeCommands++;}
    }
    @Test public void expiredVfAllowsNativeRecoveryAndRetainsBoundedProbe()throws Exception {
        N f=new N();OemSppGate.RetryWindow r=new OemSppGate.RetryWindow();r.sent(0);
        f.tick=45000000000L;f.clearAt=f.tick+5400000000L;
        assertTrue(prepare(f,r));assertEquals(1,f.commands);assertEquals(1,f.nativeCommands);
        assertEquals(50700000000L,f.tick);
        assertFalse(OemSppGate.mayAttach(false,14999999999L,true));
        assertTrue(OemSppGate.mayAttach(false,15000000000L,true));
        assertFalse(OemSppGate.mayAttach(false,15000000000L,false));
    }
    @Test public void pendingVfDuringBinderWaitPreventsNativeCommand()throws Exception {
        N f=new N();OemSppGate.RetryWindow r=new OemSppGate.RetryWindow();f.newlyPending=r;
        try{prepare(f,r);fail();}catch(IOException e){assertTrue(e.getMessage().contains("暂不清理"));}
        assertEquals(1,f.commands);assertEquals(0,f.nativeCommands);
    }
    @Test public void nativeWriteWithoutReleaseDoesNotCreateVfOrPassToProbe()throws Exception {
        N f=new N();f.never=true;OemSppGate.RetryWindow r=new OemSppGate.RetryWindow();
        try{prepare(f,r);fail();}catch(IOException e){assertTrue(e.getMessage().contains("未发送 VF"));}
        assertEquals(1,f.commands);assertEquals(1,f.nativeCommands);assertEquals(0,r.remainingMillis(f.tick));
        assertTrue(f.tick>=SppRelease.WAIT+SppRelease.NATIVE_WAIT);
    }
}
