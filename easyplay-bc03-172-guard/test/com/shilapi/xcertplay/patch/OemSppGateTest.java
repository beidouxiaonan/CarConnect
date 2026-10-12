package com.shilapi.xcertplay.patch;
import org.junit.Test;
import static org.junit.Assert.*;

public class OemSppGateTest {
    @Test public void stale172FlagNoLongerStopsBeforeDataTest(){
        assertFalse(OemSppGate.mayAttach(false,14999999999L,true));
        assertTrue(OemSppGate.mayAttach(false,15000000000L,true));
    }
    @Test public void unverifiedFirmwareRetainsOriginalGate(){
        assertFalse(OemSppGate.mayAttach(false,60000000000L,false));
    }
    @Test public void realStatusDoesNotWait(){assertTrue(OemSppGate.mayAttach(true,0,false));}
    @Test public void noFlagDoesNotAttachBeforeVfWindow(){assertFalse(OemSppGate.mayAttach(false,0,true));}
    @Test public void noReplyDoesNotFloodNativeWithVf(){
        OemSppGate.RetryWindow w=new OemSppGate.RetryWindow();
        long t=100000000000L;w.sent(t);
        assertEquals(45000L,w.remainingMillis(t));
        assertEquals(22000L,w.remainingMillis(t+23000000000L));
        assertEquals(1L,w.remainingMillis(t+44999999999L));
        assertEquals(0L,w.remainingMillis(t+45000000000L));
    }
    @Test public void successfulDataCanReconnectImmediately(){
        OemSppGate.RetryWindow w=new OemSppGate.RetryWindow();long token=w.sent(100);w.received(token);
        assertEquals(0L,w.remainingMillis(200));
    }
    @Test public void clockOriginNeedNotBePositive(){
        OemSppGate.RetryWindow w=new OemSppGate.RetryWindow();w.sent(-100000000000L);
        assertEquals(44000L,w.remainingMillis(-99000000000L));
    }
    @Test public void freshWindowAllowsFirstAttempt(){assertEquals(0L,new OemSppGate.RetryWindow().remainingMillis(0));}
    @Test public void closingOldStreamDoesNotClearNewPendingRequest(){
        OemSppGate.RetryWindow w=new OemSppGate.RetryWindow();long old=w.sent(100);
        long current=w.sent(200);w.received(old);
        assertEquals(45000L,w.remainingMillis(200));
        w.received(current);assertEquals(0L,w.remainingMillis(200));
    }
    @Test public void noDataProbeWaitsFullEightSeconds(){assertFalse(OemSppGate.probeExpired(true,0,7999999999L));}
    @Test public void noDataProbeExpiresAtEightSeconds(){assertTrue(OemSppGate.probeExpired(true,0,8000000000L));}
    @Test public void receivedDataCancelsNoDataDeadline(){assertFalse(OemSppGate.probeExpired(true,1,60000000000L));}
    @Test public void ordinaryStatusSessionHasNoProbeDeadline(){assertFalse(OemSppGate.probeExpired(false,0,60000000000L));}
    @Test public void negativeElapsedDoesNotPrematurelyExpireProbe(){assertFalse(OemSppGate.probeExpired(true,0,-1));}
}
