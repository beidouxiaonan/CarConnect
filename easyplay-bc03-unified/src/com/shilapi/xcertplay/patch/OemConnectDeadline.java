package com.shilapi.xcertplay.patch;

/** Independent stage budgets plus a finite total budget; uses monotonic time. */
final class OemConnectDeadline {
    private static final long TOTAL_NANOS=45000000000L;
    private final long start;
    private volatile Phase phase;
    OemConnectDeadline(long now){start=now;phase=new Phase("连接本地 socket",now,5000000000L);}
    void enter(String label,long now,long budget){phase=new Phase(label,now,budget);}
    String expired(long now){
        Phase p=phase;
        if(now-start>=TOTAL_NANOS)return "原车数据通道总建立超时（45 秒）；阶段："+p.label;
        if(now-p.start>=p.budget)return "原车数据通道阶段超时："+p.label;
        return null;
    }
    private static final class Phase {
        final String label;final long start,budget;
        Phase(String label,long start,long budget){this.label=label;this.start=start;this.budget=budget;}
    }
}
