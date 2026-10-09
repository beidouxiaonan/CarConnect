package com.shilapi.xcertplay.patch;
import android.app.Activity;
import android.graphics.Color;
import android.widget.*;
public final class FirstFrameSettings {
    public static void add(final Activity activity, LinearLayout panel) {
        CheckBox toggle = new CheckBox(activity);
        toggle.setTextColor(Color.WHITE);
        toggle.setText("无线无首帧时自动重连");
        toggle.setChecked(FirstFrameRecovery.enabled(activity));
        panel.addView(toggle);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                FirstFrameRecovery.save(activity, checked);
            }
        });
        TextView hint = new TextView(activity); hint.setTextColor(Color.WHITE); hint.setTextSize(14);
        hint.setText("默认开启并保存。原车蓝牙无线控制启动后，前台连续 60 秒无首帧才恢复，最多重连两次。后台不计时；首帧显示后停止。保留已选 iPhone，沿用原 SPP 清理和连接冷却。");
        panel.addView(hint);
    }
}
