package com.shilapi.xcertplay.patch;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.View;
import android.widget.*;
import com.shilapi.xcertplay.legacy.LegacySessionService;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;
/** Gather existing app report APIs, with UI state snapshotted on main and file I/O on a worker. */
public final class AllLogsExport {
    private static final AtomicBoolean busy=new AtomicBoolean();
    private static final String KEY="allLogsExportFolder15";
    static String destination(Context c){
        String chosen=c.getSharedPreferences("easyplay_enhancements",0).getString(KEY,"");
        if(chosen.length()!=0)return chosen;
        File folder=Build.VERSION.SDK_INT<=22?new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"CarConnect"):
            c.getExternalFilesDir("all-logs");
        return folder==null?"":folder.getAbsolutePath();
    }
    public static void add(final Activity a,LinearLayout panel){
        final TextView path=new TextView(a);path.setTextColor(android.graphics.Color.WHITE);
        path.setText("全部日志导出文件夹："+destination(a));panel.addView(path);
        Button choose=new Button(a);choose.setText("设置全部日志导出文件夹");panel.addView(choose);
        choose.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
            final EditText input=new EditText(a);input.setSingleLine(true);input.setText(destination(a));
            final AlertDialog dialog=new AlertDialog.Builder(a).setTitle("指定日志文件夹（绝对路径）").setView(input)
                .setPositiveButton("保存",null).setNegativeButton("取消",null).setNeutralButton("恢复默认",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){
                    if(a.getSharedPreferences("easyplay_enhancements",0).edit().remove(KEY).commit())path.setText("全部日志导出文件夹："+destination(a));
                    else toast(a,"文件夹设置保存失败");
                }}).create();
            dialog.setOnShowListener(new DialogInterface.OnShowListener(){public void onShow(DialogInterface d){
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener(){public void onClick(View v){
                    // Canonical path validation can touch storage; run it on a worker.
                    final String entered=input.getText().toString();final Button save=dialog.getButton(AlertDialog.BUTTON_POSITIVE);save.setEnabled(false);
                    final Context app=a.getApplicationContext();
                    Thread t=new Thread(new Runnable(){public void run(){String result=null,error=null;
                        try{result=AllLogsFiles.folder(entered).getAbsolutePath();
                            if(!app.getSharedPreferences("easyplay_enhancements",0).edit().putString(KEY,result).commit())throw new IOException("设置保存失败");}
                        catch(Exception failure){error=failure.getMessage();}
                        final String folder=result,message=error;
                        new Handler(Looper.getMainLooper()).post(new Runnable(){public void run(){
                            if(a.isFinishing()||a.isDestroyed())return;save.setEnabled(true);
                            if(message!=null){input.setError(message);return;}path.setText("全部日志导出文件夹："+folder);dialog.dismiss();toast(a,"已保存文件夹；导出时检查写入权限");
                        }});
                    }},"carconnect-export-folder");t.setDaemon(true);t.start();
                }});
            }});dialog.show();
        }});
        Button export=new Button(a);export.setText("一键导出全部日志到指定文件夹");panel.addView(export);
        export.setOnClickListener(new View.OnClickListener(){public void onClick(View v){export(a);}});
    }
    private static void toast(Activity a,String text){if(a!=null&&!a.isFinishing()&&!a.isDestroyed())Toast.makeText(a,text,Toast.LENGTH_LONG).show();}
    private static void section(StringBuilder out,String title,String body){out.append("\n========== ").append(title).append(" ==========\n").append(AllLogsFiles.redact(body)).append('\n');}
    private static String known(Context c,String name){try{return AllLogsFiles.readKnown(c.getFilesDir(),name);}catch(Exception error){return "该记录读取失败："+error;}}
    private static void export(final Activity a){
        if(!busy.compareAndSet(false,true)){toast(a,"正在导出全部日志，请等待完成");return;}
        final Context app=a.getApplicationContext();final WeakReference<Activity> ref=new WeakReference<Activity>(a);
        final String folder=destination(a);final long at=System.currentTimeMillis();
        final StringBuilder ui=new StringBuilder();
        try{
            LegacySessionService service=StandbyRecovery.currentService();
            section(ui,"当前连接流程 / 系统与方向盘媒体状态",service==null?"当前 Service 未运行；下方附最近持久化会话记录":service.report());
        }catch(Exception error){section(ui,"连接状态读取异常",error.toString());}
        try{section(ui,"音频",AudioRuntime.summary());}catch(Exception error){section(ui,"音频读取异常",error.toString());}
        try{section(ui,"视频 / 首帧",VideoGuard.summary());}catch(Exception error){section(ui,"视频读取异常",error.toString());}
        try{section(ui,"系统栏",FullscreenRecovery.summary(a));}catch(Exception error){section(ui,"系统栏读取异常",error.toString());}
        final String snapshot=ui.toString();toast(a,"正在汇总并写入指定文件夹，无需分享应用");
        Thread worker=new Thread(new Runnable(){public void run(){
            File result=null;String failure=null;
            try{
                StringBuilder all=new StringBuilder("CarConnect 0.1.16 全部应用日志\n生成时间戳："+at+"\n");
                section(all,"记录范围","包含当前可取得的本应用诊断和有界历史：连接流程、蓝牙/SPP、休眠、异常、音频、视频、系统栏及原有媒体/方向盘诊断。\n"
                    +"历史仅限已经保留的记录；原车 gocsdk 内部日志、其他进程及系统日志普通 APK 无权读取，未包含。未读取配对密钥、证书和偏好全量数据。");
                all.append(snapshot);
                section(all,"最近持久化会话",known(app,"legacy-session-status.txt"));
                section(all,"原有车机自检",known(app,"legacy-device-diagnostics.txt"));
                try{section(all,"蓝牙 / SPP / 休眠 / 错误与历史",OemTrace.summary(app));}
                catch(Exception error){section(all,"蓝牙历史读取失败",error.toString());}
                result=AllLogsFiles.save(all.toString(),AllLogsFiles.folder(folder),at);
                OemTrace.event("ALL_LOGS saved bytes="+result.length()+" path="+result.getAbsolutePath());
            }catch(Exception error){failure=error.getMessage();OemTrace.error("ALL_LOGS export",error);}
            finally{busy.set(false);}
            final File ready=result;final String error=failure;
            new Handler(Looper.getMainLooper()).post(new Runnable(){public void run(){
                Activity current=ref.get();if(current==null||current.isFinishing()||current.isDestroyed())return;
                TextView view=new TextView(current);view.setPadding(16,16,16,16);view.setTextIsSelectable(true);
                view.setText(ready==null?"导出失败："+error+"\n指定文件夹："+folder+"\n请使用可写的外部存储文件夹，再点击导出。":
                    "已保存全部应用日志：\n"+ready.getAbsolutePath()+"\n文件大小："+ready.length()+" 字节\n\n可用车机文件管理器取走 TXT。没有分享应用也可直接保存。旧导出文件不会被自动删除。");
                new AlertDialog.Builder(current).setTitle("全部日志导出结果").setView(view).setPositiveButton("完成",null).show();
            }});
        }},"carconnect-all-logs-export");worker.setDaemon(true);worker.start();
    }
}
