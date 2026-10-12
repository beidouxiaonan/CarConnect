package com.shilapi.xcertplay.patch;
import java.io.IOException;import org.junit.Test;import static org.junit.Assert.*;
public class SppVfWindowTest {
    @Test public void initialAndEstablishedConnectionsDoNotBlockCleanup()throws Exception {
        SppVfWindow w=new SppVfWindow();w.check(1);w.sent(10);w.established();w.check(11);
    }
    @Test public void failedOrClosedClientDoesNotErasePendingNativeRequest()throws Exception {
        SppVfWindow w=new SppVfWindow();w.sent(10);
        for(long n:new long[]{10,11,10+SppVfWindow.WAIT-1}) {
            try{w.check(n);fail();}catch(IOException e){assertTrue(e.getMessage().contains("上次 VF"));}
        }
    }
    @Test public void exactCooldownBoundaryAllowsSingleNewAttempt()throws Exception {
        SppVfWindow w=new SppVfWindow();w.sent(10);w.check(10+SppVfWindow.WAIT);
        w.sent(10+SppVfWindow.WAIT);try{w.check(11+SppVfWindow.WAIT);fail();}catch(IOException expected){}
    }
}
