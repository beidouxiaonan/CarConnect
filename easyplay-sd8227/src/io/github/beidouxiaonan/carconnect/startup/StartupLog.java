package io.github.beidouxiaonan.carconnect.startup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import java.io.*;

/** Primary DEX only: no Kotlin, native library, Bluetooth or USB dependencies. */
public final class StartupLog {
    private static final String PREFS = "carconnect_startup_probe";
    private static final String FILE = "carconnect-startup.txt";
    private static final int LIMIT = 49152;
    private StartupLog() {}

    public static String state(Context context) {
        return context.getSharedPreferences(PREFS, 0).getString("state", "first");
    }
    public static void state(Context context, String value) {
        context.getSharedPreferences(PREFS, 0).edit().putString("state", value).commit();
    }
    public static synchronized void stage(Context context, String message) {
        try {
            File file = new File(context.getFilesDir(), FILE);
            byte[] previous = read(file);
            byte[] next = (System.currentTimeMillis() + " " + message + "\n").getBytes("UTF-8");
            FileOutputStream out = new FileOutputStream(file);
            try {
                // Keep a bounded tail across launches, including the previous failure.
                int keep = Math.min(previous.length, Math.max(0, LIMIT - next.length));
                out.write(previous, previous.length - keep, keep);
                out.write(next, Math.max(0, next.length - LIMIT), Math.min(next.length, LIMIT));
                out.getFD().sync();
            } finally { out.close(); }
        } catch (Exception error) { Log.e("CarConnect-Startup", "Cannot save startup report", error); }
        Log.i("CarConnect-Startup", message);
    }
    private static byte[] read(File file) throws IOException {
        if (!file.isFile()) return new byte[0];
        FileInputStream in = new FileInputStream(file);
        try {
            if (file.length() > LIMIT) {
                long remaining = file.length() - LIMIT;
                while (remaining > 0) {
                    long skipped = in.skip(remaining);
                    if (skipped == 0) { if (in.read() == -1) break; skipped = 1; }
                    remaining -= skipped;
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] bytes = new byte[2048]; int n;
            while ((n = in.read(bytes)) != -1 && out.size() < LIMIT) out.write(bytes, 0, n);
            return out.toByteArray();
        } finally { in.close(); }
    }
    public static String report(Context context) {
        try { return new String(read(new File(context.getFilesDir(), FILE)), "UTF-8"); }
        catch (Exception e) { return "读取启动记录失败：" + e; }
    }
    public static void failure(Context context, String phase, Throwable error) {
        try { state(context, "failed"); } catch (Exception ignored) {}
        StringWriter text = new StringWriter(); error.printStackTrace(new PrintWriter(text));
        stage(context, "FAILED " + phase + "\n" + text);
    }
    public static void activityFailed(Activity activity, Throwable error) {
        failure(activity, "MAIN_ACTIVITY", error);
        try {
            activity.startActivity(new Intent(activity, EntryActivity.class)
                .putExtra("diagnostics", true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        } catch (Exception e) { stage(activity, "RECOVERY_ACTIVITY_FAILED " + e); }
        activity.finish();
    }
    public static void ready(Context context) {
        state(context, "ready"); stage(context, "MAIN_RESUMED");
    }
    public static void device(Context context) {
        stage(context, "PROCESS_BEGIN build=0.1.7-SD8227-multidex-test SDK=" + Build.VERSION.SDK_INT + " Android=" + Build.VERSION.RELEASE
            + " model=" + Build.MODEL + " device=" + Build.DEVICE + " hardware=" + Build.HARDWARE
            + " VM=" + System.getProperty("java.vm.version") + " ABI=" + Build.CPU_ABI + "/" + Build.CPU_ABI2 + " freeBytes=" + context.getFilesDir().getUsableSpace());
    }
}
