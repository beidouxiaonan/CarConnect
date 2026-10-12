package com.shilapi.xcertplay.patch;
import android.content.*;
import android.os.SystemClock;
/** Same legacy keys. Upgrade keeps pending evidence; a manual permit is local to one operation. */
public final class SppRecoveryPrefs {
    private static SharedPreferences prefs(Context c){
        Context a=c.getApplicationContext();return (a==null?c:a).getSharedPreferences("easyplay_enhancements",0);
    }
    public static boolean pending(Context c){return prefs(c).getBoolean("sppRecoveryPending",false);}
    public static boolean nativeEnabled(Context c){return prefs(c).getBoolean("sppNativeRecovery",false);}
    public static boolean begin(Context c){return begin(c,"connection-or-owned-standby");}
    static boolean begin(Context c,String reason){
        OemTrace.event("RECOVERY_JOURNAL previous "+evidence(c));
        int code=-1;try{code=c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionCode;}catch(Exception ignored){}
        boolean saved=prefs(c).edit().putBoolean("sppRecoveryPending",true)
            .putLong("sppRecoveryBeginMillis",System.currentTimeMillis()).putLong("sppRecoveryBeginElapsed",SystemClock.elapsedRealtime())
            .putInt("sppRecoveryBeginVersion",code).putString("sppRecoveryBeginReason",reason).commit();
        OemTrace.event("RECOVERY_JOURNAL begin saved="+saved+" reason="+reason+" versionCode="+code);return saved;
    }
    public static boolean released(Context c){
        boolean saved=prefs(c).edit().putBoolean("sppRecoveryPending",false).commit();
        OemTrace.event("RECOVERY_JOURNAL released saved="+saved);return saved;
    }
    public static boolean saveNative(Context c,boolean on){return prefs(c).edit().putBoolean("sppNativeRecovery",on).commit();}
    static long cooldownMillis(Context c){
        long wall=prefs(c).getLong("sppRecoveryBeginMillis",0),elapsed=prefs(c).getLong("sppRecoveryBeginElapsed",-1);
        // Across a process replacement on the same boot, keep the 45-second request spacing.
        // If the clocks no longer agree (reboot/clock change), the pending journal still requires an explicit permit.
        long age=SystemClock.elapsedRealtime()-elapsed,wallAge=System.currentTimeMillis()-wall;
        if(elapsed<0||wall==0||age<0||Math.abs(wallAge-age)>5000)return 0;
        return Math.max(0,45000-age);
    }
    static String evidence(Context c){
        SharedPreferences p=prefs(c);long at=p.getLong("sppRecoveryBeginMillis",0);
        return "recoveryPending="+pending(c)+"; origin="+(at==0?"旧版本标记，来源时间未记录":
            p.getString("sppRecoveryBeginReason","unknown")+"; wallMillis="+at+"; elapsedMillis="+p.getLong("sppRecoveryBeginElapsed",0)
            +"; versionCode="+p.getInt("sppRecoveryBeginVersion",-1));
    }
}
