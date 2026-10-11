# Fingerprint Volume Gesture (Samsung Galaxy M33 5G)

A small Android app that maps:
- Swipe **up** on the screen-edge gesture strip → volume up
- Swipe **down** on the screen-edge gesture strip → volume down

## Important hardware limitation

Android has a public `FingerprintGestureController` API for accessibility services, but it only works on devices whose fingerprint sensor and firmware explicitly support swipe gestures. The project registers for that API first: swipe up → volume up, swipe down → volume down. The Galaxy M33 5G's side-mounted fingerprint reader may not expose gesture support, and this cannot be guaranteed from the model name alone. If the sensor never sends callbacks, use the included screen-edge strip fallback (swipe up/down on the strip).

## Build the APK

1. Install Android Studio (or use a compatible Gradle/Android SDK environment).
2. Open this folder as an existing project.
3. Let Gradle sync.
4. Choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. The debug APK is normally at `app/build/outputs/apk/debug/app-debug.apk`.

Recommended: use JDK 17 and Android SDK Platform 35. The project uses Android Gradle Plugin 8.7.3 and Gradle 8.9.

## Install and enable

1. Install the APK on your phone.
2. Open **Fingerprint Volume Gesture**.
3. Tap **Open Accessibility Settings**.
4. Enable **Fingerprint Volume Gesture** under installed/downloaded apps. Android will show a system warning because accessibility services can observe screen interactions; enable it only if you trust the app/source.
5. Return to the app and tap **Start edge gesture strip**.
6. A narrow translucent strip appears at the right edge. Swipe up/down within that strip to change media volume.
7. Tap **Stop edge gesture strip** to remove it.

## Privacy

- No internet permission.
- No analytics, ads, account, or data collection.
- Accessibility is used only for the edge overlay/gesture fallback.
- Source is included so you can inspect it before building.

## GitHub

Upload the contents of this folder to a repository. Do not commit signing keys or passwords. GitHub Actions workflow is included to build a debug APK and attach it to workflow artifacts.
