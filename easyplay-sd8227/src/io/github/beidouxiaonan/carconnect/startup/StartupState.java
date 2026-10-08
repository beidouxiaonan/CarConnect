package io.github.beidouxiaonan.carconnect.startup;

/** Recovery decisions are independent of Android and never retry a failed boot. */
public final class StartupState {
    private StartupState() {}
    public static boolean shouldAutoStart(String state, boolean diagnosticIntent, boolean multidexReady) {
        return multidexReady && !diagnosticIntent && "ready".equals(state);
    }
}
