# Fingerprint Volume Gesture — Galaxy M33 / One UI

This Android app maps:
- Side fingerprint sensor swipe **up** → media volume up
- Side fingerprint sensor swipe **down** → media volume down
- Screen-edge strip swipe up/down → fallback volume controls

## Important device limitation

The code uses Android's public `FingerprintGestureController` accessibility API. It requires a device whose fingerprint sensor **and firmware expose swipe events to third-party accessibility services**. Samsung's built-in **Finger sensor gestures** option can open/close the notification panel on supported Galaxy devices, but that does not prove third-party apps receive those same callbacks.

This build explicitly requests `FLAG_REQUEST_FINGERPRINT_GESTURES` both in the service XML and at runtime, and declares both `USE_FINGERPRINT` and `USE_BIOMETRIC`. The app now displays the framework's availability response and any actual swipe callback in the app's diagnostic area. If Android says gestures are unavailable or never reports a swipe, One UI's firmware may not expose the sensor events to third-party apps; ordinary app code cannot reliably override that firmware limitation.

## Install and test

1. Install the debug APK.
2. Open the app and enable its Accessibility service in Settings.
3. Return to the app and wait a few seconds.
4. Read **Fingerprint sensor diagnosis** and tap **Refresh fingerprint diagnosis**.
5. If available, swipe up/down on the side fingerprint sensor and check whether the status changes to a received gesture.
6. If your phone provides **Settings → Advanced features → Motions and gestures → Finger sensor gestures**, you can enable it to check Samsung's built-in function. This setting is separate from third-party callback support.
7. For fallback testing, grant **Appear on top** and tap **Start edge gesture strip**. Swipe on the visible strip.

The app has no internet permission, analytics, ads, or data collection. The accessibility service does not retrieve window content.

## Build

The included GitHub Actions workflow builds a debug APK using JDK 17 and Gradle 8.9. In GitHub, open **Actions → Build debug APK**, select the latest successful run, then download the `FingerprintVolumeGesture-debug-apk` artifact.
