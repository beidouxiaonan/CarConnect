package com.shilapi.xcertplay.patch;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.*;
import android.widget.*;
import com.shilapi.xcertplay.legacy.*;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;
/** One foreground user operation: stop stale local work, recover once, then start the remembered phone. */
final class ManualRecovery {
    private static final AtomicBoolean busy=new AtomicBoolean();
    private static final Handler main=new Handler(Looper.getMainLooper());
    private static volatile Scope active;
    static void checkConnection()throws IOException{if(busy.get())throw new IOException("手动恢复仍在运行，请等待结果；未启动新的蓝牙连接");}
    static void progress(final String message){
        final Scope op=active;if(op==null)return;
        main.post(new Runnable(){public void run(){if(active==op&&!op.finished.get()&&!op.cancelled.get()&&op.dialog!=null&&op.dialog.isShowing()){
            String text=message.contains("C4")?"原车接口暂未释放，等待一次底层恢复结果（最长约 12 秒）":
                message.contains("C2")?"已请求释放原车 SPP，等待原车状态确认":message.contains("C1")?"正在检查并请求释放原车 SPP":message;
            op.dialog.setMessage(text+"\n\n请等待结果；当前不会更换手机或启动其他连接。");
        }}});
    }
    static void start(Activity a){
        if(!(a instanceof LegacyActivity)){tell(a,"请回连接主页面使用一次恢复");return;}
        final String saved=RememberedPhone.address(a);
        if(saved==null||!EnhancementPrefs.getOemBluetooth(a)){tell(a,"请先选择 iPhone 并启用原车蓝牙通道");return;}
        LegacySessionService s=StandbyRecovery.currentService();
        if(s!=null&&(s.getWired$legacy()||LegacySessionService.access$getVideoFrameSeen$p(s))){tell(a,"现有会话正在使用，请先正常断开后再恢复");return;}
        if(!busy.compareAndSet(false,true)){tell(a,"一次恢复仍在运行，请等待结果");return;}
        Scope op=new Scope(a,s,saved);
        active=op;
        try{
            StandbyRecovery.check();
            if(s!=null){s.stopSession(true,false);op.generation=LegacySessionService.access$getGeneration$p(s);
                LegacySessionService.access$update(s,"手动恢复：等待旧会话退出，然后仅尝试一次 SPP 释放");}
            OemTrace.event("MANUAL_RECOVERY user requested; durablePending="+SppRecoveryPrefs.pending(op.app));
            op.show();
            op.waitClosed();
        }catch(Exception error){op.dismiss();active=null;busy.set(false);OemTrace.error("MANUAL_RECOVERY START",error);tell(a,error.getMessage());}
    }
    private static void tell(Activity a,String text){if(a!=null&&!a.isFinishing()&&!a.isDestroyed())Toast.makeText(a,text,Toast.LENGTH_LONG).show();}
    private static final class Scope implements Runnable {
        final WeakReference<Activity> activity;
        final Context app;final LegacySessionService service;final String saved;
        final boolean autoOption;final long start=SystemClock.elapsedRealtime();
        final AtomicBoolean cancelled=new AtomicBoolean(),finished=new AtomicBoolean(),delivered=new AtomicBoolean();
        ProgressDialog dialog;volatile String cancelReason;
        int generation;
        Scope(Activity a,LegacySessionService s,String phone){activity=new WeakReference<Activity>(a);app=a.getApplicationContext();service=s;saved=phone;
            autoOption=app.getSharedPreferences("legacy-settings",0).getBoolean("autoConnect",true);}
        String invalidReason(){
            Activity a=activity.get();PowerManager p=(PowerManager)app.getSystemService(Context.POWER_SERVICE);
            if(a==null||a.isFinishing()||a.isDestroyed())return "连接页面已关闭";
            if(p==null||!p.isScreenOn())return "车机已灭屏";
            if(!EnhancementPrefs.getOemBluetooth(app))return "原车通道已关闭";
            if(!saved.equals(RememberedPhone.address(app)))return "已选 iPhone 改变";
            if(app.getSharedPreferences("legacy-settings",0).getBoolean("wired",false))return "已切换有线连接";
            if(autoOption!=app.getSharedPreferences("legacy-settings",0).getBoolean("autoConnect",true))return "自动连接选项改变";
            if(StandbyRecovery.currentService()!=service)return "连接服务已重建";
            if(service!=null&&LegacySessionService.access$getGeneration$p(service)!=generation)return "另一个连接操作改变了会话";
            if(service!=null&&service.getController()!=null)return "另一个连接任务已经启动";
            if(SystemClock.elapsedRealtime()-start>=70000)return "恢复操作超过 70 秒";
            return null;
        }
        void cancel(String reason){
            if(cancelled.compareAndSet(false,true)){cancelReason=reason;OemTrace.event("MANUAL_RECOVERY CANCEL reason="+reason+" elapsedMs="+(SystemClock.elapsedRealtime()-start));}
        }
        void show(){
            dialog=new ProgressDialog(activity.get());dialog.setTitle("正在恢复原车 SPP");dialog.setIndeterminate(true);
            dialog.setMessage("等待旧本地会话退出；释放请求最多执行一次。请等待结果。");dialog.setCancelable(false);dialog.setCanceledOnTouchOutside(false);
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE,"取消后续操作",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){}});
            dialog.show();dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(new android.view.View.OnClickListener(){public void onClick(android.view.View v){
                cancel("用户取消后续操作");dialog.setMessage("已取消后续操作，正在等待当前查询退出。已发出的原车请求不能撤销；保留未完成标记。");v.setEnabled(false);
            }});
        }
        void dismiss(){if(dialog!=null){try{dialog.dismiss();}catch(RuntimeException ignored){}dialog=null;}}
        void waitClosed(){
            String reason=invalidReason();
            if(cancelled.get()||reason!=null){cancel(reason==null?cancelReason:reason);finish("恢复已取消："+cancelReason+"；未启动旧连接");active=null;busy.set(false);return;}
            if(service!=null&&service.getClosing()){
                if(SystemClock.elapsedRealtime()-start>=10000){cancel("旧会话退出超过 10 秒");finish("旧会话尚未退出，未发送恢复命令，请等待后再试");active=null;busy.set(false);return;}
                main.postDelayed(new Runnable(){public void run(){waitClosed();}},200);return;
            }
            main.postDelayed(this,200);
            Thread worker=new Thread(new Runnable(){public void run(){
                Bc03Access.Snapshot result=null;String failure=null;
                final SppIdleRecovery.Clock guard=new SppIdleRecovery.Clock(){
                    public long now(){return System.nanoTime();}
                    public void pause()throws Exception{Thread.sleep(100);}
                    public void check()throws Exception{
                        if(cancelled.get()||Thread.currentThread().isInterrupted()||SystemClock.elapsedRealtime()-start>=70000)
                            throw new IOException("一次恢复已取消："+(cancelReason==null?"线程中断或超过 70 秒":cancelReason)+"；不会继续发送命令或启动连接");
                    }
                };
                try{result=OemTransport.recoverOnce(app,saved,guard);guard.check();}
                catch(Exception error){failure=error.getMessage()==null?error.toString():error.getMessage();}
                finally{finished.set(true);}
                final Bc03Access.Snapshot phone=result;final String error=failure;
                main.post(new Runnable(){public void run(){
                    main.removeCallbacks(Scope.this);
                    if(active==Scope.this)active=null;
                    busy.set(false);
                    String reason=invalidReason();
                    if(cancelled.get()||reason!=null){cancel(reason==null?cancelReason:reason);finish("恢复已结束："+cancelReason+"；未启动旧连接。恢复保护="+(SppRecoveryPrefs.pending(app)?"未解除":"已确认解除"));return;}
                    if(error!=null){if(service!=null)LegacySessionService.access$update(service,"一次恢复未通过："+error+"；自动重试保持暂停，请导出全部日志");
                        finish("一次恢复未通过："+error+"。请导出全部日志，勿连续重复清理。");return;}
                    if(delivered.compareAndSet(false,true)){
                        dismiss();
                        OemTrace.event("MANUAL_RECOVERY idle confirmed; resume remembered phone through normal validation");
                        ManualSppRecovery.handoff(saved);
                        ((LegacyActivity)activity.get()).patchUseRememberedPhone(phone.phone,phone.name);
                        tell(activity.get(),"SPP 已确认空闲，正在重新握手连接；是否成功以 CarPlay 画面为准");
                    }
                }});
            }},"carconnect-manual-spp-recovery");worker.setDaemon(true);worker.start();
        }
        public void run(){
            if(finished.get())return;
            String reason=invalidReason();
            if(reason!=null){
                cancel(reason);dismiss();tell(activity.get(),"恢复已取消："+cancelReason+"；等待查询退出后再试");return;
            }
            main.postDelayed(this,200);
        }
        void finish(String message){
            dismiss();OemTrace.event("MANUAL_RECOVERY RESULT "+message);
            Activity a=activity.get();if(delivered.compareAndSet(false,true)&&a!=null&&!a.isFinishing()&&!a.isDestroyed())
                new AlertDialog.Builder(a).setTitle("一次恢复结果").setMessage(message).setPositiveButton("确定",null).show();
        }
    }
}
