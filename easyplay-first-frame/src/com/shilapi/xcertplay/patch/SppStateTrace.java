package com.shilapi.xcertplay.patch;
import android.content.*;
import android.os.SystemClock;

/** Broadcasts are diagnostic evidence only: never override the Binder gate or permit VF. */
public final class SppStateTrace {
    private static boolean installed;
    private static final SppTraceState state = new SppTraceState();
    private SppStateTrace() {}
    public static synchronized void install(Context c) {
        if (installed) return;
        Context app = c.getApplicationContext(); if (app == null) app = c;
        IntentFilter filter = new IntentFilter("SPP_CONNECTION_CHANGED");
        app.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent == null) return;
                if ("SPP_CONNECTION_CHANGED".equals(intent.getAction()))
                    state.event(intent.getIntExtra("SPP_CONNECTION_STATE", -1), SystemClock.elapsedRealtime());
            }
        }, filter);
        installed = true;
    }
    public static void cache(boolean connected) { state.cache(connected, SystemClock.elapsedRealtime()); }
    public static void request() { state.request(SystemClock.elapsedRealtime()); }
    public static String summary() { return state.summary(SystemClock.elapsedRealtime()); }
}
