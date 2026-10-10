package com.shilapi.xcertplay.patch;
import android.app.Activity;
import android.os.*;
import org.junit.*;
import static org.junit.Assert.*;

public class FullscreenTest {
    @Before public void reset(){Handler.reset();Build.VERSION.SDK_INT=19;Build.DEVICE="test";}
    @Test public void manualLauncherHandoffRestoresBarsAfterDelayedFocus(){
        Activity a=new Activity();FullscreenRecovery.resumed(a);assertEquals(0,a.window.decor.getSystemUiVisibility());
        a.focus=true;FullscreenRecovery.focused(a,true);assertEquals(0x1006,a.window.decor.getSystemUiVisibility());
        assertEquals(0x400,a.window.flags&0x400);
        a.window.decor.systemRestoresBars();Handler.advance(1300);assertEquals(0x1006,a.window.decor.getSystemUiVisibility());
    }
    @Test public void enteringCarPlayMakesAnotherBoundedRequest(){
        Activity a=new Activity();a.focus=true;FullscreenRecovery.resumed(a);Handler.advance(2500);
        a.window.decor.systemRestoresBars();assertEquals(0,a.window.decor.getSystemUiVisibility());
        FullscreenRecovery.carPlay(a,true);assertEquals(0x1006,a.window.decor.getSystemUiVisibility());
    }
    @Test public void pauseCancelsRetriesAndDoesNotFightOtherApps(){
        Activity a=new Activity();a.focus=true;FullscreenRecovery.resumed(a);FullscreenRecovery.paused(a);
        a.window.decor.systemRestoresBars();Handler.advance(3000);assertEquals(0,a.window.decor.getSystemUiVisibility());assertEquals(0,Handler.pending());
    }
    @Test public void vendorIgnoringRequestHasBoundedWork(){
        Activity a=new Activity();a.focus=true;a.window.decor.reject=true;FullscreenRecovery.resumed(a);Handler.advance(6000);
        assertTrue(a.window.decor.setCalls<=4);assertEquals(0,Handler.pending());
    }
    @Test public void preKitKatDoesNotRequestUnsupportedStickyFlag(){
        Build.VERSION.SDK_INT=17;Activity a=new Activity();a.focus=true;FullscreenRecovery.resumed(a);assertEquals(6,a.window.decor.getSystemUiVisibility());
    }
    @Test public void knownE03NavigationExceptionIsPreserved(){
        Build.VERSION.SDK_INT=29;Build.DEVICE="E03vendor";Activity a=new Activity();a.focus=true;FullscreenRecovery.resumed(a);assertEquals(4,a.window.decor.getSystemUiVisibility());
    }
}
