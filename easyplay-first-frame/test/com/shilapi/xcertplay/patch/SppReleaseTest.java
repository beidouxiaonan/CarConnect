package com.shilapi.xcertplay.patch;
import java.io.IOException;
import org.junit.Test;import static org.junit.Assert.*;
public class SppReleaseTest {
    private static class F implements SppRelease.Port,SppRelease.Monitor,SppRelease.Time {
        long tick,clearAt=300000000L; int commands; boolean initial=true,fail,cancel,cancelAfter;String last;
        public boolean connected(){return initial&&(commands==0||tick<clearAt);}
        public void disconnect()throws Exception{commands++;if(fail)throw new IOException("binder failure");if(cancelAfter)cancel=true;}
        public void check()throws Exception{if(cancel)throw new IOException("cancelled");}
        public void log(String s){last=s;}
        public long now(){return tick;}
        public void pause(){tick+=150000000L;}
    }
    private static void rejects(SppRelease r,boolean enabled,boolean owned,F f,String text)throws Exception {
        try{r.ensureFree(enabled,owned,f,f,f);fail("unexpected continuation");}
        catch(IOException e){assertTrue(e.getMessage(),e.getMessage().contains(text));}
    }
    @Test public void disabledNeverDisconnectsOccupiedChannel()throws Exception{F f=new F();rejects(new SppRelease(),false,false,f,"开关未开启");assertEquals(0,f.commands);}
    @Test public void freeChannelRequiresNoDisconnectEvenWhenDisabled()throws Exception{F f=new F();f.initial=false;assertFalse(new SppRelease().ensureFree(false,false,f,f,f));assertEquals(0,f.commands);}
    @Test public void ownSessionProtectedWhenEnabled()throws Exception{F f=new F();rejects(new SppRelease(),true,true,f,"自己");assertEquals(0,f.commands);}
    @Test public void waitsForAcknowledgedStableRelease()throws Exception{F f=new F();assertTrue(new SppRelease().ensureFree(true,false,f,f,f));assertEquals(1,f.commands);assertEquals(600000000L,f.tick);assertTrue(f.last.startsWith("C3"));}
    @Test public void returnedVoidDoesNotMeanReleased()throws Exception{F f=new F();f.clearAt=Long.MAX_VALUE;rejects(new SppRelease(),true,false,f,"超时");assertEquals(1,f.commands);assertTrue(f.tick>=SppRelease.WAIT);assertFalse(f.last.startsWith("C3"));}
    @Test public void temporaryFalseStateDoesNotReleaseGate()throws Exception{
        F f=new F(){public boolean connected(){if(commands==0)return true;return tick<300000000L||(tick>=450000000L&&tick<900000000L);}};
        assertTrue(new SppRelease().ensureFree(true,false,f,f,f));assertEquals(1200000000L,f.tick);assertEquals(1,f.commands);
    }
    @Test public void cancelledBeforeDispatchDoesNotDisconnect()throws Exception{F f=new F();f.cancel=true;rejects(new SppRelease(),true,false,f,"cancelled");assertEquals(0,f.commands);}
    @Test public void cancellationDuringWaitCannotContinue()throws Exception{F f=new F();f.cancelAfter=true;rejects(new SppRelease(),true,false,f,"cancelled");assertEquals(1,f.commands);assertFalse(f.last.startsWith("C3"));}
    @Test public void stateMayReleaseWithoutDispatch()throws Exception{F f=new F(){int reads;public boolean connected(){return ++reads==1;}};assertFalse(new SppRelease().ensureFree(true,false,f,f,f));assertEquals(0,f.commands);}
    @Test public void freedChannelMayConnectDuringCooldown()throws Exception{F f=new F();SppRelease r=new SppRelease();assertTrue(r.ensureFree(true,false,f,f,f));f.initial=false;assertFalse(r.ensureFree(true,false,f,f,f));assertEquals(1,f.commands);}
    @Test public void subsequentOccupiedChannelCanRetryAtCooldownBoundary()throws Exception{F f=new F();SppRelease r=new SppRelease();assertTrue(r.ensureFree(true,false,f,f,f));f.tick=SppRelease.COOLDOWN;f.clearAt=f.tick+300000000L;assertTrue(r.ensureFree(true,false,f,f,f));assertEquals(2,f.commands);}

