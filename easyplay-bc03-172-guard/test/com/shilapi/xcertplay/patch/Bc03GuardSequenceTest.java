package com.shilapi.xcertplay.patch;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;
/** Exercises the production sequence that combines 1.7.2 VF gating and 0.1.10 recovery journal. */
public class Bc03GuardSequenceTest {
    private static class F implements SppRelease.NativePort,SppRelease.Monitor,SppRelease.Time,SppRelease.Control {
        long tick=100000000000L,clearAt=Long.MAX_VALUE;
        boolean occupied,pending,cancel;
        int disconnects,nativeCalls,begins,releases;
        final OemSppGate.RetryWindow retry=new OemSppGate.RetryWindow();
        public boolean connected(){return occupied&&tick<clearAt;}
        public void disconnect(){assertTrue(pending);disconnects++;}
        public void disconnectAll(){nativeCalls++;}
        public void check()throws Exception{if(cancel)throw new IOException("cancel");}
        public void log(String value){}
        public long now(){return tick;}
        public void pause(){tick+=150000000L;}
        public boolean pending(){return pending;}
        public boolean nativeEnabled(){return false;}
        public void beforeRequest(){pending=true;begins++;}
        public void released(){pending=false;releases++;}
        boolean run()throws Exception{return OemSppGate.prepare(new SppRelease(),true,false,this,this,this,retry,this);}
    }
    private void blocked(F f,String reason)throws Exception {
        try{f.run();fail("Connection unexpectedly authorized");}
        catch(IOException expected){assertTrue(expected.getMessage(),expected.getMessage().contains(reason));}
    }
    @Test public void idle172StartsNormallyWithoutCleanup()throws Exception {
        F f=new F();assertFalse(f.run());assertEquals(0,f.disconnects);assertEquals(0,f.nativeCalls);
    }
    @Test public void pendingVfCannotBeTornDownEvenIfOccupied()throws Exception {
        F f=new F();f.occupied=true;f.retry.sent(f.tick);blocked(f,"VF");
        assertEquals(0,f.disconnects);assertEquals(0,f.begins);
    }
    @Test public void journalStillBlocksAfterVfCooldownExpires()throws Exception {
        F f=new F();f.occupied=true;f.pending=true;f.retry.sent(f.tick);f.tick+=45000000000L;
        blocked(f,"上次");assertEquals(0,f.disconnects);assertTrue(f.pending);
    }
    @Test public void confirmedIdleClearsJournalAfterVfCooldown()throws Exception {
        F f=new F();f.pending=true;f.retry.sent(f.tick);f.tick+=45000000000L;
        assertFalse(f.run());assertFalse(f.pending);assertEquals(0,f.disconnects);
    }
    @Test public void oneConfirmedBinderReleaseAllows172VfStage()throws Exception {
        F f=new F();f.occupied=true;f.clearAt=f.tick+450000000L;
        assertTrue(f.run());assertEquals(1,f.disconnects);assertEquals(0,f.nativeCalls);assertFalse(f.pending);
    }
    @Test public void cancelledSequenceSendsNoCommands()throws Exception {
        F f=new F();f.occupied=true;f.cancel=true;blocked(f,"cancel");
        assertEquals(0,f.disconnects);assertEquals(0,f.begins);
    }
}
