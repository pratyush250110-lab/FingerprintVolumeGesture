package com.pra.fingerprintvolume;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.FingerprintGestureController;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

public class EdgeGestureService extends AccessibilityService {
    public static final String ACTION_START = "com.pra.fingerprintvolume.START";
    public static final String ACTION_STOP = "com.pra.fingerprintvolume.STOP";

    private WindowManager windowManager;
    private View strip;
    private float downY;
    private boolean tracking;
    private long lastActionAt;
    private FingerprintGestureController fingerprintController;
    private FingerprintGestureController.FingerprintGestureCallback fingerprintCallback;

    public static boolean isEnabled(Context context) {
        String enabled = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        String target = new android.content.ComponentName(context, EdgeGestureService.class).flattenToString();
        for (String part : enabled.split(":")) {
            if (part.equalsIgnoreCase(target)) return true;
        }
        return false;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                fingerprintController = getFingerprintGestureController();
                fingerprintCallback = new FingerprintGestureController.FingerprintGestureCallback() {
                    @Override
                    public void onGestureDetected(int gesture) {
                        if (gesture == FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_UP) {
                            adjustVolume(AudioManager.ADJUST_RAISE);
                        } else if (gesture == FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_DOWN) {
                            adjustVolume(AudioManager.ADJUST_LOWER);
                        }
                    }

                    @Override
                    public void onGestureDetectionAvailabilityChanged(boolean available) { }
                };
                fingerprintController.registerFingerprintGestureCallback(fingerprintCallback, null);
            } catch (RuntimeException ignored) {
                fingerprintController = null;
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_START.equals(intent.getAction())) showStrip();
        if (intent != null && ACTION_STOP.equals(intent.getAction())) removeStrip();
        return START_NOT_STICKY;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() { }

    @Override
    public void onDestroy() {
        removeStrip();
        if (fingerprintController != null && fingerprintCallback != null) {
            try {
                fingerprintController.unregisterFingerprintGestureCallback(fingerprintCallback);
            } catch (RuntimeException ignored) { }
        }
        super.onDestroy();
    }

    private void adjustVolume(int direction) {
        long now = System.currentTimeMillis();
        if (now - lastActionAt < 180) return;
        AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audio != null) {
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI);
            lastActionAt = now;
        }
    }

    private void showStrip() {
        if (strip != null || !Settings.canDrawOverlays(this)) return;
        if (windowManager == null) windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        TextView view = new TextView(this);
        view.setText("↕");
        view.setTextSize(20);
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER);
        view.setContentDescription("Swipe up for volume up; swipe down for volume down");
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xAA5267A8);
        bg.setCornerRadius(dp(18));
        view.setBackground(bg);
        view.setAlpha(0.78f);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                dp(38), dp(112), type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        params.x = dp(2);

        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downY = event.getRawY();
                    tracking = true;
                    return true;
                case MotionEvent.ACTION_UP:
                    if (tracking) {
                        float delta = event.getRawY() - downY;
                        if (Math.abs(delta) >= dp(24) && System.currentTimeMillis() - lastActionAt > 180) {
                            adjustVolume(delta < 0 ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER);
                        }
                    }
                    tracking = false;
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    tracking = false;
                    return true;
                default:
                    return true;
            }
        });

        try {
            windowManager.addView(view, params);
            strip = view;
        } catch (RuntimeException e) {
            strip = null;
        }
    }

    private void removeStrip() {
        if (strip != null && windowManager != null) {
            try { windowManager.removeView(strip); } catch (RuntimeException ignored) { }
            strip = null;
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
