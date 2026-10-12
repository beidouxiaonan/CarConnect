package com.shilapi.xcertplay.patch;
import java.io.IOException;
/** Do not tear down a potentially pending VF request during its retry window. */
final class SppCleanupSequence {
    static boolean prepare(SppRelease release,boolean enabled,boolean owned,SppRelease.Port port,
            final SppRelease.Monitor monitor,final SppRelease.Time time,final OemSppGate.RetryWindow retry)throws Exception {
        return release.ensureFree(enabled,owned,port,new SppRelease.Monitor(){
            public void check()throws Exception {
                monitor.check();
                long remaining=retry.remainingMillis(time.now());
                if(remaining>0)throw new IOException("原车 VF 请求冷却中，约 "+((remaining+999)/1000)+" 秒后再试；暂不清理 SPP，手机记录已保留");
            }
            public void log(String value){monitor.log(value);}
        },time);
    }
}
