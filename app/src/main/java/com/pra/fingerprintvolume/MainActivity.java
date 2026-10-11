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

public class MainActivity extends Activity {
    private TextView status;

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
        intro.setText("\nGalaxy M33 5G • prototype\n\nSwipe up in the right-edge strip = volume up. Swipe down = volume down.\n\nImportant: Android only exposes fingerprint swipes on devices whose sensor and firmware support them. If your M33 does not provide those callbacks, use the screen-edge strip; this app cannot guarantee fingerprint-button gestures.");
        intro.setTextColor(Color.rgb(210, 218, 235));
        intro.setTextSize(16);
        root.addView(intro);

        status = new TextView(this);
        status.setTextColor(Color.rgb(151, 225, 177));
        status.setTextSize(14);
        root.addView(status);

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
            status.setText("Requested start. A slim strip should appear at the right edge.");
        });
        root.addView(start);

        Button stop = button("Stop edge gesture strip");
        stop.setOnClickListener(v -> {
            startService(new Intent(this, EdgeGestureService.class).setAction(EdgeGestureService.ACTION_STOP));
            status.setText("Requested stop.");
        });
        root.addView(stop);

        TextView footer = new TextView(this);
        footer.setText("\nNo internet permission. No analytics. Source code is available in the GitHub project.");
        footer.setTextColor(Color.rgb(155, 164, 184));
        footer.setTextSize(13);
        root.addView(footer);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (status != null) {
            status.setText(EdgeGestureService.isEnabled(this)
                    ? "Accessibility service: enabled"
                    : "Accessibility service: not enabled yet");
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
