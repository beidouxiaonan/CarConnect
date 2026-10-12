package com.shilapi.xcertplay.patch;
import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.View;
import android.widget.*;
import com.shilapi.xcertplay.legacy.LegacyActivity;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Explicit user recovery, plus an address-redacted file share. All I/O is off the UI thread. */
public final class ConnectionDiagnostics {
    private static final AtomicBoolean checking=new AtomicBoolean(),exporting=new AtomicBoolean();
    public static void add(Activity activity,LinearLayout panel){
        OemTrace.install(activity);
        AllLogsExport.add(activity,panel);
        final WeakReference<Activity> reference=new WeakReference<Activity>(activity);
        Button retry=new Button(activity);retry.setText("复查 SPP 并恢复连接（不清理）");panel.addView(retry);
        retry.setOnClickListener(new View.OnClickListener(){public void onClick(View view){recheck(reference);}});
        Button export=new Button(activity);export.setText("导出蓝牙诊断日志");panel.addView(export);
        export.setOnClickListener(new View.OnClickListener(){public void onClick(View view){export(reference);}});
        Button copy=new Button(activity);copy.setText("复制蓝牙诊断日志（无需分享应用）");panel.addView(copy);
        copy.setOnClickListener(new View.OnClickListener(){public void onClick(View view){copyReport(reference);}});
    }
    private static Activity live(WeakReference<Activity> ref){Activity a=ref.get();return a==null||a.isFinishing()||a.isDestroyed()?null:a;}
    private static void tell(WeakReference<Activity> ref,String message){Activity a=live(ref);if(a!=null)Toast.makeText(a,message,Toast.LENGTH_LONG).show();}
    private static void recheck(final WeakReference<Activity> ref){
        Activity activity=live(ref);if(activity==null)return;
        if(!checking.compareAndSet(false,true)){tell(ref,"复查仍在运行，请等待结束；不会重复清理");return;}
        final Context app=activity.getApplicationContext();final Handler main=new Handler(Looper.getMainLooper());
        final AtomicBoolean expired=new AtomicBoolean(),delivered=new AtomicBoolean();
        final long start=System.nanoTime();
        final SppIdleRecovery.Clock guard=new SppIdleRecovery.Clock(){
            public long now(){return System.nanoTime();}
            public void pause()throws Exception {Thread.sleep(100);}
            public void check()throws Exception {
                if(expired.get()||Thread.currentThread().isInterrupted()||live(ref)==null||now()-start>=8000000000L)
                    throw new IOException("复查已取消或超过 8 秒；不会在后台恢复连接");
            }
        };
        final Runnable timeout=new Runnable(){public void run(){
            expired.set(true);
            if(delivered.compareAndSet(false,true))tell(ref,"原车查询尚未返回，复查已取消。等待后台查询退出后再试；未发送清理命令。");
        }};
        main.postDelayed(timeout,8000);tell(ref,"正在只读复查原车状态；确认空闲后恢复已选 iPhone");
        Thread worker=new Thread(new Runnable(){public void run(){
            Bc03Access.Snapshot result=null;String failure=null;
            try{result=OemTransport.confirmIdle(app,guard);guard.check();}
            catch(Exception error){failure=error.getMessage();}
            finally{checking.set(false);}
            final Bc03Access.Snapshot phone=result;final String message=failure;
            main.post(new Runnable(){public void run(){
                main.removeCallbacks(timeout);
                if(!delivered.compareAndSet(false,true)||expired.get())return;
                Activity a=live(ref);if(a==null)return;
                if(message!=null){tell(ref,message);return;}
                if(!EnhancementPrefs.getOemBluetooth(app)||!phone.phone.equals(RememberedPhone.address(app))){tell(ref,"选项或已选手机已变化，未启动旧连接");return;}
                if(!(a instanceof LegacyActivity)){tell(ref,"复查已完成，请回主页面开启自动连接");return;}
                OemTrace.event("IDLE_RECHECK resume saved phone; normal connection still revalidates all gates");
                ((LegacyActivity)a).patchUseRememberedPhone(phone.phone,phone.name);
                tell(ref,"SPP 已确认空闲，已恢复连接；手机选择保留");
            }});
        }},"carconnect-spp-idle-recheck");worker.setDaemon(true);worker.start();
    }
    private static void copyReport(final WeakReference<Activity> ref){
        final Activity a=live(ref);if(a==null)return;
        final Context app=a.getApplicationContext();
        Thread worker=new Thread(new Runnable(){public void run(){
            try{final String text=ReportFiles.excerpt(OemTrace.summary(app));
                new Handler(Looper.getMainLooper()).post(new Runnable(){public void run(){copy(ref,text);}});
            }catch(Exception error){OemTrace.error("REPORT copy",error);}
        }},"carconnect-report-copy");worker.setDaemon(true);worker.start();
    }
    private static void copy(WeakReference<Activity> ref,String text){
        Activity a=live(ref);if(a==null)return;
        try{((ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("CarConnect diagnostics",text));
            tell(ref,"诊断已复制；完整日志可保存为文件");
        }catch(Exception error){tell(ref,"复制失败："+error.getMessage());}
    }
    private static void export(final WeakReference<Activity> ref){
        Activity activity=live(ref);if(activity==null)return;
        if(!exporting.compareAndSet(false,true)){tell(ref,"正在生成诊断文件");return;}
        final Context app=activity.getApplicationContext();final Handler main=new Handler(Looper.getMainLooper());
        Thread worker=new Thread(new Runnable(){public void run(){
            File report=null,shared=null;String failure=null,text="";
            try{
                text=OemTrace.summary(app);String name="CarConnect-OEM-"+System.currentTimeMillis()+".txt";
                try{shared=ReportFiles.save(text,name,new File(app.getFilesDir(),"oem-shared"));}
                catch(Exception error){OemTrace.error("REPORT private share file",error);}
                File downloads=Build.VERSION.SDK_INT<=22?new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"CarConnect"):null;
                report=ReportFiles.save(text,name,downloads,app.getExternalFilesDir("diagnostics"));
                OemTrace.event("REPORT directly saved bytes="+report.length()+" path="+report.getAbsolutePath());
            }catch(Exception error){failure=error.getMessage();OemTrace.error("REPORT export",error);}
            finally{exporting.set(false);}
            final File ready=report,shareFile=shared;final String error=failure,preview=ReportFiles.excerpt(text);
            main.post(new Runnable(){public void run(){
                Activity a=live(ref);if(a==null)return;
                TextView view=new TextView(a);view.setTextIsSelectable(true);view.setText(
                    (ready==null?"外部文件保存失败："+error+"\n可长按或点击下方复制日志。":
                    "已直接保存，无需分享应用：\n"+ready.getAbsolutePath()+"\n文件大小："+ready.length()+" 字节")+"\n\n"+preview);
                view.setPadding(16,16,16,16);ScrollView scroll=new ScrollView(a);scroll.addView(view);
                android.app.AlertDialog.Builder dialog=new android.app.AlertDialog.Builder(a).setTitle("蓝牙诊断日志")
                    .setView(scroll).setPositiveButton("复制",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){copy(ref,preview);}})
                    .setNegativeButton("完成",null);
                if(shareFile!=null){
                    final Uri uri=Uri.parse("content://"+OemDiagnosticProvider.AUTHORITY+"/"+shareFile.getName());
                    final Intent share=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_STREAM,uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    share.setClipData(ClipData.newUri(a.getContentResolver(),"CarConnect diagnostics",uri));
                    if(!a.getPackageManager().queryIntentActivities(share,0).isEmpty())dialog.setNeutralButton("分享",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){
                        Activity current=live(ref);if(current==null)return;
                        try{current.startActivity(Intent.createChooser(share,"分享诊断日志"));}
                        catch(ActivityNotFoundException missing){tell(ref,"分享应用不可用，文件仍已保存");}
                    }});
                }
                dialog.show();
            }});
        }},"carconnect-oem-report-export");worker.setDaemon(true);worker.start();
    }
}
