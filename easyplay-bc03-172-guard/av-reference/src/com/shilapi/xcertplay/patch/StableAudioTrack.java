package com.shilapi.xcertplay.patch;

import android.media.AudioTrack;
import android.os.SystemClock;
import android.util.Log;
import kotlin.jvm.functions.Function1;

/** KitKat streaming track with media prefill; navigation/Siri/calls still start immediately. */
public final class StableAudioTrack extends AudioTrack {
    private final int rate, frameBytes, actualCapacity;
    private final Object playbackLock=new Object();
    private volatile boolean closed;
    private PcmBufferState buffer;
    private Function1 report;
    private String channel="unknown";
    private long nextHealth, lastWrite=-1, maxWriteGap;
    private boolean bufferingFailed, idleWarning;

    public StableAudioTrack(int stream, int rate, int mask, int encoding, int bytes, int mode) {
        super(stream,rate,mask,encoding,bytes,mode);
        this.rate=rate;
        frameBytes=(mask==4 ? 1 : 2)*2; // Original renderer is exclusively mono/stereo PCM16.
        int capacity=0;
        try { long n=(long)getNativeFrameCount()*frameBytes; if(n>0 && n<=Integer.MAX_VALUE)capacity=(int)n; }
        catch(RuntimeException ignored) { }
        // Never prefill a stopped track using an unverified requested capacity: a write could block.
        actualCapacity=capacity;
    }
    void configure(String channel, int millis, Function1 report) {
        this.channel=channel; this.report=report;
        int safeMillis=millis==500||millis==1000 ? millis : 300;
        long now=SystemClock.elapsedRealtime();
        buffer=new PcmBufferState(rate,frameBytes,actualCapacity,safeMillis,"MEDIA".equals(channel),now);
        nextHealth=now+5000;
        note("AudioBuffer channel="+channel+" capacity="+actualCapacity+" start="+buffer.startBytes+
            " mode="+(buffer.prefilling ? "prefill" : "immediate"));
    }
    boolean ready() { return !closed && getState()==STATE_INITIALIZED &&
        (getPlayState()==PLAYSTATE_PLAYING || buffer!=null && buffer.prefilling); }
    @Override public void play() {
        if(buffer!=null && buffer.prefilling && !bufferingFailed)return;
        startPlayback("immediate");
    }
    private void startPlayback(String reason) {
        synchronized(playbackLock) {
            if(closed)return;
            super.play();
            if(buffer!=null)buffer.started();
        }
        if(!"immediate".equals(reason))note("AudioBuffer start reason="+reason);
    }
    @Override public int write(byte[] bytes, int offset, int length) {
        if(closed)return ERROR_INVALID_OPERATION;
        int limit=buffer==null ? length : buffer.writeLimit(length);
        int count=super.write(bytes,offset,limit);
        if(count>0 && !closed && buffer!=null) {
            long now=SystemClock.elapsedRealtime();
            if(lastWrite>=0)maxWriteGap=Math.max(maxWriteGap,now-lastWrite);
            lastWrite=now;
            if(buffer.written(count,now))startPlayback("filled");
            health(now);
        }
        return count; // Original renderer retains its partial-write loop and exact written byte counter.
    }
    void idle() {
        if(closed || buffer==null)return;
        long now=SystemClock.elapsedRealtime();
        try {
            int head=buffer.needsHead(now) && !bufferingFailed ? getPlaybackHeadPosition() : 0;
            int action=bufferingFailed ? PcmBufferState.NONE : buffer.idle(head,now);
            if(action==PcmBufferState.START)startPlayback("short-tail");
            else if(action==PcmBufferState.REBUFFER) {
                synchronized(playbackLock) { if(closed)return; super.pause(); }
                note("AudioBuffer empty; rebuffer="+buffer.rebuffers);
            }
            health(now);
        } catch(RuntimeException error) {
            bufferingFailed=true; buffer.abandonPrefill();
            if(!idleWarning) { idleWarning=true; note("AudioBuffer fallback: "+error.getClass().getSimpleName()); }
            startPlayback("fallback");
        }
    }
    private void health(long now) {
        if(now<nextHealth || closed)return;
        nextHealth=now+5000;
        long queued=0;
        try { queued=buffer.queuedBytes(getPlaybackHeadPosition()); } catch(RuntimeException ignored) { }
        note("AudioHealth channel="+channel+" queuedMs="+(queued*1000/((long)rate*frameBytes))+
            " maxWriteGapMs="+maxWriteGap+" rebuffers="+buffer.rebuffers+" prefill="+buffer.prefilling);
        maxWriteGap=0;
    }
    @Override public void stop() {
        synchronized(playbackLock) { closed=true; super.stop(); }
    }
    @Override public void release() {
        synchronized(playbackLock) { closed=true; super.release(); }
    }
    private void note(String message) {
        AudioRuntime.record(channel,message);
        Log.i("CarConnect-Audio",message);
        if(report!=null)try { report.invoke(message); } catch(RuntimeException ignored) { }
    }
}
