package com.shilapi.xcertplay.patch;

import android.app.Activity;
import android.content.ComponentName;
import android.media.AudioManager;
import android.media.RemoteControlClient;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.WeakHashMap;

/** API17-20 media-button registration maintenance only while CarPlay owns the visible window. */
public final class MediaKeyRecovery {
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static final WeakHashMap<Activity, WindowState> windows = new WeakHashMap<Activity, WindowState>();
    private static final WeakHashMap<Object, Lease> leases = new WeakHashMap<Object, Lease>();
    private static final class WindowState { WindowState() {} boolean resumed, video, focus; }
    private static WindowState window(Activity a) {
        WindowState w = windows.get(a);
        if (w == null) { w = new WindowState(); windows.put(a, w); }
        return w;
    }
    public static void resumed(Activity a) { synchronized (windows) { window(a).resumed = true; } wake(); }
    public static void paused(Activity a) { synchronized (windows) { window(a).resumed = false; } }
    public static void focused(Activity a, boolean value) { synchronized (windows) { window(a).focus = value; } if (value) wake(); }
    public static void carPlay(Activity a, boolean value) { synchronized (windows) { window(a).video = value; } if (value) wake(); }
    static boolean foreground() {
        synchronized (windows) {
            for (Activity a : windows.keySet()) {
                WindowState w = windows.get(a);
                if (w.resumed && w.video && w.focus && !a.isFinishing() && a.hasWindowFocus()) return true;
            }
        }
        return false;
    }
    private static void wake() {
        ArrayList<Lease> copy;
        synchronized (leases) { copy = new ArrayList<Lease>(leases.values()); }
        for (Lease l : copy) l.schedule(0);
    }
    public static void start(Object owner, AudioManager audio, ComponentName receiver, RemoteControlClient remote) {
        stop(owner);
        Lease l = new Lease(owner, audio, receiver, remote);
        synchronized (leases) { leases.put(owner, l); }
        l.schedule(3000);
    }
    public static void stop(Object owner) {
        Lease l;
        synchronized (leases) { l = leases.remove(owner); }
        if (l != null) l.close();
    }
    private static final class Lease implements Runnable {
        final WeakReference<Object> owner;
        final AudioManager audio;
        final ComponentName receiver;
        final RemoteControlClient remote;
        boolean closed, reported;
        Lease(Object owner, AudioManager audio, ComponentName receiver, RemoteControlClient remote) {
            this.owner = new WeakReference<Object>(owner); this.audio = audio;
            this.receiver = receiver; this.remote = remote;
        }
        synchronized void schedule(long delay) {
            if (closed) return;
            main.removeCallbacks(this); main.postDelayed(this, delay);
        }
        synchronized void close() { closed = true; main.removeCallbacks(this); }
        public synchronized void run() {
            if (closed) return;
            if (owner.get() == null) { close(); return; }
            if (foreground()) {
                try {
                    if (audio.getMode() == AudioManager.MODE_NORMAL) {
                        audio.registerMediaButtonEventReceiver(receiver);
                        audio.registerRemoteControlClient(remote);
                    }
                } catch (RuntimeException failure) {
                    if (!reported) Log.i("CarConnect-Wheel", "Media receiver renewal failed: " + failure.getClass().getSimpleName());
                    reported = true;
                }
            }
            schedule(3000);
        }
    }
    private MediaKeyRecovery() {}
}
