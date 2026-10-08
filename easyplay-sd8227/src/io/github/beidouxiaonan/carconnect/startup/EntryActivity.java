package io.github.beidouxiaonan.carconnect.startup;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.*;

/** A software rendered launcher that remains usable without secondary DEX. */
public final class EntryActivity extends Activity {
    private TextView report;
    private boolean launching;
    @Override protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setTitle("CarConnect · SD8227 启动检查");
        ScrollView scroll = new ScrollView(this);
        LinearLayout column = new LinearLayout(this); column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(24, 16, 24, 16); scroll.addView(column);
        TextView title = new TextView(this); title.setTextSize(22);
        title.setText("CarConnect 0.1.7 · 旧系统 DEX 加载兼容测试"); column.addView(title);
        TextView instructions = new TextView(this); instructions.setTextSize(16);
        instructions.setText("请先点击启动，确认主页面能打开。失败后重新打开会停在这里，请复制最新启动记录反馈。此入口不会连接 USB 或蓝牙。");
        column.addView(instructions);
        Button start = new Button(this); start.setText("启动 CarConnect"); column.addView(start);
        start.setEnabled(StartupApplication.multidexReady);
        start.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { launch(); } });
        Button copy = new Button(this); copy.setText("复制启动记录"); column.addView(copy);
        copy.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (manager != null) { manager.setPrimaryClip(ClipData.newPlainText("CarConnect startup", StartupLog.report(EntryActivity.this)));
                Toast.makeText(EntryActivity.this, "已复制；记录仅保存在本机", Toast.LENGTH_SHORT).show(); }
        } });
        Button refresh = new Button(this); refresh.setText("刷新记录"); column.addView(refresh);
        refresh.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { update(); } });
        report = new TextView(this); report.setTextSize(14); report.setTextIsSelectable(true); column.addView(report);
        setContentView(scroll); StartupLog.stage(this, "ENTRY_VISIBLE"); update();
        if (StartupState.shouldAutoStart(StartupLog.state(this), getIntent().getBooleanExtra("diagnostics", false), StartupApplication.multidexReady)) {
            new Handler().postDelayed(new Runnable() { public void run() { if (!isFinishing()) launch(); } }, 400);
        }
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent); launching = false; update();
    }
    @Override protected void onResume() { super.onResume(); launching = false; update(); }
    private void update() { if (report != null) report.setText("状态：" + StartupLog.state(this) + "\n" + StartupLog.report(this)); }
    private void launch() {
        if (launching || !StartupApplication.multidexReady) return;
        launching = true; StartupLog.state(this, "launching"); StartupLog.stage(this, "MAIN_LAUNCH_REQUEST");
        try {
            // A component string avoids resolving LegacyActivity until the user chooses to enter it.
            startActivity(new Intent().setClassName(this, "com.shilapi.xcertplay.legacy.LegacyActivity"));
        } catch (Throwable error) { launching = false; StartupLog.failure(this, "START_ACTIVITY", error); update(); }
    }
}
