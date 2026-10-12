package com.shilapi.xcertplay.patch;
import android.content.*;
import android.os.SystemClock;

/** Broadcasts are diagnostic evidence only: never override the Binder gate or permit VF. */
public final class SppStateTrace {
    private static boolean installed;
    private static final SppTraceState state = new SppTraceState();
    private static long epoch;private static Boolean cached;
    private SppStateTrace() {}
    public static synchronized void install(Context c) {
        OemTrace.install(c);
        if (installed) return;
        Context app = c.getApplicationContext(); if (app == null) app = c;
        IntentFilter filter = new IntentFilter("SPP_CONNECTION_CHANGED");
        app.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent == null) return;
                OemTrace.broadcast(intent);
                if ("SPP_CONNECTION_CHANGED".equals(intent.getAction())){
                    int value=intent.getIntExtra("SPP_CONNECTION_STATE",-1);
                    if(value==0||value==1)synchronized(SppStateTrace.class){epoch++;}
                    state.event(intent.getIntExtra("SPP_CONNECTION_STATE", -1), SystemClock.elapsedRealtime());
                }
            }
        }, filter);
        installed = true;
    }
    public static synchronized void cache(boolean connected) {
        if(cached!=null&&cached.booleanValue()!=connected)epoch++;
        cached=Boolean.valueOf(connected);state.cache(connected, SystemClock.elapsedRealtime());
    }
    static synchronized long epoch(){return epoch;}
    public static void request() { state.request(SystemClock.elapsedRealtime()); }
    public static String summary() { return state.summary(SystemClock.elapsedRealtime()); }
}
