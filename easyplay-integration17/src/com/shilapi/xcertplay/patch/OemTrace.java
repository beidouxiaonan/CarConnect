package com.shilapi.xcertplay.patch;
import android.content.*;
import android.os.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
/** Application evidence only: does not consume the OEM UART response stream. */
public final class OemTrace {
    private static OemTraceJournal journal; private static Boolean lastState;
    private static final AtomicLong attempts=new AtomicLong();
    public static synchronized void install(Context c){
        if(journal!=null)return;
        journal=new OemTraceJournal(new File(c.getFilesDir(),"oem-trace"));
        event("PROCESS_BEGIN CarConnect 0.1.16 test; SDK="+Build.VERSION.SDK_INT+" ABI="+Build.CPU_ABI+" model="+Build.MODEL);
    }
    public static long attempt(Context c){install(c);long n=attempts.incrementAndGet();event("ATTEMPT #"+n+" BEGIN; recoveryPending="+SppRecoveryPrefs.pending(c));return n;}
    public static synchronized void event(String message){
        if(journal!=null)journal.event(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",Locale.US).format(new java.util.Date())
            +" uptime="+SystemClock.elapsedRealtime()+" thread="+Thread.currentThread().getName()+" "+message);
    }
    public static synchronized void state(boolean value,long duration){
        if(lastState==null||lastState.booleanValue()!=value){event("BINDER isSppConnect="+value+" elapsedMs="+duration);lastState=Boolean.valueOf(value);}
        else if(duration>=250)event("BINDER isSppConnect slow elapsedMs="+duration+" value="+value);
    }
    public static void broadcast(Intent i){if(i!=null&&"SPP_CONNECTION_CHANGED".equals(i.getAction()))event("BROADCAST SPP state="+i.getIntExtra("SPP_CONNECTION_STATE",-1)+" (observation only)");}
    public static void error(String stage,Exception error){
        event(stage+" ERROR "+error.toString());
        StackTraceElement[] frames=error.getStackTrace();for(int n=0;n<Math.min(8,frames.length);n++)event("  at "+frames[n]);
        if(error.getCause()!=null)event("  caused by "+error.getCause().toString());
    }
    public static String summary(Context c)throws IOException {
        install(c);
        final OemTraceJournal current; synchronized(OemTrace.class){current=journal;}
        return "CarConnect 0.1.16 BC03 诊断测试\nSDK="+Build.VERSION.SDK_INT+" ABI="+Build.CPU_ABI+" model="+Build.MODEL
            +"\nSPP recoveryPending="+SppRecoveryPrefs.pending(c)+" nativeRecovery="+SppRecoveryPrefs.nativeEnabled(c)
            +" standbyRecovery="+StandbyRecovery.enabled(c)
            +"\n"+SppRecoveryPrefs.evidence(c)
            +"\n本报告仅记录本应用调用、原车 Binder 查询和广播；不包含 gocsdk 内部返回码。未记录握手载荷、证书、密钥、热点密码和完整 MAC。\n"
            +OemTraceJournal.clean(OemTransport.summary())+"\n"+current.report();
    }
}
