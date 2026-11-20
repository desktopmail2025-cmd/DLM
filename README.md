# DLM

DLM is packaged as an Android app using a native Android WebView.

## Build APK with GitHub Actions

1. Push changes to the `main` branch, or open **Actions → Build DLM Android APK → Run workflow**.
2. Wait for the **Build debug APK** workflow to finish.
3. Open the successful workflow run and download the **DLM-debug-apk** artifact.
4. Extract the ZIP file to get `app-debug.apk`, then install it on your Android device.

## Local build

Requires JDK 17, Android SDK (API 35), and Gradle 8.9.

```bash
gradle :app:assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

The web app is bundled at `app/src/main/assets/DLM.html`. The Android app uses AndroidX WebKit's asset loader so LocalStorage and IndexedDB use a stable HTTPS-like origin. The profile image picker is supported.
