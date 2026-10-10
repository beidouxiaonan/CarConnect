package com.shilapi.xcertplay.patch;
import java.io.IOException;
import org.junit.Test;import static org.junit.Assert.*;
public class SppGuardTest {
    private static class F implements SppRelease.NativePort,SppRelease.Monitor,SppRelease.Time,SppRelease.Control {
        long tick,clearAt=Long.MAX_VALUE;int binder,nativeCalls,begin,clear;
        boolean occupied=true,pending,nativeAllowed,cancel,beginFail,clearFail,binderFail,nativeFail;
        String log;
        public boolean connected(){return occupied&&tick<clearAt;}
        public void disconnect()throws Exception{assertTrue(pending);binder++;if(binderFail)throw new IOException("binder");}
        public void disconnectAll()throws Exception{assertTrue(pending);nativeCalls++;if(nativeFail)throw new IOException("native");}
        public void check()throws Exception{if(cancel)throw new IOException("cancelled");}
        public void log(String value){log=value;}
        public long now(){return tick;}
        public void pause(){tick+=150000000L;}
        public boolean pending(){return pending;}
        public boolean nativeEnabled(){return nativeAllowed;}
        public void beforeRequest()throws Exception{begin++;if(beginFail)throw new SppRelease.BlockedException("journal failed");pending=true;}
        public void released()throws Exception{clear++;if(clearFail)throw new SppRelease.BlockedException("clear failed");pending=false;}
        boolean run(SppRelease r,boolean enabled,boolean own)throws Exception{return r.ensureFree(enabled,own,this,this,this,this);}
    }
    private static void rejects(F f,SppRelease r,boolean enabled,boolean owned,String text)throws Exception{
        try{f.run(r,enabled,owned);fail("Gate unexpectedly opened");}catch(IOException e){assertTrue(e.getMessage(),e.getMessage().contains(text));}
    }
    @Test public void cleanChannelConnectsWithoutDestructiveRequestEvenWhenCleanupOff()throws Exception{F f=new F();f.occupied=false;assertFalse(f.run(new SppRelease(),false,false));assertEquals(0,f.binder);assertEquals(0,f.nativeCalls);assertEquals(SppRelease.SETTLE,f.tick);}
    @Test public void stableIdleAutomaticallyClearsPreviousFailure()throws Exception{F f=new F();f.pending=true;f.occupied=false;assertFalse(f.run(new SppRelease(),true,false));assertFalse(f.pending);assertEquals(0,f.begin);}
    @Test public void transientIdleDoesNotClearFailureOrAuthorizeConnection()throws Exception{F f=new F(){public boolean connected(){return tick>=150000000L;}};f.pending=true;rejects(f,new SppRelease(),true,false,"上次");assertTrue(f.pending);assertEquals(0,f.binder);assertEquals(0,f.clear);}
    @Test public void defaultRecoveryNeverSendsNativeCommand()throws Exception{F f=new F();rejects(f,new SppRelease(),true,false,"底层恢复未开启");assertEquals(1,f.binder);assertEquals(0,f.nativeCalls);assertTrue(f.pending);}
    @Test public void failedEpisodeCannotRepeatCommandsAfterCooldown()throws Exception{F f=new F();SppRelease r=new SppRelease();rejects(f,r,true,false,"底层恢复未开启");f.tick+=3*SppRelease.COOLDOWN;rejects(f,r,true,false,"上次");assertEquals(1,f.binder);assertEquals(0,f.nativeCalls);}
    @Test public void newReleaseObjectStillHonorsSavedFailure()throws Exception{F f=new F();rejects(f,new SppRelease(),true,false,"底层恢复未开启");rejects(f,new SppRelease(),true,false,"上次");assertEquals(1,f.binder);}
    @Test public void nativeOptInStillHasSingleDispatchAndStopsWhenUnconfirmed()throws Exception{F f=new F();f.nativeAllowed=true;SppRelease r=new SppRelease();rejects(f,r,true,false,"底层清理超时");f.tick+=SppRelease.COOLDOWN;rejects(f,r,true,false,"上次");assertEquals(1,f.binder);assertEquals(1,f.nativeCalls);}
    @Test public void acknowledgedBinderReleaseClearsJournalWithoutNative()throws Exception{F f=new F();f.clearAt=450000000L;assertTrue(f.run(new SppRelease(),true,false));assertFalse(f.pending);assertEquals(1,f.binder);assertEquals(0,f.nativeCalls);assertTrue(f.log.startsWith("C3"));}
    @Test public void acknowledgedNativeReleaseMayContinue()throws Exception{F f=new F(){public void disconnectAll()throws Exception{super.disconnectAll();clearAt=tick+450000000L;}};f.nativeAllowed=true;assertTrue(f.run(new SppRelease(),true,false));assertFalse(f.pending);assertEquals(1,f.nativeCalls);assertTrue(f.log.startsWith("C5"));}
    @Test public void clearJournalFailureStopsBeforeContinuation()throws Exception{F f=new F();f.clearAt=300000000L;f.clearFail=true;rejects(f,new SppRelease(),true,false,"clear failed");assertTrue(f.pending);}
    @Test public void journalMustPersistBeforeAnyCommand()throws Exception{F f=new F();f.beginFail=true;rejects(f,new SppRelease(),true,false,"journal failed");assertEquals(0,f.binder);assertEquals(0,f.nativeCalls);}
    @Test public void binderErrorRetainsPending()throws Exception{F f=new F();f.binderFail=true;rejects(f,new SppRelease(),true,false,"binder");assertTrue(f.pending);assertEquals(1,f.binder);assertEquals(0,f.nativeCalls);}
    @Test public void nativeWriteErrorRetainsPending()throws Exception{F f=new F();f.nativeAllowed=true;f.nativeFail=true;rejects(f,new SppRelease(),true,false,"native");assertTrue(f.pending);assertEquals(1,f.nativeCalls);}
    @Test public void cancellationBeforeDispatchMakesNoChange()throws Exception{F f=new F();f.cancel=true;rejects(f,new SppRelease(),true,false,"cancelled");assertFalse(f.pending);assertEquals(0,f.begin);}
    @Test public void cancellationAfterBinderDoesNotDispatchNative()throws Exception{F f=new F(){public void disconnect()throws Exception{super.disconnect();cancel=true;}};f.nativeAllowed=true;rejects(f,new SppRelease(),true,false,"cancelled");assertTrue(f.pending);assertEquals(0,f.nativeCalls);}
    @Test public void cancellationAfterNativeCannotContinue()throws Exception{F f=new F(){public void disconnectAll()throws Exception{super.disconnectAll();cancel=true;}};f.nativeAllowed=true;rejects(f,new SppRelease(),true,false,"cancelled");assertTrue(f.pending);assertEquals(1,f.nativeCalls);}
    @Test public void ownLiveChannelCannotBeCleanedOrClearJournal()throws Exception{F f=new F();f.occupied=false;f.pending=true;rejects(f,new SppRelease(),true,true,"自己");assertTrue(f.pending);assertEquals(0,f.clear);}
    @Test public void disabledCleanupStopsWithoutJournalOrCommands()throws Exception{F f=new F();rejects(f,new SppRelease(),false,false,"开关未开启");assertEquals(0,f.binder);assertEquals(0,f.begin);}
    @Test public void optOutDuringBinderWaitPreventsNative()throws Exception{F f=new F(){public void pause(){super.pause();nativeAllowed=false;}};f.nativeAllowed=true;rejects(f,new SppRelease(),true,false,"底层恢复未开启");assertEquals(0,f.nativeCalls);}
    @Test public void lateReleaseSettlesWithoutNative()throws Exception{F f=new F();f.nativeAllowed=true;f.clearAt=4950000000L;assertTrue(f.run(new SppRelease(),true,false));assertEquals(0,f.nativeCalls);}
    @Test public void manuallyRearmedEpisodeStillWaitsForCooldown()throws Exception{F f=new F();SppRelease r=new SppRelease();rejects(f,r,true,false,"底层恢复未开启");f.pending=false;rejects(f,r,true,false,"底层恢复未开启");assertEquals(2,f.binder);assertTrue(f.tick>=SppRelease.COOLDOWN+SppRelease.WAIT);}
    @Test public void cancellationDuringRearmedCooldownDoesNotDispatchAgain()throws Exception{F f=new F(){public void pause(){super.pause();if(tick>10000000000L)cancel=true;}};SppRelease r=new SppRelease();rejects(f,r,true,false,"底层恢复未开启");f.pending=false;rejects(f,r,true,false,"cancelled");assertEquals(1,f.binder);}
    @Test public void releaseDuringCooldownNeedsNoSecondRequest()throws Exception{F f=new F();SppRelease r=new SppRelease();rejects(f,r,true,false,"底层恢复未开启");f.pending=false;f.clearAt=8000000000L;assertFalse(f.run(r,true,false));assertEquals(1,f.binder);assertFalse(f.pending);}
    @Test public void flappingStateCannotAuthorizeRelease()throws Exception{F f=new F(){int reads;public boolean connected(){return tick<5000000000L||++reads%2==0;}};f.nativeAllowed=true;rejects(f,new SppRelease(),true,false,"超时");assertTrue(f.pending);assertTrue(f.tick<=SppRelease.WAIT+SppRelease.NATIVE_WAIT+SppRelease.SETTLE+300000000L);}
}
