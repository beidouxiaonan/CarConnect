package com.shilapi.xcertplay.patch;
import android.content.Context;
import android.content.SharedPreferences;
/** Journal before issuing a disconnect. Kept across process death; never treats a write as release. */
public final class SppRecoveryPrefs {
    private static SharedPreferences prefs(Context c) {
        Context app=c.getApplicationContext();
        return (app==null?c:app).getSharedPreferences("easyplay_enhancements",Context.MODE_PRIVATE);
    }
    public static boolean pending(Context c){return prefs(c).getBoolean("sppRecoveryPending",false);}
    public static boolean nativeEnabled(Context c){return prefs(c).getBoolean("sppNativeRecovery",false);}
    public static boolean begin(Context c){return prefs(c).edit().putBoolean("sppRecoveryPending",true).commit();}
    public static boolean released(Context c){return prefs(c).edit().putBoolean("sppRecoveryPending",false).commit();}
    public static boolean saveNative(Context c,boolean enabled){return prefs(c).edit().putBoolean("sppNativeRecovery",enabled).commit();}
}
