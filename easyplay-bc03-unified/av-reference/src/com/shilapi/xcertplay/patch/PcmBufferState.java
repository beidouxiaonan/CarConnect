package com.shilapi.xcertplay.patch;

/** Worker-owned PCM accounting. Does not flush queued sound on a temporary transport gap. */
public final class PcmBufferState {
    static final int NONE=0, START=1, REBUFFER=2;
    final int frameBytes, capacityBytes, chunkBytes, startBytes;
    final boolean media;
    boolean prefilling;
    private long written, playedFrames, lastHead, pendingBytes;
    private long firstPending=-1, lastWrite=-1, lastRebuffer;
    int rebuffers;

    public PcmBufferState(int rate, int frameBytes, int capacity, int millis, boolean media, long now) {
        this.frameBytes=frameBytes;
        capacityBytes=capacity>0 ? capacity-capacity%frameBytes : 0;
        chunkBytes=capacityBytes>=frameBytes*2 ? Math.max(frameBytes,
            Math.min(Math.max(frameBytes, rate/100*frameBytes), capacityBytes/2)/frameBytes*frameBytes) : frameBytes;
        this.media=media && capacityBytes>=frameBytes*2;
        long desired=(long)rate*frameBytes*millis/1000;
        startBytes=this.media ? (int)Math.max(frameBytes,
            Math.min(desired, capacityBytes-chunkBytes)/frameBytes*frameBytes) : 0;
        prefilling=this.media;
        lastRebuffer=now;
    }
    int writeLimit(int bytes) { return prefilling ? Math.min(bytes, chunkBytes) : bytes; }
    boolean written(int bytes, long now) {
        if(bytes<=0)return false;
        written+=bytes; lastWrite=now;
        if(!prefilling)return false;
        if(firstPending<0)firstPending=now;
        pendingBytes+=bytes;
        return pendingBytes>=startBytes;
    }
    long queuedBytes(int rawHead) {
        long head=rawHead&0xffffffffL;
        playedFrames+=(head-lastHead)&0xffffffffL;
        lastHead=head;
        return Math.max(0, written-playedFrames*frameBytes);
    }
    boolean needsHead(long now) { return media && !prefilling && lastWrite>=0 && now-lastWrite>=60; }
    int idle(int rawHead, long now) {
        if(!media)return NONE;
        if(prefilling) {
            // A short media clip must also be audible, even when it never fills the target buffer.
            return pendingBytes>0 && firstPending>=0 && now-firstPending>=400 && now-lastWrite>=60 ? START : NONE;
        }
        if(needsHead(now) && now-lastRebuffer>=1000 && queuedBytes(rawHead)==0) {
            prefilling=true; pendingBytes=0; firstPending=-1; lastRebuffer=now; rebuffers++;
            return REBUFFER;
        }
        return NONE;
    }
    void started() { prefilling=false; pendingBytes=0; firstPending=-1; }
    void abandonPrefill() { started(); }
}
