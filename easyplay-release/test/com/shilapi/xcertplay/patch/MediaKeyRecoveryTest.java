package com.shilapi.xcertplay.patch;
import org.junit.*;import static org.junit.Assert.*;
import android.app.Activity;import android.content.ComponentName;
import android.media.*;import android.os.Handler;
public class MediaKeyRecoveryTest {
 private Object owner;private Activity a;private AudioManager audio;private RemoteControlClient remote;
 @Before public void before(){Handler.reset();owner=new Object();a=new Activity();audio=new AudioManager();remote=new RemoteControlClient();}
 @After public void after(){MediaKeyRecovery.paused(a);MediaKeyRecovery.stop(owner);Handler.reset();}
 private void visible(){MediaKeyRecovery.resumed(a);MediaKeyRecovery.focused(a,true);MediaKeyRecovery.carPlay(a,true);}
 private void start(){MediaKeyRecovery.start(owner,audio,new ComponentName(),remote);}
 @Test public void renewsExistingReceiverWhileContinuouslyInCarPlay(){visible();start();Handler.advance(30000);assertEquals(10,audio.receivers);assertEquals(10,audio.clients);assertSame(remote,audio.lastClient);}
 @Test public void noRegistrationWhileHomeOrOtherAppVisible(){visible();start();MediaKeyRecovery.paused(a);Handler.advance(30000);assertEquals(0,audio.receivers);}
 @Test public void noRegistrationInSettingsWithoutCarPlay(){MediaKeyRecovery.resumed(a);MediaKeyRecovery.focused(a,true);start();Handler.advance(9000);assertEquals(0,audio.receivers);}
 @Test public void focusLossDoesNotFightDialogOrIncomingCall(){visible();start();MediaKeyRecovery.focused(a,false);a.focus=false;Handler.advance(9000);assertEquals(0,audio.receivers);}
 @Test public void stoppedSessionCannotRegisterAfterClose(){visible();start();MediaKeyRecovery.stop(owner);Handler.advance(9000);assertEquals(0,audio.receivers);assertEquals(0,Handler.pending());}
 @Test public void replacementHasExactlyOneTimer(){visible();start();start();assertEquals(1,Handler.pending());Handler.advance(3000);assertEquals(1,audio.receivers);}
 @Test public void returnToCarPlayRefreshesImmediately(){visible();start();MediaKeyRecovery.paused(a);Handler.advance(3000);MediaKeyRecovery.resumed(a);Handler.advance(0);assertEquals(1,audio.receivers);}
 @Test public void realWindowFocusAndFinishingAreAlsoChecked(){visible();start();a.finishing=true;Handler.advance(3000);assertEquals(0,audio.receivers);a.finishing=false;a.focus=false;Handler.advance(3000);assertEquals(0,audio.receivers);}
 @Test public void rejectedRegistrationDoesNotCrashOrSpin(){visible();start();audio.fail=true;Handler.advance(9000);assertEquals(0,audio.receivers);assertEquals(1,Handler.pending());audio.fail=false;Handler.advance(3000);assertEquals(1,audio.receivers);}
 @Test public void hidingCarPlayStopsRenewal(){visible();start();MediaKeyRecovery.carPlay(a,false);Handler.advance(6000);assertEquals(0,audio.receivers);}
 @Test public void callModeSuppressesRenewalEvenIfWindowRemainsFocused(){visible();start();audio.mode=2;Handler.advance(3000);assertEquals(0,audio.receivers);audio.mode=3;Handler.advance(3000);assertEquals(0,audio.receivers);audio.mode=0;Handler.advance(3000);assertEquals(1,audio.receivers);}
}
