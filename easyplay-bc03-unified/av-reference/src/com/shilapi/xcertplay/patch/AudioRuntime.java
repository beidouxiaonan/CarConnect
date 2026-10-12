package com.shilapi.xcertplay.patch;
import android.media.AudioTrack;
import android.os.Process;
import android.util.Log;
import kotlin.jvm.functions.Function1;

public final class AudioRuntime {
    private static final java.util.LinkedHashMap<String,String> health=new java.util.LinkedHashMap<String,String>();
    static synchronized void record(String channel,String message) { health.put(channel,message); }
    public static synchronized String summary() {
        if(health.isEmpty())return "尚无音频记录；连接后播放音乐并快速切换页面，再打开此诊断。";
        StringBuilder result=new StringBuilder();
        for(String message:health.values())result.append(message).append('\n');
        return result+"queuedMs 为尚未播放的 PCM；maxWriteGapMs 为最近写入间隔；rebuffers 为补缓冲次数。记录可能来自刚结束的会话。";
    }
    public static void workerPriority() { priority(-8); }
    public static void receivePriority() { priority(-4); }
    private static void priority(int requested) {
        try {
            Process.setThreadPriority(requested);
            Log.i("CarConnect-Audio","AudioPriority thread="+Thread.currentThread().getName()+
                " actual="+Process.getThreadPriority(Process.myTid()));
        } catch(RuntimeException error) {
            Log.i("CarConnect-Audio","AudioPriority unchanged: "+error.getClass().getSimpleName());
        }
    }
    public static void prepare(AudioTrack track, String channel, int millis, Function1 report) {
        if(track instanceof StableAudioTrack)((StableAudioTrack)track).configure(channel,millis,report);
        track.play();
    }
    public static int readyPlayState(AudioTrack track) {
        return track instanceof StableAudioTrack && ((StableAudioTrack)track).ready() ?
            AudioTrack.PLAYSTATE_PLAYING : track.getPlayState();
    }
    public static void idle(AudioTrack track) {
        if(track instanceof StableAudioTrack)((StableAudioTrack)track).idle();
    }
}
