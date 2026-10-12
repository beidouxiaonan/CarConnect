package com.shilapi.xcertplay.patch;
import android.content.Context;
import android.content.SharedPreferences;
public final class SppCleanupPrefs {
    private static SharedPreferences prefs(Context c) {
        Context app=c.getApplicationContext();
        return (app==null?c:app).getSharedPreferences("easyplay_enhancements",Context.MODE_PRIVATE);
    }
    public static boolean enabled(Context c) { return prefs(c).getBoolean("oemSppCleanup",false); }
    public static void save(Context c,boolean enabled) { prefs(c).edit().putBoolean("oemSppCleanup",enabled).apply(); }
}
