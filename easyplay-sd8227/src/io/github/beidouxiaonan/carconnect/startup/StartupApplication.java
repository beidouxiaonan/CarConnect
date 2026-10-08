package io.github.beidouxiaonan.carconnect.startup;

import android.app.Application;
import android.content.Context;
import android.os.Process;
import java.lang.reflect.InvocationTargetException;

/** Install legacy multidex after the durable reporter, so its failure is visible. */
public final class StartupApplication extends Application {
    public static boolean multidexReady;
    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        StartupLog.device(this);
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread thread, Throwable error) {
                try { StartupLog.failure(StartupApplication.this, "UNCAUGHT thread=" + thread.getName(), error); }
                finally {
                    // Preserve Android's crash handling; never continue an inconsistent process.
                    if (previous != null) previous.uncaughtException(thread, error);
                    else { Process.killProcess(Process.myPid()); System.exit(10); }
                }
            }
        });
        LegacyDexInstaller.setReporter(new LegacyDexInstaller.Reporter() {
            @Override public void stage(String message) { StartupLog.stage(StartupApplication.this, message); }
        });
        StartupLog.stage(this, "MULTIDEX_BEGIN runtime-signature-adapter=0.1.7");
        try {
            Class.forName("androidx.multidex.MultiDex").getMethod("install", Context.class).invoke(null, this);
            multidexReady = true;
            StartupLog.stage(this, "MULTIDEX_OK");
        } catch (Throwable error) {
            if (error instanceof InvocationTargetException && error.getCause() != null) error = error.getCause();
            StartupLog.failure(this, "MULTIDEX", error);
        }
    }
    @Override public void onCreate() {
        super.onCreate(); StartupLog.stage(this, "APPLICATION_CREATED");
    }
}
