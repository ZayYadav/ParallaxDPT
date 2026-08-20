package com.parallax.shell;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Debug;
import android.os.Process;
import android.provider.Settings;
import android.text.TextUtils;

import com.parallax.parallax.BuildConfig;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.WeakHashMap;

public final class ParallaxBhaiya {
    private static final WeakHashMap<Activity, AlertDialog> DIALOGS = new WeakHashMap<>();
    private static volatile boolean legacyInstalled;

    private ParallaxBhaiya() {}

    static boolean shouldBlockDeveloperMode(Context context) {
        return isDeveloperOptionsEnabled(context) && !isDeviceAuthorized(context);
    }

    static boolean isDeveloperOptionsEnabled(Context context) {
        if (context == null) return false;
        try {
            return Settings.Global.getInt(
                    context.getContentResolver(),
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                    0) == 1;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean isDebuggerAttached() {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger();
    }

    static boolean isDeviceAuthorized(Context context) {
        String configured = BuildConfig.AUTHORIZED_DEVICE_HASHES;
        if (TextUtils.isEmpty(configured)) return false;
        String current = getDeviceAuthorizationHash(context);
        if (TextUtils.isEmpty(current)) return false;
        for (String candidate : configured.split(",")) {
            if (current.equalsIgnoreCase(candidate.trim())) return true;
        }
        return false;
    }

    static String getDeviceAuthorizationHash(Context context) {
        if (context == null) return "";
        try {
            String id = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            if (TextUtils.isEmpty(id)) return "";
            MessageDigest digest = MessageDigest.getInstance(
                    ParallaxHuYaarBhai.z(127300711, 18, 97, 153, 90, 221, 7, 148));
            byte[] bytes = digest.digest(id.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) out.append(String.format(Locale.US, "%02x", b & 0xff));
            return out.toString();
        } catch (Throwable ignored) {
            return "";
        }
    }

    public static native boolean nativeGate();

    static boolean nativeSecurityPassed() {
        try {
            return nativeGate() && !isDebuggerAttached();
        } catch (Throwable ignored) {
            return false;
        }
    }

    static void installLegacyActivityBlocker(Application application) {
        if (application == null || legacyInstalled) return;
        synchronized (ParallaxBhaiya.class) {
            if (legacyInstalled) return;
            legacyInstalled = true;
        }
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity activity, android.os.Bundle state) { showDeveloperDialog(activity); }
            @Override public void onActivityStarted(Activity activity) { showDeveloperDialog(activity); }
            @Override public void onActivityResumed(Activity activity) { showDeveloperDialog(activity); }
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, android.os.Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }

    static void showDeveloperDialog(final Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        activity.runOnUiThread(new Runnable() {
            @Override public void run() {
                if (activity.isFinishing()) return;
                synchronized (DIALOGS) {
                    AlertDialog existing = DIALOGS.get(activity);
                    if (existing != null && existing.isShowing()) return;

                    String title = ParallaxHuYaarBhai.z(1599219909, 123, 14, 184, 102, 54, 161, 23, 93, 146, 235, 63, 97, 40, 21, 110, 197, 227, 243, 66, 91, 206, 70, 2, 36);
                    String detail = ParallaxHuYaarBhai.z(29645620, 22, 174, 212, 189, 166, 30, 62, 191, 22, 146, 182, 44, 16, 148, 80, 67, 171, 153, 174, 45, 183, 69, 9, 73, 234, 37, 176, 174, 130, 126, 187, 106, 228, 178, 129, 248, 208, 232, 165, 48, 175, 53, 48, 48, 45, 203, 236, 180, 141, 133, 228, 62, 116, 30, 225, 28, 214, 150, 233, 103, 7, 185, 55, 188, 208, 73, 53, 112, 174, 15, 129, 52, 24, 233, 88, 212, 39, 127, 2);
                    String hashLabel = ParallaxHuYaarBhai.z(2061953758, 241, 103, 185, 26, 171, 242, 173, 110, 166, 214, 128, 25, 175, 98, 194, 202, 169, 161, 28, 80, 233, 221, 71, 140, 75, 228, 239);
                    String close = ParallaxHuYaarBhai.z(131935658, 123, 184, 128, 64, 141, 206, 60, 45, 233);
                    String hash = getDeviceAuthorizationHash(activity);

                    AlertDialog dialog = new AlertDialog.Builder(activity)
                            .setTitle(title)
                            .setMessage(detail + "\n\n" + hashLabel + hash)
                            .setCancelable(false)
                            .setPositiveButton(close, new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface dialog, int which) {
                                    terminate(activity);
                                }
                            })
                            .create();
                    dialog.setCanceledOnTouchOutside(false);
                    DIALOGS.put(activity, dialog);
                    dialog.show();
                }
            }
        });
    }

    private static void terminate(Activity activity) {
        try {
            activity.finishAndRemoveTask();
        } catch (Throwable ignored) {
            activity.finish();
        }
        Process.killProcess(Process.myPid());
        System.exit(0);
    }
}
