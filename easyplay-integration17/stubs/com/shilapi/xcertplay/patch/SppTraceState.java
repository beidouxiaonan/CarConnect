package com.shilapi.xcertplay.patch;
/** Bounded, address-free evidence. OEM broadcasts are unauthenticated on these systems. */
public final class SppTraceState {
    private int events, lastEvent = -1, cacheChanges;
    private long eventAt, requestedAt;
    private boolean cacheKnown, cached, requested;
    private int eventsAtRequest;
    public synchronized void event(int value, long now) {
        if (value != 0 && value != 1) return;
        events++; lastEvent = value; eventAt = now;
    }
    public synchronized void cache(boolean value, long now) {
        if (cacheKnown && cached != value) cacheChanges++;
        cached = value; cacheKnown = true;
    }
    public synchronized void request(long now) {
        requested = true; requestedAt = now; eventsAtRequest = events;
    }
    public synchronized String summary(long now) {
        String text = "SPP 观察：Binder缓存=" + (cacheKnown ? (cached ? "连接" : "未连接") : "未读取")
            + "，状态变化=" + cacheChanges + "，广播次数=" + events;
        if (lastEvent >= 0) text += "，最近广播=" + (lastEvent == 1 ? "连接" : "断开")
            + "（" + Math.max(0, (now - eventAt)/1000) + "秒前）";
        if (requested) text += "，清理请求后广播=" + (events - eventsAtRequest)
            + "，已等=" + Math.max(0, (now - requestedAt)/1000) + "秒";
        if (requested && cacheKnown && cached && events == eventsAtRequest)
            text += "；未观察到新回执，无法区分真实占用与缓存残留";
        if (cacheKnown && cached && lastEvent == 0 && requested && eventAt >= requestedAt)
            text += "；断开广播与缓存不一致，仍停止接入";
        return text + "。广播只作诊断，不替代清理成功校验。";
    }
}
