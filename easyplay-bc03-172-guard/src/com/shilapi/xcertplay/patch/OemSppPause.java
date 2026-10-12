package com.shilapi.xcertplay.patch;
import java.lang.ref.WeakReference;
import com.shilapi.xcertplay.legacy.LegacySessionService;
import com.shilapi.xcertplay.orchestration.CarPlayController;
/** Typed, owner-specific pause; other failures retain the original retry behavior. */
public final class OemSppPause {
    private static WeakReference<CarPlayController> blocked=new WeakReference<CarPlayController>(null);
    private static String reason;
    public static synchronized void mark(CarPlayController owner,String value){
        if(owner!=null){blocked=new WeakReference<CarPlayController>(owner);reason=value;}
    }
    public static synchronized void forget(CarPlayController owner){
        if(owner!=null&&blocked.get()==owner){blocked.clear();reason=null;}
    }
    private static synchronized String take(CarPlayController owner){
        if(owner==null||blocked.get()!=owner)return null;
        String value=reason;blocked.clear();reason=null;return value;
    }
    /** Called at the exact original Failed/ControlEnded stop site, after generation validation. */
    public static void failed(LegacySessionService service){
        CarPlayController owner=service.getController();
        String value=service.getWired$legacy()?null:take(owner);
        if(value==null){service.stopSession(true,true);return;}
        int generation=LegacySessionService.access$getGeneration$p(service);
        LegacySessionService.access$record(service,"P1：SPP 恢复未确认；暂停本轮自动重试，不再循环发送断开请求："+value);
        if(service.getController()!=owner||service.getClosing()
            ||generation!=LegacySessionService.access$getGeneration$p(service))return;
        // This keeps saved preferences but removes this Service's retry loop and closes its old worker.
        service.stopSession(true,false);
        if(service.getController()==null&&service.getClosing()
            &&generation+1==LegacySessionService.access$getGeneration$p(service))
            LegacySessionService.access$update(service,"SPP 状态未释放，本轮自动连接已暂停；请先在原车蓝牙断开投屏或关闭再开启蓝牙，恢复后关闭再开启自动连接。已选 iPhone 保留。");
    }
}
