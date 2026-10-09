package com.shilapi.xcertplay.patch;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.shilapi.xcertplay.patch.FirstFramePolicy.Action.*;
public class FirstFramePolicyTest {
    private FirstFramePolicy ready() { FirstFramePolicy p = new FirstFramePolicy(); p.arm(1); return p; }
    @Test public void gracePeriodIncludesHandshakeAndPermissionTime() {
        FirstFramePolicy p=ready(); assertEquals(WAIT,p.check(1,100,true,false,true));
        assertEquals(WAIT,p.check(1,60099,true,false,true)); assertEquals(RETRY,p.check(1,60100,true,false,true));
    }
    @Test public void alreadyRenderedSessionIsNeverStopped() {
        FirstFramePolicy p=ready();p.check(1,0,true,false,true);
        assertEquals(SUCCESS,p.check(1,90000,true,true,true));assertEquals(0,p.retries());
    }
    @Test public void backgroundNeverUsesTimeout() {
        FirstFramePolicy p=ready(); assertEquals(WAIT,p.check(1,999999,true,false,false));assertEquals(0,p.retries());
        assertEquals(WAIT,p.check(1,1000000,true,false,true));
    }
    @Test public void foregroundMustBeContinuous() {
        FirstFramePolicy p=ready();p.check(1,0,true,false,true);p.check(1,59000,true,false,false);
        assertEquals(WAIT,p.check(1,90000,true,false,true));assertEquals(WAIT,p.check(1,149999,true,false,true));
        assertEquals(RETRY,p.check(1,150000,true,false,true));
    }
    @Test public void repeatedReadyDoesNotExtendDeadline() {
        FirstFramePolicy p=ready();p.check(1,0,true,false,true);assertFalse(p.arm(1));
        assertEquals(RETRY,p.check(1,60000,true,false,true));
    }
    @Test public void oldGenerationCannotStopNewSession() {
        FirstFramePolicy p=ready();p.check(1,0,true,false,true);p.arm(2);
        assertEquals(CANCEL,p.check(1,60000,true,false,true));assertEquals(WAIT,p.check(2,60000,true,false,true));
    }
    @Test public void automaticRetryKeepsBudget() {
        FirstFramePolicy p=ready();p.check(1,0,true,false,true);p.check(1,60000,true,false,true);p.cancel(false);p.arm(3);
        p.check(3,70000,true,false,true);assertEquals(RETRY,p.check(3,130000,true,false,true));assertEquals(2,p.retries());
    }
    @Test public void thirdNoFrameAttemptStaysOpenForLatePhone() {
        FirstFramePolicy p=ready();
        for(int i=1;i<=3;i++){p.arm(i);p.check(i,i*100000,true,false,true);
            assertEquals(i<=2?RETRY:LIMIT,p.check(i,i*100000+60000,true,false,true));}
        assertEquals(2,p.retries()); assertEquals(CANCEL,p.check(3,999999,true,false,true));
    }
    @Test public void manualStopStartsNewBudget() {
        FirstFramePolicy p=ready();p.check(1,0,true,false,true);p.check(1,60000,true,false,true);p.cancel(true);
        p.arm(2);p.check(2,70000,true,false,true);assertEquals(RETRY,p.check(2,130000,true,false,true));assertEquals(1,p.retries());
    }
    @Test public void successfulRetryResetsBudget() {
        FirstFramePolicy p=ready();p.check(1,0,true,false,true);p.check(1,60000,true,false,true);p.arm(2);
        assertEquals(SUCCESS,p.check(2,70000,true,true,true));assertEquals(0,p.retries());
    }
    @Test public void wiredAutoOffOemOffOrDisabledCancels() {
        FirstFramePolicy p=ready();p.check(1,0,true,false,true);assertEquals(CANCEL,p.check(1,90000,false,false,true));
        assertEquals(0,p.retries());assertEquals(CANCEL,p.check(1,150000,true,false,true));
    }
    @Test public void duplicateTimeoutCannotSpendTwoRetries() {
        FirstFramePolicy p=ready();p.check(1,0,true,false,true);assertEquals(RETRY,p.check(1,60000,true,false,true));
        assertEquals(CANCEL,p.check(1,60001,true,false,true));assertEquals(1,p.retries());
    }
}
