package com.shilapi.xcertplay.patch;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;
public class StandbySppTest {
    static class Fake implements StandbySppRelease.Port,SppIdleRecovery.Clock {
        long time;int disconnects,prepares,clears;boolean busy=true,pending,failCommit,cancel,changed,blockRead,release=true;
        public long now(){return time;}
        public void pause(){time+=100000000L;}
        public void check()throws Exception{if(cancel)throw new IOException("screen on");}
        public void validate()throws Exception{if(changed)throw new IOException("phone/controller changed");}
        public boolean connected(){if(blockRead)cancel=true;return busy;}
        public void checkLease()throws Exception{if(changed)throw new IOException("new SPP observed");}
        public void prepareRequest()throws Exception{prepares++;if(failCommit)throw new IOException("disk failed");pending=true;}
        public void disconnectOne(){disconnects++;if(release)busy=false;}
        public void clearJournal(){clears++;pending=false;}
    }
    @Test public void idleDoesNotDisconnectAndNeedsStability()throws Exception{
        Fake f=new Fake();f.busy=false;StandbySppRelease.run(f,f);assertEquals(0,f.disconnects);assertEquals(0,f.prepares);assertEquals(600000000L,f.time);assertEquals(1,f.clears);
    }
    @Test public void oneRequestNeedsObservedReleaseBeforeClearing()throws Exception{
        Fake f=new Fake();StandbySppRelease.run(f,f);assertEquals(1,f.disconnects);assertEquals(1,f.prepares);assertFalse(f.pending);assertEquals(1,f.clears);
    }
    @Test public void failedReleasePreservesPendingAndNeverLoopsCommands()throws Exception{
        Fake f=new Fake();f.release=false;try{StandbySppRelease.run(f,f);fail();}catch(IOException expected){}
        assertEquals(1,f.disconnects);assertTrue(f.pending);assertEquals(0,f.clears);
    }
    @Test public void wakingDuringBlockedQueryCannotDispatchLater()throws Exception{
        Fake f=new Fake();f.blockRead=true;try{StandbySppRelease.run(f,f);fail();}catch(IOException expected){}
        assertEquals(0,f.prepares);assertEquals(0,f.disconnects);
    }
    @Test public void journalFailureCannotSendDisconnect()throws Exception{
        Fake f=new Fake();f.failCommit=true;try{StandbySppRelease.run(f,f);fail();}catch(IOException expected){}assertEquals(0,f.disconnects);
    }
    @Test public void observationChangeDuringJournalWriteCannotDisconnectNewOwner()throws Exception{
        Fake f=new Fake(){public void prepareRequest()throws Exception{super.prepareRequest();changed=true;}};
        try{StandbySppRelease.run(f,f);fail();}catch(IOException expected){}assertEquals(0,f.disconnects);assertTrue(f.pending);
    }
    @Test public void wakeDuringJournalWriteCannotClearOrDisconnect()throws Exception{
        Fake f=new Fake(){public void prepareRequest()throws Exception{super.prepareRequest();cancel=true;}};
        try{StandbySppRelease.run(f,f);fail();}catch(IOException expected){}assertEquals(0,f.disconnects);assertEquals(0,f.clears);assertTrue(f.pending);
    }
}
