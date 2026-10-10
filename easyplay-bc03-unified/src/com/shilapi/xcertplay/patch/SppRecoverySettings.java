package com.shilapi.xcertplay.patch;
import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
public final class SppRecoverySettings {
    public static void add(final Activity activity,LinearLayout panel){
        final CheckBox clear=new CheckBox(activity);clear.setTextColor(Color.WHITE);
        clear.setText("连接前清理原车 SPP（失败后暂停重试）");clear.setChecked(SppCleanupPrefs.enabled(activity));panel.addView(clear);
        clear.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            public void onCheckedChanged(CompoundButton b,boolean value){SppCleanupPrefs.save(activity,value);}
        });
        final CheckBox nativeRecovery=new CheckBox(activity);nativeRecovery.setTextColor(Color.WHITE);
        nativeRecovery.setText("允许底层 SPP 恢复（实验，默认关闭）");
        nativeRecovery.setChecked(SppRecoveryPrefs.nativeEnabled(activity));panel.addView(nativeRecovery);
        nativeRecovery.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            private boolean updating;
            public void onCheckedChanged(CompoundButton b,boolean value){
                if(updating)return;
                if(!SppRecoveryPrefs.saveNative(activity,value)){
                    updating=true;nativeRecovery.setChecked(!value);updating=false;
                    Toast.makeText(activity,"设置保存失败，未更改底层恢复选项",Toast.LENGTH_LONG).show();return;
                }
                Toast.makeText(activity,value?"已允许一次底层恢复；会断开模块管理的 SPP 投屏连接":"已关闭底层恢复；普通空闲连接不受此选项影响",Toast.LENGTH_LONG).show();
            }
        });
        TextView hint=new TextView(activity);hint.setTextColor(Color.WHITE);hint.setTextSize(14);
        hint.setText("空闲时直接连接，不清理。原接口请求未释放则暂停本轮自动重试；未完成标记保留到通道确认空闲。底层恢复默认关闭，不沿用旧版清理开关自动发送 VH。不删除手机记录、不重启模块。");panel.addView(hint);
        Button retry=new Button(activity);retry.setText("允许再次清理一次");panel.addView(retry);
        retry.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
            try{
                OemTransport.rearmCleanup(activity);
                Toast.makeText(activity,"已解除清理暂停；请再开启自动连接。仍遵守清理冷却和占用校验",Toast.LENGTH_LONG).show();
            }catch(Exception error){Toast.makeText(activity,error.getMessage(),Toast.LENGTH_LONG).show();}
        }});
    }
}
