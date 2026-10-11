package com.pra.fingerprintvolume;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.FingerprintGestureController;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

public class EdgeGestureService extends AccessibilityService {
    public static final String ACTION_START = "com.pra.fingerprintvolume.START";
    public static final String ACTION_STOP = "com.pra.fingerprintvolume.STOP";
    private static final String TAG = "FingerprintVolume";
    private static final String PREFS = "fingerprint_gesture_status";

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

    private void saveFingerprintStatus(String status) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("status", status)
                .putLong("updated_at", System.currentTimeMillis())
                .apply();
        Log.i(TAG, status);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean("service_connected", true).apply();

        // Explicitly request fingerprint gesture events at runtime as well as in XML.
        // This helps diagnose OEMs that do not apply the XML flag consistently.
        try {
            AccessibilityServiceInfo serviceInfo = getServiceInfo();
            if (serviceInfo != null) {
                serviceInfo.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES;
                setServiceInfo(serviceInfo);
            }
        } catch (RuntimeException e) {
            saveFingerprintStatus("Accessibility service connected, but requesting the fingerprint gesture flag failed: "
                    + e.getClass().getSimpleName());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                fingerprintController = getFingerprintGestureController();
                fingerprintCallback = new FingerprintGestureController.FingerprintGestureCallback() {
                    @Override
                    public void onGestureDetected(int gesture) {
                        if (gesture == FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_UP) {
                            saveFingerprintStatus("Fingerprint swipe UP received by Android. Raising media volume.");
                            adjustVolume(AudioManager.ADJUST_RAISE);
                        } else if (gesture == FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_DOWN) {
                            saveFingerprintStatus("Fingerprint swipe DOWN received by Android. Lowering media volume.");
                            adjustVolume(AudioManager.ADJUST_LOWER);
                        } else {
                            saveFingerprintStatus("Fingerprint gesture received (direction " + gesture
                                    + "). Only up/down are mapped to volume.");
                        }
                    }

                    @Override
                    public void onGestureDetectionAvailabilityChanged(boolean available) {
                        if (available) {
                            saveFingerprintStatus("Android reports fingerprint gesture detection is AVAILABLE. Try swiping up/down on the side sensor.");
                        } else {
                            saveFingerprintStatus("Android reports fingerprint gesture detection is UNAVAILABLE. The sensor may be busy or One UI may not expose sensor swipes to third-party services.");
                        }
                    }
                };
                fingerprintController.registerFingerprintGestureCallback(fingerprintCallback, null);

                boolean available = fingerprintController.isGestureDetectionAvailable();
                if (available) {
                    saveFingerprintStatus("Listener registered; Android currently reports fingerprint gesture detection AVAILABLE. Try swiping on the side sensor.");
                } else {
                    saveFingerprintStatus("Listener registered, but Android reports fingerprint gesture detection UNAVAILABLE. Samsung's built-in sensor gesture can work even when third-party callbacks are not exposed.");
                }
            } catch (SecurityException e) {
                fingerprintController = null;
                saveFingerprintStatus("Fingerprint gesture access was denied by Android: " + e.getMessage());
            } catch (RuntimeException e) {
                fingerprintController = null;
                saveFingerprintStatus("Fingerprint gesture API could not start: " + e.getClass().getSimpleName());
            }
        } else {
            saveFingerprintStatus("Fingerprint gestures require Android 8.0 or newer.");
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
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean("service_connected", false).apply();
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
