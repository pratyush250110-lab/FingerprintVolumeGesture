package com.pra.fingerprintvolume;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.SharedPreferences;

public class MainActivity extends Activity {
    private static final String PREFS = "fingerprint_gesture_status";
    private TextView status;
    private TextView fingerprintStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(26), dp(22), dp(22));
        root.setBackgroundColor(Color.rgb(14, 18, 30));

        TextView title = new TextView(this);
        title.setText("Fingerprint Volume Gesture");
        title.setTextColor(Color.WHITE);
        title.setTextSize(25);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView intro = new TextView(this);
        intro.setText("\nGalaxy M33 5G • One UI diagnostic build\n\nTarget: swipe UP on the side fingerprint sensor = volume up; swipe DOWN = volume down. Android exposes this only when the phone's firmware sends sensor gestures to third-party accessibility services. Samsung's native notification-panel gesture alone does not guarantee third-party access.");
        intro.setTextColor(Color.rgb(210, 218, 235));
        intro.setTextSize(15);
        root.addView(intro);

        status = new TextView(this);
        status.setTextColor(Color.rgb(151, 225, 177));
        status.setTextSize(14);
        root.addView(status);

        TextView diagnosticTitle = new TextView(this);
        diagnosticTitle.setText("\nFingerprint sensor diagnosis");
        diagnosticTitle.setTextColor(Color.WHITE);
        diagnosticTitle.setTextSize(18);
        diagnosticTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(diagnosticTitle);

        fingerprintStatus = new TextView(this);
        fingerprintStatus.setTextColor(Color.rgb(255, 209, 130));
        fingerprintStatus.setTextSize(14);
        root.addView(fingerprintStatus);

        Button refresh = button("Refresh fingerprint diagnosis");
        refresh.setOnClickListener(v -> refreshStatus());
        root.addView(refresh);

        Button accessibility = button("1. Open Accessibility Settings");
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(accessibility);

        Button overlay = button("2. Allow display over other apps");
        overlay.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        root.addView(overlay);

        Button start = button("Start edge gesture strip");
        start.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                status.setText("Allow display over other apps first.");
                return;
            }
            if (!EdgeGestureService.isEnabled(this)) {
                status.setText("Enable the accessibility service first, then return here.");
                return;
            }
            startService(new Intent(this, EdgeGestureService.class).setAction(EdgeGestureService.ACTION_START));
            status.setText("Requested start. Swipe on the right-edge strip to test the fallback.");
        });
        root.addView(start);

        Button stop = button("Stop edge gesture strip");
        stop.setOnClickListener(v -> {
            startService(new Intent(this, EdgeGestureService.class).setAction(EdgeGestureService.ACTION_STOP));
            status.setText("Requested stop.");
        });
        root.addView(stop);

        TextView footer = new TextView(this);
        footer.setText("\nTip: if your Settings app has Advanced features → Motions and gestures → Finger sensor gestures, enable it to test Samsung's built-in sensor gesture. This app still depends on Android delivering callbacks to third-party services.\n\nNo internet permission, analytics or data collection.");
        footer.setTextColor(Color.rgb(155, 164, 184));
        footer.setTextSize(13);
        root.addView(footer);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        if (status == null || fingerprintStatus == null) return;
        boolean enabled = EdgeGestureService.isEnabled(this);
        status.setText(enabled
                ? "Accessibility service: enabled (confirm it stays on in Settings)."
                : "Accessibility service: not enabled yet.");
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String saved = prefs.getString("status", null);
        boolean connected = prefs.getBoolean("service_connected", false);
        if (!enabled) {
            fingerprintStatus.setText("Enable the service first, then return here. The diagnosis will appear after Android connects the service.");
        } else if (saved == null) {
            fingerprintStatus.setText(connected
                    ? "Service connected, but no fingerprint status was recorded yet. Tap refresh again."
                    : "Waiting for Android to connect the accessibility service. Re-open this screen after enabling it.");
        } else {
            fingerprintStatus.setText(saved);
        }
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(15);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        b.setLayoutParams(lp);
        return b;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
