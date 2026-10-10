package com.shilapi.xcertplay.patch;
import android.media.AudioTrack;
import android.os.SystemClock;
import org.junit.*;
import static org.junit.Assert.*;

public class AudioBufferTest {
    @Before public void reset(){SystemClock.now=0;AudioTrack.grantedFrames=24000;AudioTrack.denyHead=false;AudioTrack.denyPause=false;}
    private StableAudioTrack track(String channel,int millis){
        StableAudioTrack t=new StableAudioTrack(3,48000,12,2,96000,1);
        AudioRuntime.prepare(t,channel,millis,null);return t;
    }
    private void write(StableAudioTrack t,int bytes){byte[] b=new byte[bytes];int off=0;while(off<bytes){int n=t.write(b,off,bytes-off);assertTrue(n>0);off+=n;}}
    @Test public void mediaReallyPrefillsBeforePlay(){
        StableAudioTrack t=track("MEDIA",300);assertEquals(0,t.playCalls);assertEquals(3,AudioRuntime.readyPlayState(t));
        write(t,48000*4*299/1000);assertEquals(0,t.playCalls);
        write(t,192);assertEquals(1,t.playCalls);assertEquals(57600,t.queued);
    }
    @Test public void smallerGrantedCapacityCannotBlockAnOversizedFirstPacket(){
        AudioTrack.grantedFrames=512;StableAudioTrack t=track("MEDIA",1000);
        write(t,16384);assertEquals(1,t.playCalls);assertEquals(16384,t.queued);
    }
    @Test public void unknownCapacityFallsBackToImmediatePlayback(){
        AudioTrack.grantedFrames=0;StableAudioTrack t=track("MEDIA",300);assertEquals(1,t.playCalls);write(t,4096);
    }
    @Test public void promptChannelsKeepImmediatePlayback(){
        for(String channel:new String[]{"PHONE","SPEECH","ALERT","NAVIGATION"}){
            StableAudioTrack t=track(channel,1000);assertEquals(1,t.playCalls);write(t,192);SystemClock.now=2000;t.idle();assertEquals(0,t.pauseCalls);
        }
    }
    @Test public void shortMediaClipGetsPlayedEvenBelowStartThreshold(){
        StableAudioTrack t=track("MEDIA",300);write(t,19200);SystemClock.now=399;t.idle();assertEquals(0,t.playCalls);
        SystemClock.now=400;t.idle();assertEquals(1,t.playCalls);
    }
    @Test public void drainingOncePausesAndRefillsWithoutFlushing(){
        StableAudioTrack t=track("MEDIA",300);write(t,57600);t.consume(14400);
        SystemClock.now=2000;t.idle();assertEquals(1,t.pauseCalls);assertEquals(0,t.flushCalls);
        write(t,57600);assertEquals(2,t.playCalls);assertEquals(0,t.flushCalls);
    }
    @Test public void pendingSoundIsPreservedWhileTransportIsQuiet(){
        StableAudioTrack t=track("MEDIA",300);write(t,57600);t.consume(1000);
        SystemClock.now=2000;t.idle();assertEquals(0,t.pauseCalls);assertEquals(53600,t.queued);
    }
    @Test public void stoppedTrackNeverRestartsFromShortTailCallback(){
        StableAudioTrack t=track("MEDIA",300);write(t,19200);t.stop();SystemClock.now=2000;t.idle();assertEquals(0,t.playCalls);
        assertEquals(-3,t.write(new byte[192],0,192));
    }
    @Test public void vendorPauseFailureFallsBackToPlaying(){
        StableAudioTrack t=track("MEDIA",300);write(t,57600);t.consume(14400);AudioTrack.denyPause=true;
        SystemClock.now=2000;t.idle();assertEquals(3,t.getPlayState());write(t,19200);SystemClock.now=5000;t.idle();assertEquals(3,t.getPlayState());
    }
    @Test public void unsignedHeadHandlesSignBitAndWrap(){
        PcmBufferState b=new PcmBufferState(48000,4,96000,300,true,0);
        b.written(Integer.MAX_VALUE,0);b.written(Integer.MAX_VALUE,0);b.written(Integer.MAX_VALUE,0);b.written(Integer.MAX_VALUE,0);b.written(Integer.MAX_VALUE,0);
        long q=b.queuedBytes(0x7ffffffe);assertEquals(2147483651L,q);
        assertEquals(q-16,b.queuedBytes(0x80000002));
        b.queuedBytes(0xfffffffe);b.written(Integer.MAX_VALUE,0);b.written(Integer.MAX_VALUE,0);b.written(Integer.MAX_VALUE,0);b.written(Integer.MAX_VALUE,0);
        long before=b.queuedBytes(0xfffffffe);assertEquals(before-16,b.queuedBytes(2));
    }
    @Test public void deniedPriorityDoesNotStopAudio(){
        android.os.Process.deny=true;try {AudioRuntime.workerPriority();AudioRuntime.receivePriority();}finally {android.os.Process.deny=false;}
    }
}
