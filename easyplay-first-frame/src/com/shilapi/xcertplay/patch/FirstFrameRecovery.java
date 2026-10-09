package com.shilapi.xcertplay.patch;

import android.content.Context;
import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Surface;
import com.shilapi.xcertplay.legacy.LegacySessionService;
import com.shilapi.xcertplay.orchestration.CarPlayController;
import com.shilapi.xcertplay.orchestration.CarPlayStatus;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** All callbacks run on the existing Service main Handler; close uses its existing worker. */
public final class FirstFrameRecovery {
    private static final String PREF = "firstFrameAutoRetry";
    private static final WeakHashMap<LegacySessionService, Entry> entries = new WeakHashMap<LegacySessionService, Entry>();
    private static boolean installed;
    private static WeakReference<Activity> foreground = new WeakReference<Activity>(null);
    private FirstFrameRecovery() {}
    public static void install(Activity activity) {
        if (installed) return;
        installed = true;
        SppStateTrace.install(activity);
        activity.getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            public void onActivityResumed(Activity a) {
                if (a.getClass().getName().equals("com.shilapi.xcertplay.legacy.LegacyActivity")) foreground = new WeakReference<Activity>(a);
            }
            public void onActivityPaused(Activity a) { if (foreground.get() == a) foreground.clear(); }
            public void onActivityDestroyed(Activity a) { if (foreground.get() == a) foreground.clear(); }
            public void onActivityCreated(Activity a, Bundle b) {}
            public void onActivityStarted(Activity a) {}
            public void onActivityStopped(Activity a) {}
            public void onActivitySaveInstanceState(Activity a, Bundle b) {}
        });
    }
    public static boolean enabled(Context c) {
        return c.getSharedPreferences("easyplay_enhancements", Context.MODE_PRIVATE).getBoolean(PREF, true);
    }
    public static void save(Context c, boolean value) {
        c.getSharedPreferences("easyplay_enhancements", Context.MODE_PRIVATE).edit().putBoolean(PREF, value).apply();
    }
    private static boolean eligible(LegacySessionService s) {
        return !LegacySessionService.access$getDestroyed$p(s) && !s.getClosing()
            && s.getWaitingAutomatically() && !s.getWired$legacy()
            && enabled(s) && EnhancementPrefs.getOemBluetooth(s);
    }
    public static void status(LegacySessionService s, int token, CarPlayStatus state) {
        if (token != LegacySessionService.access$getGeneration$p(s) || !eligible(s)) return;
        if (state != CarPlayStatus.RunningWireless.INSTANCE && state != CarPlayStatus.WirelessActive.INSTANCE) return;
        CarPlayController owner = s.getController();
        if (owner == null) return;
        Entry e = entries.get(s);
        if (e == null) { e = new Entry(s); entries.put(s, e); }
        if (e.policy.arm(token)) {
            e.handler.removeCallbacks(e);
            e.token = token;
            e.owner = new WeakReference<CarPlayController>(owner);
            LegacySessionService.access$record(s, "R1：无线控制已启动；前台连续 60 秒无首帧时恢复，最多自动重连 2 次");
            e.handler.post(e);
        }
    }
    public static void stopped(LegacySessionService s, boolean retainAutomatic) {
        Entry e = entries.get(s);
        if (e != null) { e.handler.removeCallbacks(e); e.policy.cancel(!retainAutomatic); }
    }
    public static void destroyed(LegacySessionService s) {
        Entry e = entries.remove(s);
        if (e != null) { e.handler.removeCallbacks(e); e.policy.cancel(true); }
    }
    private static final class Entry implements Runnable {
        final WeakReference<LegacySessionService> service;
        final Handler handler;
        final FirstFramePolicy policy = new FirstFramePolicy();
        WeakReference<CarPlayController> owner;
        int token;
        Entry(LegacySessionService s) {
            service = new WeakReference<LegacySessionService>(s);
            handler = LegacySessionService.access$getMain$p(s);
        }
        public void run() {
            LegacySessionService s = service.get();
            if (s == null) return;
            // Every tick rechecks ownership before looking at the current render flag.
            if (token != LegacySessionService.access$getGeneration$p(s) || owner.get() != s.getController()) {
                policy.cancel(false); return;
            }
            Surface surface = LegacySessionService.access$getSurface$p(s);
            FirstFramePolicy.Action action = policy.check(token, SystemClock.elapsedRealtime(), eligible(s),
                LegacySessionService.access$getVideoFrameSeen$p(s), foreground.get() != null && surface != null && surface.isValid());
            if (action == FirstFramePolicy.Action.WAIT) { handler.postDelayed(this, 1000); return; }
            if (action == FirstFramePolicy.Action.SUCCESS) {
                LegacySessionService.access$record(s, "R2：首帧已显示；取消无首帧恢复并重置重试计数"); return;
            }
            if (action == FirstFramePolicy.Action.LIMIT) {
                LegacySessionService.access$update(s, "已自动重连 2 次仍无首帧；请检查 iPhone CarPlay 授权和车机热点，关闭再开启自动连接可重新尝试"); return;
            }
            if (action != FirstFramePolicy.Action.RETRY) return;
            LegacySessionService.access$update(s, "R3：60 秒无首帧，自动重连 " + policy.retries() + "/2；正在关闭本次会话，随后按原退避恢复已保存 iPhone");
            // A UI listener invoked by update() may synchronously cancel or replace the session.
            if (token != LegacySessionService.access$getGeneration$p(s) || !eligible(s)
                || owner.get() != s.getController() || LegacySessionService.access$getVideoFrameSeen$p(s)) return;
            s.stopSession(true, true);
        }
    }
}
