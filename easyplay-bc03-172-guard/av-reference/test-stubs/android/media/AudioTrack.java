package android.media;
public class AudioTrack {
    public static final int STATE_INITIALIZED=1,PLAYSTATE_PLAYING=3,ERROR_INVALID_OPERATION=-3;
    public static int grantedFrames=24000;
    public static boolean denyHead,denyPause;
    public int playCalls,pauseCalls,flushCalls,head,queued,state=1,playState;
    public boolean released;
    protected final int frameBytes,capacity;
    public AudioTrack(int stream,int rate,int mask,int encoding,int bytes,int mode){frameBytes=(mask==4?1:2)*2;capacity=grantedFrames*frameBytes;}
    protected int getNativeFrameCount(){return grantedFrames;}
    public int getState(){return state;}
    public int getPlayState(){return playState;}
    public int getPlaybackHeadPosition(){if(denyHead)throw new IllegalStateException();return head;}
    public void play(){playCalls++;playState=3;}
    public void pause(){if(denyPause)throw new IllegalStateException();pauseCalls++;playState=2;}
    public void stop(){playState=1;queued=0;}
    public void release(){released=true;}
    public void flush(){flushCalls++;queued=0;}
    public int write(byte[] bytes,int offset,int length){
        if(playState!=3 && grantedFrames>0 && length>capacity-queued)throw new AssertionError("Would block before play: "+length);
        queued+=length;return length;
    }
    public void consume(int frames){int count=Math.min(frames,queued/frameBytes);head+=count;queued-=count*frameBytes;}
}
