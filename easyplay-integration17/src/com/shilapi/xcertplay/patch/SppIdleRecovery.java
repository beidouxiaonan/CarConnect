package com.shilapi.xcertplay.patch;
import java.io.IOException;
/** Read-only recovery: never infers release from elapsed time or a broadcast. */
public final class SppIdleRecovery {
    public interface Port { boolean connected() throws Exception; void validate() throws Exception; void clearJournal() throws Exception; }
    public interface Clock { long now(); void pause() throws Exception; void check() throws Exception; }
    public static void confirm(Port port,Clock clock)throws Exception {
        clock.check();port.validate();clock.check();
        long start=clock.now(),idleAt=0;boolean idle=false;
        while(clock.now()-start<3000000000L){
            clock.check();boolean busy=port.connected();clock.check();
            if(busy)idle=false;
            else if(!idle){idle=true;idleAt=clock.now();}
            if(idle&&clock.now()-idleAt>=600000000L){
                port.validate();clock.check();
                if(port.connected()){idle=false;continue;}
                clock.check();port.clearJournal();return;
            }
            clock.pause();
        }
        throw new IOException("原车仍上报 SPP 占用；保留保护标记，未发送断开/VH/VF。请先关闭其他投屏连接，再复查。");
    }
}
