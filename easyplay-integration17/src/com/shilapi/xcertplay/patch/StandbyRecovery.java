package com.shilapi.xcertplay.patch;
import android.content.*;
import android.os.*;
import android.widget.*;
import com.shilapi.xcertplay.legacy.LegacySessionService;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

/** Opt-in screen-off session teardown. SCREEN_OFF is a proxy, not proof of vehicle ACC off. */
public final class StandbyRecovery {
    private static final String PREF="oemStandbyRecovery14";
    private static final StandbyPolicy policy=new StandbyPolicy();
    private static final Handler main=new Handler(Looper.getMainLooper());
    private static final AtomicBoolean working=new AtomicBoolean();
    private static WeakReference<LegacySessionService> service=new WeakReference<LegacySessionService>(null);
    private static BroadcastReceiver receiver;
    private static Context app;
    private static long elapsed,uptime;
    private static boolean resumeAutomatic;
    private static int stoppedGeneration;
    private static String savedPhone;
    public static boolean enabled(Context c){return c.getSharedPreferences("easyplay_enhancements",0).getBoolean(PREF,false);}
    // Callers read lifecycle/report fields only on the main thread.
    static LegacySessionService currentService(){LegacySessionService s=service.get();return live(s)?s:null;}
    private static boolean automatic(Context c){return c.getSharedPreferences("legacy-settings",0).getBoolean("autoConnect",true)
        &&!c.getSharedPreferences("legacy-settings",0).getBoolean("wired",false);}
    public static void add(final android.app.Activity a,LinearLayout panel){
        CheckBox b=new CheckBox(a);b.setTextColor(android.graphics.Color.WHITE);
        b.setText("休眠恢复：灭屏结束无线会话，亮屏复查回连（默认关闭）");b.setChecked(enabled(a));panel.addView(b);
        b.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){public void onCheckedChanged(CompoundButton v,boolean on){
            a.getSharedPreferences("easyplay_enhancements",0).edit().putBoolean(PREF,on).apply();
            OemTrace.event("STANDBY option="+on);
            if(!on){policy.cancel();resumeAutomatic=false;}
            Toast.makeText(a,on?"灭屏也会停止 CarPlay 音乐；亮屏确认空闲后恢复已选手机":"已关闭休眠恢复；不会自动重新启动刚才停止的会话",Toast.LENGTH_LONG).show();
        }});
    }
    public static void created(LegacySessionService s){
        OemTrace.install(s);service=new WeakReference<LegacySessionService>(s);app=s.getApplicationContext();
        OemTrace.event("SERVICE_CREATE standby="+enabled(s));
        if(receiver==null){
            receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){
                if(Intent.ACTION_SCREEN_OFF.equals(i.getAction()))off(false);
                else if(Intent.ACTION_SCREEN_ON.equals(i.getAction()))wake();
            }};
            IntentFilter f=new IntentFilter(Intent.ACTION_SCREEN_OFF);f.addAction(Intent.ACTION_SCREEN_ON);app.registerReceiver(receiver,f);
        }
        elapsed=SystemClock.elapsedRealtime();uptime=SystemClock.uptimeMillis();main.removeCallbacks(heartbeat);main.postDelayed(heartbeat,5000);
    }
    public static void destroyed(LegacySessionService s){
        if(service.get()!=s)return;
        OemTrace.event("SERVICE_DESTROY standby cancelled");policy.cancel();resumeAutomatic=false;service.clear();main.removeCallbacks(heartbeat);
        if(receiver!=null){try{app.unregisterReceiver(receiver);}catch(IllegalArgumentException ignored){}receiver=null;}
    }
    public static void check()throws IOException{if(policy.blocked())throw new IOException("休眠恢复处理中；旧会话已暂停，未发送新 VF");}
    private static final Runnable heartbeat=new Runnable(){public void run(){
        LegacySessionService s=service.get();if(s==null)return;
        long e=SystemClock.elapsedRealtime(),u=SystemClock.uptimeMillis(),gap=(e-elapsed)-(u-uptime);
        elapsed=e;uptime=u;
        if(gap>=2000){
            OemTrace.event("SUSPEND_GAP deepSleepMs="+gap+" screenOn="+screenOn());
            // Some vendor systems omit SCREEN_OFF. No disconnect is sent retroactively on wake.
            if(!policy.blocked()&&enabled(s)&&screenOn()){off(true);wake();}
        }
        main.postDelayed(this,5000);
    }};
    private static boolean screenOn(){PowerManager p=(PowerManager)app.getSystemService(Context.POWER_SERVICE);return p!=null&&p.isScreenOn();}
    private static boolean live(LegacySessionService s){return s!=null&&service.get()==s&&!LegacySessionService.access$getDestroyed$p(s);}
    private static void off(final boolean missed){
        final LegacySessionService s=service.get();
        OemTrace.event("SCREEN_OFF missed="+missed+" elapsed="+SystemClock.elapsedRealtime()+" uptime="+SystemClock.uptimeMillis());
        // A second screen-off during reconciliation must invalidate the older wake callback,
        // even though stopSession has already cleared the Service's automatic flag.
        if(policy.blocked()){long n=policy.off();if(n!=0)OemTrace.event("STANDBY #"+n+" wake interrupted by another screen-off");return;}
        if(!live(s)||!enabled(s)||!EnhancementPrefs.getOemBluetooth(s)||s.getWired$legacy()
            ||s.getClosing()||(!LegacySessionService.access$getAutomatic$p(s)&&s.getController()==null))return;
        final long token=policy.off();if(token==0)return;
        resumeAutomatic=LegacySessionService.access$getAutomatic$p(s)&&automatic(s);savedPhone=RememberedPhone.address(s);
        final Object owned=OemTransport.captureStandbyOwner(s.getController(),LegacySessionService.access$getVideoFrameSeen$p(s));
        OemTrace.event("STANDBY #"+token+" stop local session; provenOwner="+(owned!=null)+" restoreAutomatic="+resumeAutomatic);
        PowerManager.WakeLock hold=null;
        if(!missed&&owned!=null){
            try{PowerManager pm=(PowerManager)app.getSystemService(Context.POWER_SERVICE);
                hold=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"CarConnect:standby-release");hold.setReferenceCounted(false);hold.acquire(8000);
                OemTrace.event("STANDBY #"+token+" CPU hold maxMs=8000 for owned session teardown");
            }catch(Exception error){OemTrace.error("STANDBY short CPU hold",error);}
        }
        s.stopSession(true,false);stoppedGeneration=LegacySessionService.access$getGeneration$p(s);
        if(!missed&&owned!=null)waitForClose(s,token,owned,SystemClock.elapsedRealtime()+8000,hold);
    }
    private static void releaseHold(PowerManager.WakeLock hold){if(hold!=null)try{if(hold.isHeld())hold.release();}catch(RuntimeException ignored){}}
    private static void waitForClose(final LegacySessionService s,final long token,final Object owned,final long until,final PowerManager.WakeLock hold){
        if(!live(s)||!policy.offValid(token)||!enabled(s)||SystemClock.elapsedRealtime()>=until){releaseHold(hold);return;}
        if(s.getClosing()||!working.compareAndSet(false,true)){
            main.postDelayed(new Runnable(){public void run(){waitForClose(s,token,owned,until,hold);}},200);return;
        }
        worker(new Runnable(){public void run(){
            try{OemTransport.releaseStandbyOwner(app,owned,new SppIdleRecovery.Clock(){
                public long now(){return SystemClock.elapsedRealtime()*1000000L;}
                public void pause()throws Exception{Thread.sleep(100);}
                public void check()throws Exception{if(!policy.offValid(token)||!enabled(app)||SystemClock.elapsedRealtime()>=until)
                    throw new IOException("灭屏释放已取消或超时；不会在亮屏后补发断开请求");}
            });}catch(Exception error){OemTrace.error("STANDBY #"+token+" owned release",error);}
            finally{working.set(false);releaseHold(hold);}
        }},"carconnect-standby-release");
    }
    private static void wake(){
        OemTrace.event("SCREEN_ON elapsed="+SystemClock.elapsedRealtime()+" uptime="+SystemClock.uptimeMillis());
        long token=policy.wake();if(token==0)return;
        LegacySessionService s=service.get();
        if(!live(s)||!enabled(s)||!resumeAutomatic||!automatic(s)||savedPhone==null){policy.complete(token);return;}
        LegacySessionService.access$update(s,"休眠唤醒：等待原车蓝牙就绪，复查 SPP 后恢复已保存 iPhone");
        waitWake(s,token,SystemClock.elapsedRealtime()+20000);
    }
    private static void waitWake(final LegacySessionService s,final long token,final long until){
        if(!live(s)||!policy.wakeValid(token))return;
        if(!enabled(s)||!automatic(s)||!savedPhone.equals(RememberedPhone.address(s))||SystemClock.elapsedRealtime()>=until){
            policy.complete(token);LegacySessionService.access$update(s,"休眠复查已取消或超时；已选 iPhone 保留，请查看连接诊断");return;
        }
        if(s.getClosing()||working.get()) {main.postDelayed(new Runnable(){public void run(){waitWake(s,token,until);}},250);return;}
        if(LegacySessionService.access$getGeneration$p(s)!=stoppedGeneration||s.getController()!=null){policy.complete(token);return;}
        if(!working.compareAndSet(false,true))return;
        final String expected=savedPhone;
        worker(new Runnable(){public void run(){
            boolean idle=false;String failure="未复查";
            final SppIdleRecovery.Clock guard=new SppIdleRecovery.Clock(){
                public long now(){return SystemClock.elapsedRealtime()*1000000L;}
                public void pause()throws Exception{Thread.sleep(100);}
                public void check()throws Exception{if(!policy.wakeValid(token)||!enabled(app)||!automatic(app)||!expected.equals(RememberedPhone.address(app))||SystemClock.elapsedRealtime()>=until)
                    throw new IOException("唤醒任务已取消或超时");}
            };
            try{
                while(SystemClock.elapsedRealtime()<until){
                    guard.check();try{OemTransport.confirmIdle(app,guard);guard.check();idle=true;break;}
                    catch(Exception error){failure=error.getMessage();OemTrace.error("WAKE #"+token+" readonly check",error);guard.check();Thread.sleep(1000);}
                }
            }catch(Exception error){failure=error.getMessage();}
            finally{working.set(false);}
            final boolean ready=idle;final String reason=failure;
            main.post(new Runnable(){public void run(){
                if(!live(s)||!policy.wakeValid(token))return;
                boolean eligible=ready&&enabled(s)&&automatic(s)&&expected.equals(RememberedPhone.address(s))
                    &&SystemClock.elapsedRealtime()<until&&!s.getClosing()&&s.getController()==null
                    &&LegacySessionService.access$getGeneration$p(s)==stoppedGeneration;
                if(!policy.complete(token))return;
                if(!eligible){LegacySessionService.access$update(s,"休眠复查未通过："+(ready?"设置或会话已经变化":reason)+"；未重复清理，手机选择保留");return;}
                OemTrace.event("WAKE #"+token+" idle confirmed; restart saved automatic session");
                try{s.startService(new Intent(s,LegacySessionService.class).setAction("connect"));}
                catch(Exception error){OemTrace.error("WAKE service restart",error);LegacySessionService.access$update(s,"唤醒复查完成，请手动开启自动连接");}
            }});
        }},"carconnect-standby-recheck");
    }
    private static void worker(Runnable r,String name){Thread t=new Thread(r,name);t.setDaemon(true);t.start();}
}
