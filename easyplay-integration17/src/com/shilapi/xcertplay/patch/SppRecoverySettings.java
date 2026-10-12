package com.shilapi.xcertplay.patch;
import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
public final class SppRecoverySettings {
    public static void add(final Activity activity,LinearLayout panel){
        ConnectionDiagnostics.add(activity,panel);
        StandbyRecovery.add(activity,panel);
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
                Toast.makeText(activity,value?"已启用实验底层恢复选项；已暂停时请点击“允许一次恢复并连接”。恢复会断开模块管理的 SPP 投屏连接":"已关闭底层恢复；普通空闲连接不受此选项影响",Toast.LENGTH_LONG).show();
            }
        });
        TextView hint=new TextView(activity);hint.setTextColor(Color.WHITE);hint.setTextSize(14);
        hint.setText("空闲时直接连接。覆盖安装会保留上次未完成的恢复标记；启用底层选项本身不解除暂停。下面的按钮只允许当前操作恢复一次：结束旧任务、核对手机、请求释放并确认空闲后重新连接。底层 VH 按实验开关执行，可能断开其他 SPP 投屏；失败后暂停，不删除手机记录。\n"+SppRecoveryPrefs.evidence(activity));panel.addView(hint);
        Button retry=new Button(activity);retry.setText("允许一次恢复并连接");panel.addView(retry);
        retry.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
            ManualRecovery.start(activity);
        }});
    }
}
