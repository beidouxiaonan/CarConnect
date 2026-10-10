package com.shilapi.xcertplay.patch;

import android.os.SystemClock;
import android.view.KeyEvent;
import java.util.WeakHashMap;

/** Keep original mapping/long-press dispatch; only repair its event identity. */
public final class WheelInputRecovery {
    private static final WeakHashMap<Object, WheelEventClock> clocks = new WeakHashMap<Object, WheelEventClock>();
    public static KeyEvent normalize(Object owner, KeyEvent e) {
        if (e == null || (e.getAction() != 0 && e.getAction() != 1)) return e;
        WheelEventClock c;
        synchronized (clocks) {
            c = clocks.get(owner);
            if (c == null) { c = new WheelEventClock(); clocks.put(owner, c); }
        }
        WheelEventClock.Times t = c.normalize(e.getDeviceId(), e.getKeyCode(), e.getAction(),
            e.getRepeatCount(), e.getDownTime(), e.getEventTime(), SystemClock.uptimeMillis());
        return new KeyEvent(t.down, t.event, e.getAction(), e.getKeyCode(), e.getRepeatCount(),
            e.getMetaState(), e.getDeviceId(), e.getScanCode(), e.getFlags(), e.getSource());
    }
    public static void clear(Object owner) { synchronized (clocks) { clocks.remove(owner); } }
}
