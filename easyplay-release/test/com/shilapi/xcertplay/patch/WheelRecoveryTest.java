package com.shilapi.xcertplay.patch;
import org.junit.Test;
import static org.junit.Assert.*;
import android.view.KeyEvent;
import android.os.SystemClock;

public class WheelRecoveryTest {
    private WheelEventClock.Times event(WheelEventClock c, int action, int repeat, long down, long time, long now) {
        return c.normalize(1,87,action,repeat,down,time,now);
    }
    @Test public void fixedTimestampsNeverPermanentlyBlockNextTrack() {
        WheelEventClock c=new WheelEventClock(); long last=-1;
        for(int i=0;i<100;i++) {
            long now=1000+i*500;
            WheelEventClock.Times t=event(c,0,0,0,0,now);
            assertTrue(t.down>last); last=t.down;
            assertEquals(t.down,event(c,1,0,0,0,now+40).down);
        }
    }
    @Test public void reusedDownWithDifferentEventTimeAndUpCanBeFast() {
        WheelEventClock c=new WheelEventClock();
        long first=event(c,0,0,10,100,1000).down;
        event(c,1,0,10,110,1010);
        assertNotEquals(first,event(c,0,0,10,150,1050).down);
    }
    @Test public void duplicatesAcrossActivityAndReceiverShareIdentity() {
        WheelEventClock c=new WheelEventClock();
        WheelEventClock.Times a=event(c,0,0,10,20,1000),b=event(c,0,0,10,20,1020);
        assertEquals(a.down,b.down); assertEquals(a.event,b.event);
    }
    @Test public void delayedDuplicateAfterUpDoesNotSendAnotherTrack() {
        WheelEventClock c=new WheelEventClock(); long a=event(c,0,0,10,20,1000).down;
        event(c,1,0,10,30,1010);
        assertEquals(a,event(c,0,0,10,20,1040).down);
    }
    @Test public void missingUpDoesNotLatchForever() {
        WheelEventClock c=new WheelEventClock(); long a=event(c,0,0,0,0,1000).down;
        assertNotEquals(a,event(c,0,0,0,0,2000).down);
    }
    @Test public void validRepeatRetainsLongPressDuration() {
        WheelEventClock c=new WheelEventClock(); long a=event(c,0,0,100,105,1000).down;
        WheelEventClock.Times r=event(c,0,1,100,905,1700);
        assertEquals(a,r.down); assertEquals(800,r.event-r.down);
        assertEquals(a,event(c,1,0,100,1005,1800).down);
    }
    @Test public void fixedClockLongPressUsesArrivalDuration() {
        WheelEventClock c=new WheelEventClock();long a=event(c,0,0,0,0,1000).down;
        WheelEventClock.Times r=event(c,0,1,0,0,1900);
        assertEquals(a,r.down); assertEquals(900,r.event-r.down);
    }
    @Test public void distinctPhysicalKeysAndDevicesDoNotShareHistory() {
        WheelEventClock c=new WheelEventClock();
        long a=c.normalize(1,87,0,0,0,0,1000).down;
        assertNotEquals(a,c.normalize(2,87,0,0,0,0,1000).down);
        assertNotEquals(a,c.normalize(1,88,0,0,0,0,1000).down);
    }
    @Test public void realRapidDownTimesRemainSeparate() {
        WheelEventClock c=new WheelEventClock();
        long a=event(c,0,0,100,100,1000).down;
        assertNotEquals(a,event(c,0,0,120,120,1020).down);
    }
    @Test public void unknownUpDoesNotFinishAnotherKey() {
        WheelEventClock c=new WheelEventClock();long a=event(c,0,0,100,100,1000).down;
        assertEquals(999,event(c,1,0,999,999,1010).down);
        assertEquals(a,event(c,0,1,100,100,1020).down);
    }
    @Test public void clockRollbackAndResetCannotRetainPermanentDuplicates() {
        WheelEventClock c=new WheelEventClock();long a=event(c,0,0,0,0,1000).down;
        assertNotEquals(a,event(c,0,0,0,0,900).down);
        c.clear();assertEquals(0,c.size());
    }
    @Test public void keyHistoryHasBoundedMemory() {
        WheelEventClock c=new WheelEventClock();
        for(int i=0;i<1000;i++)c.normalize(i,87,0,0,0,0,i);
        assertEquals(32,c.size());
    }
    @Test public void adapterKeepsFlagsScanSourceAndRepeat() {
        Object owner=new Object(); SystemClock.now=1000;
        KeyEvent e=new KeyEvent(0,0,0,87,0,3,7,8,32,0x101);
        KeyEvent out=WheelInputRecovery.normalize(owner,e);
        assertEquals(87,out.getKeyCode());assertEquals(3,out.getMetaState());
        assertEquals(7,out.getDeviceId());assertEquals(8,out.getScanCode());
        assertEquals(32,out.getFlags());assertEquals(0x101,out.getSource());
        assertEquals(0,out.getRepeatCount());
        SystemClock.now=1050; assertEquals(out.getDownTime(),WheelInputRecovery.normalize(owner,e).getDownTime());
        WheelInputRecovery.clear(owner);SystemClock.now=1500;
        assertNotEquals(out.getDownTime(),WheelInputRecovery.normalize(owner,e).getDownTime());
        WheelInputRecovery.clear(owner);
    }
    @Test public void adapterPassesNullAndUnsupportedActions() {
        Object owner=new Object();assertNull(WheelInputRecovery.normalize(owner,null));
        KeyEvent e=new KeyEvent(0,0,2,87,0,0,0,0,0,0);
        assertSame(e,WheelInputRecovery.normalize(owner,e));
    }
}
