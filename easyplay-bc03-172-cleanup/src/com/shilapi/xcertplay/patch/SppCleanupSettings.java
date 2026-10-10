package com.shilapi.xcertplay.patch;
import android.app.Activity;
import android.graphics.Color;
import android.widget.*;
public final class SppCleanupSettings {
    public static void add(final Activity activity,LinearLayout panel) {
        CheckBox toggle=new CheckBox(activity);toggle.setTextColor(Color.WHITE);
        toggle.setText("连接前自动清理原车 SPP（含底层恢复）");
        toggle.setChecked(SppCleanupPrefs.enabled(activity));panel.addView(toggle);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            public void onCheckedChanged(CompoundButton button,boolean checked){
                SppCleanupPrefs.save(activity,checked);
                if(checked)OemTransport.startup(activity);
                Toast.makeText(activity,checked?"已开启 SPP 自动清理；会中断原车当前 SPP 投屏连接":"已关闭 SPP 自动清理",Toast.LENGTH_LONG).show();
            }
        });
        TextView hint=new TextView(activity);hint.setTextColor(Color.WHITE);hint.setTextSize(14);
        hint.setText("默认关闭并保存。仅在使用原车蓝牙且服务已核对时生效。开启会断开本模块管理的其他 SPP 投屏；原服务未释放时会发送一次底层清理。启动只检查状态，不消耗清理冷却。不重启模块、不删除配对。正在使用的 CarConnect 通道会跳过。状态不释放仍停止连接。");
        panel.addView(hint);
    }
}