    private static class N extends F implements SppRelease.NativePort {
        int nativeCommands;long nativeClearAt;boolean nativeFail,cancelNative;
        N(){clearAt=Long.MAX_VALUE;}
        @Override public boolean connected(){return nativeCommands==0||tick<nativeClearAt;}
        public void disconnectAll()throws Exception {
            nativeCommands++;nativeClearAt=tick+450000000L;
            if(nativeFail)throw new IOException("native write denied");
            if(cancelNative)cancel=true;
        }
    }
    @Test public void noopBinderFallsBackExactlyOnceThenWaitsForConfirmedRelease()throws Exception {
        N n=new N();assertTrue(new SppRelease().ensureFree(true,false,n,n,n));
        assertEquals(1,n.commands);assertEquals(1,n.nativeCommands);assertTrue(n.last.startsWith("C5"));
        assertTrue(n.tick>SppRelease.WAIT&&n.tick<SppRelease.WAIT+SppRelease.NATIVE_WAIT);
    }
    @Test public void workingBinderDoesNotTriggerNativeFallback()throws Exception {
        N n=new N(){public boolean connected(){return tick<300000000L;}};
        assertTrue(new SppRelease().ensureFree(true,false,n,n,n));assertEquals(0,n.nativeCommands);
    }
    @Test public void nativeWriteReturnIsNotReleaseAcknowledgement()throws Exception {
        N n=new N(){public boolean connected(){return true;}};
        rejects(new SppRelease(),true,false,n,"底层清理超时");assertEquals(1,n.nativeCommands);assertFalse(n.last.startsWith("C5"));
    }
    @Test public void disabledAndOwnedSessionsNeverTriggerNativeRelease()throws Exception {
        N n=new N();rejects(new SppRelease(),false,false,n,"开关未开启");rejects(new SppRelease(),true,true,n,"自己");
        assertEquals(0,n.commands);assertEquals(0,n.nativeCommands);
    }
    @Test public void cancellationAfterNativeWriteCannotContinueToConnect()throws Exception {
        N n=new N();n.cancelNative=true;rejects(new SppRelease(),true,false,n,"cancelled");assertEquals(1,n.nativeCommands);assertFalse(n.last.startsWith("C5"));
    }
    @Test public void lateBinderReleaseFinishesSettlingWithoutNativeCommand()throws Exception {
        N n=new N(){public boolean connected(){return tick<4950000000L;}};
        assertTrue(new SppRelease().ensureFree(true,false,n,n,n));assertEquals(0,n.nativeCommands);assertTrue(n.last.startsWith("C3"));
    }
    @Test public void unstableLateReleaseHasBoundedOverallDeadline()throws Exception {
        N n=new N(){int reads;public boolean connected(){reads++;return tick<5000000000L||reads%2==0;}};
        try {new SppRelease().ensureFree(true,false,n,n,n);fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("超时"));}
        assertTrue(n.tick<=SppRelease.WAIT+SppRelease.NATIVE_WAIT+SppRelease.SETTLE+300000000L);assertTrue(n.nativeCommands<=1);
    }

    @Test public void cooldownWaitsWithoutRepeatedCommandsThenAttemptsOnce()throws Exception {
        F f=new F(){public boolean connected(){return true;}};SppRelease r=new SppRelease();
        rejects(r,true,false,f,"超时");assertEquals(1,f.commands);
        rejects(r,true,false,f,"超时");assertEquals(2,f.commands);assertTrue(f.tick>=SppRelease.COOLDOWN+SppRelease.WAIT);
    }
    @Test public void lateReleaseDuringCooldownNeedsNoSecondDisconnect()throws Exception {
        F f=new F();f.fail=true;SppRelease r=new SppRelease();rejects(r,true,false,f,"binder failure");
        f.fail=false;f.clearAt=2000000000L;
        assertFalse(r.ensureFree(true,false,f,f,f));assertEquals(1,f.commands);assertTrue(f.tick<SppRelease.COOLDOWN);
    }
    @Test public void cancellationInCooldownNeverRepeatsDisconnect()throws Exception {
        F f=new F(){public boolean connected(){return true;}public void pause(){super.pause();if(tick>10000000000L)cancel=true;}};
        SppRelease r=new SppRelease();rejects(r,true,false,f,"超时");rejects(r,true,false,f,"cancelled");assertEquals(1,f.commands);
    }
    @Test public void unstableReleaseDuringCooldownWaitsForStableState()throws Exception {
        F f=new F(){public boolean connected(){return commands==0||tick<2000000000L||tick>=2150000000L&&tick<3000000000L;}};
        f.fail=true;SppRelease r=new SppRelease();rejects(r,true,false,f,"binder failure");f.fail=false;
        assertFalse(r.ensureFree(true,false,f,f,f));assertEquals(1,f.commands);assertEquals(3300000000L,f.tick);
    }
}
