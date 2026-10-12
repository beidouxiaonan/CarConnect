package com.shilapi.xcertplay.patch;
import android.app.Activity;
import android.os.SystemClock;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import com.shilapi.xcertplay.legacy.LegacySessionService;
import com.shilapi.xcertplay.orchestration.*;
import org.junit.*;
import static org.junit.Assert.*;
public class FirstFrameRecoveryTest {
    private Activity screen;
    @Before public void foreground()throws Exception{screen=new Activity();setScreen(screen);SystemClock.now=0;EnhancementPrefs.oem=true;}
    private void setScreen(Activity a)throws Exception{Field f=FirstFrameRecovery.class.getDeclaredField("foreground");f.setAccessible(true);f.set(null,new WeakReference<Activity>(a));}
    private LegacySessionService ready(){LegacySessionService s=new LegacySessionService();FirstFrameRecovery.status(s,1,CarPlayStatus.RunningWireless.INSTANCE);s.handler.tick(0);return s;}
    @Test public void noFrameClosesOnceThroughExistingService() {
        LegacySessionService s=ready();s.handler.tick(60000);assertEquals(1,s.stops);assertTrue(s.automatic);assertTrue(s.closing);assertEquals(0,s.handler.count());
    }
    @Test public void renderedFrameCancelsBeforeTimeout() {
        LegacySessionService s=ready();s.frame=true;s.handler.tick(60000);assertEquals(0,s.stops);assertEquals(0,s.handler.count());
    }
    @Test public void sameGenerationDifferentControllerIsProtected() {
        LegacySessionService s=ready();s.controller=new CarPlayController();s.handler.tick(60000);assertEquals(0,s.stops);assertEquals(0,s.handler.count());
    }
    @Test public void staleGenerationStatusCannotArmTimer() {
        LegacySessionService s=new LegacySessionService();FirstFrameRecovery.status(s,0,CarPlayStatus.RunningWireless.INSTANCE);assertEquals(0,s.handler.count());
    }
    @Test public void manualStopCancelsPostedRecovery() {
        LegacySessionService s=ready();FirstFrameRecovery.stopped(s,false);s.handler.tick(60000);assertEquals(0,s.stops);assertEquals(0,s.handler.count());
    }
    @Test public void destroyedServiceCannotRecover() {
        LegacySessionService s=ready();s.destroyed=true;FirstFrameRecovery.destroyed(s);s.handler.tick(60000);assertEquals(0,s.stops);
    }
    @Test public void disableSwitchCancelsExistingTimer() {
        LegacySessionService s=ready();FirstFrameRecovery.save(s,false);s.handler.tick(60000);assertEquals(0,s.stops);assertEquals(0,s.handler.count());
    }
    @Test public void backgroundAndSurfaceLossRestartForegroundGrace()throws Exception {
        LegacySessionService s=ready();setScreen(null);s.handler.tick(59000);setScreen(screen);s.handler.tick(90000);
        s.surface.valid=false;s.handler.tick(140000);s.surface.valid=true;s.handler.tick(200000);
        assertEquals(0,s.stops);s.handler.tick(260000);assertEquals(1,s.stops);
    }
    @Test public void reentrantUiCancelCannotCloseNewController() {
        final LegacySessionService s=ready();s.listener=new Runnable(){public void run(){s.controller=new CarPlayController();s.generation++;}};
        s.handler.tick(60000);assertEquals(0,s.stops);
    }
    @Test public void wiredAndStandardBluetoothNeverArm() {
        LegacySessionService s=new LegacySessionService();s.wired=true;FirstFrameRecovery.status(s,1,CarPlayStatus.RunningWireless.INSTANCE);assertEquals(0,s.handler.count());
        s.wired=false;EnhancementPrefs.oem=false;FirstFrameRecovery.status(s,1,CarPlayStatus.RunningWireless.INSTANCE);assertEquals(0,s.handler.count());
    }
    @Test public void budgetSurvivesServiceGenerationChanges() {
        LegacySessionService s=ready();s.handler.tick(60000);assertEquals(1,s.stops);
        for(int attempt=2;attempt<=3;attempt++){
            s.closing=false;s.controller=new CarPlayController();SystemClock.now=attempt*100000;
            FirstFrameRecovery.status(s,s.generation,CarPlayStatus.RunningWireless.INSTANCE);s.handler.tick(SystemClock.now);s.handler.tick(SystemClock.now+60000);
        }
        assertEquals(2,s.stops);assertFalse(s.closing);assertNotNull(s.controller);
        assertTrue(s.logs.get(s.logs.size()-1).startsWith("已自动重连 2 次"));
    }
}
