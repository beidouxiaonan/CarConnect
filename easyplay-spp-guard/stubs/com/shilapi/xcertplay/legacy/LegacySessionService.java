package com.shilapi.xcertplay.legacy;
/** Compile-only signatures verified against the exact OEM 0.1.8 DEX. Never packaged. */
public abstract class LegacySessionService extends android.app.Service {
    public abstract boolean getClosing();
    public abstract boolean getWaitingAutomatically();
    public abstract boolean getWired$legacy();
    public abstract com.shilapi.xcertplay.orchestration.CarPlayController getController();
    public abstract void stopSession(boolean keepStatus, boolean retainAutomatic);
    public static boolean access$getDestroyed$p(LegacySessionService s) { throw new AssertionError(); }
    public static int access$getGeneration$p(LegacySessionService s) { throw new AssertionError(); }
    public static android.os.Handler access$getMain$p(LegacySessionService s) { throw new AssertionError(); }
    public static android.view.Surface access$getSurface$p(LegacySessionService s) { throw new AssertionError(); }
    public static boolean access$getVideoFrameSeen$p(LegacySessionService s) { throw new AssertionError(); }
    public static void access$record(LegacySessionService s, String message) { throw new AssertionError(); }
    public static void access$update(LegacySessionService s, String message) { throw new AssertionError(); }
}
