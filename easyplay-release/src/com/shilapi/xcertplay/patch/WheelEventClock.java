package com.shilapi.xcertplay.patch;

import java.util.LinkedHashMap;

/** Per-receiver physical presses, without trusting repeated OEM event timestamps. */
public final class WheelEventClock {
    static final long ECHO_MS = 200;
    private final LinkedHashMap<Long, Press> presses = new LinkedHashMap<Long, Press>();
    private long serial;
    private long lastNow = -1;

    public static final class Times {
        public final long down, event;
        Times(long down, long event) { this.down = down; this.event = event; }
    }
    private static final class Press {
        Press() {}
        long rawDown, rawFirst, down, event, started, seen;
        boolean held;
    }
    public synchronized Times normalize(int device, int key, int action, int repeat,
                                        long down, long event, long now) {
        if (action != 0 && action != 1) return new Times(down, event);
        if (lastNow > now) presses.clear();
        lastNow = now;
        long id = ((long) device << 32) | (key & 0xffffffffL);
        Press p = presses.get(id);
        if (action == 0 && repeat == 0) {
            boolean echo = p != null && p.rawDown == down && now >= p.seen &&
                now - p.seen <= ECHO_MS &&
                (p.held || p.rawFirst == event);
            if (echo) {
                // Do not extend the duplicate window with every echo.
                return new Times(p.down, p.event);
            }
            if (presses.size() >= 32 && !presses.containsKey(id))
                presses.remove(presses.keySet().iterator().next());
            p = new Press();
            p.rawDown = down; p.rawFirst = event;
            p.down = Math.max(now, serial + 1); serial = p.down;
            p.event = p.down; p.started = now; p.seen = now; p.held = true;
            presses.put(id, p);
            return new Times(p.down, p.event);
        }
        if (p == null || p.rawDown != down) return new Times(down, event);
        long elapsed = Math.max(0, now - p.started);
        // Valid device time wins; arrival time handles zero/constant timestamps.
        if (event >= p.rawFirst && event != p.rawFirst) elapsed = event - p.rawFirst;
        if (action == 0 && !p.held) return new Times(down, event);
        p.seen = now;
        if (action == 1) p.held = false;
        return new Times(p.down, p.down + elapsed);
    }
    public synchronized void clear() { presses.clear(); lastNow = -1; }
    synchronized int size() { return presses.size(); }
}
