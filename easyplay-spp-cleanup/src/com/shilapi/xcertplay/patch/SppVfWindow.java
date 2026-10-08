package com.shilapi.xcertplay.patch;
import java.io.IOException;
/** Protect an asynchronous OEM connect even after our client socket has closed. */
public final class SppVfWindow {
    static final long WAIT=45000000000L;
    private boolean pending;private long sentAt;
    public synchronized void sent(long now){pending=true;sentAt=now;}
    public synchronized void established(){pending=false;}
    public synchronized void check(long now)throws IOException {
        long elapsed=now-sentAt;
        if(pending&&elapsed>=0&&elapsed<WAIT)
            throw new IOException("上次 VF 仍可能在原车处理中，约 "+((WAIT-elapsed+999999999L)/1000000000L)+" 秒后再清理 SPP；未发送新命令");
    }
}
