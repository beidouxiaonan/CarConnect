package com.shilapi.xcertplay.patch;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class AvDiagnostics {
    public static void append(final Activity a,LinearLayout panel) {
        TextView heading=new TextView(a); heading.setText("音频与系统栏诊断"); heading.setTextSize(20); heading.setTextColor(Color.WHITE); panel.addView(heading);
        final TextView status=new TextView(a); status.setTextSize(14); status.setTextColor(Color.WHITE); panel.addView(status);
        status.setText(text(a));
        Button button=new Button(a); button.setText("刷新并复制音频 / 系统栏记录"); panel.addView(button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String value=text(a); status.setText(value);
                ClipboardManager clip=(ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE);
                if(clip!=null)clip.setPrimaryClip(ClipData.newPlainText("CarConnect AV",value));
                Toast.makeText(a,"诊断已复制",Toast.LENGTH_SHORT).show();
            }
        });
    }
    private static String text(Activity a) { return "CarConnect 0.1.5 AV 测试\n"+AudioRuntime.summary()+"\n"+FullscreenRecovery.summary(a); }
}
