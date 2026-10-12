package com.shilapi.xcertplay.patch;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** Own-window immersive recovery only. No guessed OEM broadcasts or changes to global SystemUI. */
public final class FullscreenRecovery {
    private static final WeakHashMap<Activity,Controller> windows=new WeakHashMap<Activity,Controller>();
    private static Controller get(Activity a) {
        Controller c=windows.get(a);
        if(c==null) { c=new Controller(a); windows.put(a,c); }
        return c;
    }
    public static void resumed(Activity a) { Controller c=get(a); c.resumed=true; c.request(); }
    public static void paused(Activity a) { Controller c=get(a); c.resumed=false; c.cancel(); }
    public static void focused(Activity a, boolean focus) {
        Controller c=get(a); if(focus)c.request(); else c.cancel();
    }
    public static void carPlay(Activity a, boolean visible) {
        Controller c=get(a);
        if(visible && !c.video)c.request();
        c.video=visible;
    }
    public static String summary(Activity a) {
        Controller c=get(a);
        int actual=a.getWindow().getDecorView().getSystemUiVisibility();
        return "窗口全屏：请求 0x"+Integer.toHexString(c.flags())+"，当前 0x"+Integer.toHexString(actual)+
            "；焦点="+a.hasWindowFocus()+"，CarPlay="+c.video+
            "。若标志已生效而快捷栏仍在，需要识别厂商悬浮栏接口。";
    }
    private static final class Controller implements Runnable,View.OnSystemUiVisibilityChangeListener {
        private final WeakReference<Activity> activity;
        private final Handler handler=new Handler(); // All entry points run on the Activity/UI thread.
        private boolean resumed,video,applying;
        private int retries;
        Controller(Activity a) {
            activity=new WeakReference<Activity>(a);
            a.getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);
        }
        void cancel() { handler.removeCallbacks(this); }
        void request() { cancel(); retries=0; apply(); handler.postDelayed(this,150); }
        public void run() {
            if(!active())return;
            apply();
            if(++retries<3)handler.postDelayed(this,retries==1 ? 450 : 1000);
        }
        boolean active() {
            Activity a=activity.get(); return resumed && a!=null && !a.isFinishing() && a.hasWindowFocus();
        }
        int flags() {
            // Preserve the baseline's known E03 navigation exception.
            if(Build.VERSION.SDK_INT>=29 && Build.DEVICE!=null && Build.DEVICE.toLowerCase(java.util.Locale.US).startsWith("e03"))return 4;
            return Build.VERSION.SDK_INT>=19 ? 0x1006 : 6;
        }
        void apply() {
            if(!active())return;
            try {
                Activity a=activity.get();
                a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                View decor=a.getWindow().getDecorView();
                int target=flags();
                applying=true;
                if(decor.getSystemUiVisibility()!=target)decor.setSystemUiVisibility(target);
            } catch(RuntimeException error) { Log.i("CarConnect-Fullscreen","Own-window request failed: "+error.getClass().getSimpleName()); }
            finally { applying=false; }
        }
        public void onSystemUiVisibilityChange(int visibility) {
            // Sticky immersive normally handles edge gestures itself; retries are bounded per lifecycle transition.
            if(!applying && active() && (visibility&flags())!=flags() && retries<3) {
                cancel(); handler.postDelayed(this,1200);
            }
        }
    }
}
