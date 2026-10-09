package com.shilapi.xcertplay.patch;

/** Monotonic, per-Service recovery budget. No Android or Bluetooth operations. */
public final class FirstFramePolicy {
    public static final long WAIT_MS = 60000;
    public static final int MAX_RETRIES = 2;
    public enum Action { WAIT, SUCCESS, CANCEL, RETRY, LIMIT }
    private int generation, retries;
    private long visibleSince = -1;
    private boolean armed;

    public boolean arm(int token) {
        if (armed && generation == token) return false;
        generation = token;
        visibleSince = -1;
        armed = true;
        return true;
    }
    public Action check(int token, long now, boolean eligible, boolean frame, boolean visible) {
        if (!armed || generation != token) return Action.CANCEL;
        if (!eligible) { cancel(false); return Action.CANCEL; }
        if (frame) { cancel(true); return Action.SUCCESS; }
        // A background/surface recreation interval never spends the foreground timeout.
        if (!visible) { visibleSince = -1; return Action.WAIT; }
        if (visibleSince < 0) visibleSince = now;
        if (now - visibleSince < WAIT_MS) return Action.WAIT;
        armed = false;
        if (retries >= MAX_RETRIES) return Action.LIMIT;
        retries++;
        return Action.RETRY;
    }
    public void cancel(boolean resetBudget) {
        armed = false;
        visibleSince = -1;
        if (resetBudget) retries = 0;
    }
    public int retries() { return retries; }
}
