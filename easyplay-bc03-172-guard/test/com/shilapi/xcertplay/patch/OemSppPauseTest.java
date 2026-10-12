package com.shilapi.xcertplay.patch;
import com.shilapi.xcertplay.legacy.LegacySessionService;
import com.shilapi.xcertplay.orchestration.CarPlayController;
import org.junit.Test;import static org.junit.Assert.*;
public class OemSppPauseTest {
    @Test public void sppFailurePausesOnlyCurrentServiceWithoutSavingAutomaticOff(){
        LegacySessionService s=new LegacySessionService();OemSppPause.mark(s.controller,"timeout");
        OemSppPause.failed(s);assertEquals(1,s.stops);assertFalse(s.lastRetain);assertFalse(s.automatic);
        assertTrue(s.logs.get(s.logs.size()-1).contains("已选 iPhone 保留"));
    }
    @Test public void ordinaryFailureRetainsOriginalRetry(){LegacySessionService s=new LegacySessionService();OemSppPause.failed(s);assertTrue(s.lastRetain);assertEquals(1,s.stops);}
    @Test public void oldControllerCannotPauseReplacement(){LegacySessionService s=new LegacySessionService();OemSppPause.mark(s.controller,"old");s.controller=new CarPlayController();OemSppPause.failed(s);assertTrue(s.lastRetain);}
    @Test public void wiredFailureRetainsOriginalRetry(){LegacySessionService s=new LegacySessionService();s.wired=true;OemSppPause.mark(s.controller,"old");OemSppPause.failed(s);assertTrue(s.lastRetain);}
    @Test public void noControllerRetainsOriginalRetry(){LegacySessionService s=new LegacySessionService();s.controller=null;OemSppPause.mark(null,"invalid");OemSppPause.failed(s);assertTrue(s.lastRetain);}
    @Test public void consumedPauseDoesNotAffectNextSession(){LegacySessionService s=new LegacySessionService();OemSppPause.mark(s.controller,"blocked");OemSppPause.failed(s);s.controller=new CarPlayController();s.closing=false;OemSppPause.failed(s);assertTrue(s.lastRetain);}
    @Test public void synchronousReplacementDuringRecordIsProtected(){
        final LegacySessionService s=new LegacySessionService();OemSppPause.mark(s.controller,"blocked");
        s.recordListener=new Runnable(){public void run(){s.controller=new CarPlayController();s.generation++;}};
        OemSppPause.failed(s);assertEquals(0,s.stops);
    }
    @Test public void replacementDuringCloseDoesNotReceiveOldPauseMessage(){
        final LegacySessionService s=new LegacySessionService();OemSppPause.mark(s.controller,"blocked");
        s.listener=new Runnable(){public void run(){s.controller=new CarPlayController();s.generation++;s.closing=false;}};
        OemSppPause.failed(s);assertEquals(1,s.stops);assertEquals(1,s.logs.size());
    }
}
