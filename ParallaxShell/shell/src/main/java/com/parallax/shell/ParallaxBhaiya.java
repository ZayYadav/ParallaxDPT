package com.parallax.shell;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Debug;
import android.os.Process;
import android.provider.Settings;
import android.text.TextUtils;

import com.parallax.parallax.BuildConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.WeakHashMap;

/** Central Java/native runtime protection coordinator. */
public final class ParallaxBhaiya {
    private static final WeakHashMap<Activity, AlertDialog> DIALOGS = new WeakHashMap<>();
    private static volatile boolean legacyInstalled;
    private static volatile boolean rootLegacyInstalled;

    static final String PROTECTION_TITLE = z(2026082101, 197, 132, 244, 105, 129, 217, 129, 134, 246, 5, 253, 4, 156, 149, 102, 162, 208, 104, 48);
    static final String DEV_MESSAGE = z(2026082102, 209, 146, 211, 102, 126, 241, 156, 94, 61, 118, 23, 104, 120, 76, 88, 26, 229, 126, 236, 219, 113, 121, 208, 190, 38, 16, 39, 67, 152, 203, 11, 94, 83, 249, 237, 186, 74, 199, 251, 103, 243, 161, 66, 14, 70, 141, 229, 128, 67, 57, 196, 36, 141, 39, 167, 220, 235, 123, 10, 48, 206, 13, 24, 136, 9, 108, 217, 15, 85, 30, 9, 159, 93, 162, 229, 239, 245, 97, 95, 21, 71, 35, 178, 117, 185, 21, 246, 84, 140, 208, 205, 122, 250, 84, 243, 116, 122, 48, 35, 208, 82, 74, 91, 76, 154, 247);
    static final String HASH_LABEL = z(2026082103, 209, 144, 229, 201, 118, 168, 128, 15, 205, 124, 165, 114, 103, 118, 66, 205, 67, 209, 34, 104, 127, 194, 48, 65, 196, 226, 207);
    static final String CLOSE_LABEL = z(2026082104, 212, 157, 242, 107, 116, 157, 14, 220, 140);
    static final String VIRTUAL_MESSAGE = z(2026082105, 199, 146, 247, 124, 99, 17, 20, 217, 178, 59, 210, 124, 188, 246, 203, 161, 126, 108, 131, 73, 22, 39, 206, 152, 41, 112, 248, 206, 131, 107, 113, 141, 235, 218, 239, 41, 50, 17, 94, 116, 233, 56, 132, 31, 126, 189, 201, 41, 113, 171, 249, 128, 244, 3, 138, 223, 11, 133, 48, 23, 157, 177, 211, 90, 249, 253, 227, 121, 52, 210, 246, 241, 244, 60, 67, 81, 44, 143, 218, 157, 204, 76, 12, 232, 219, 87, 161, 145, 247, 235, 237, 116, 232, 186, 163, 59, 86, 12, 65, 148, 75, 115, 202, 200, 67, 2, 209, 169, 32, 167, 15, 202, 9, 131, 60, 150, 11, 32, 133, 107, 218, 253, 22, 48);
    static final String RUNTIME_MESSAGE = z(2026082106, 211, 183, 252, 37, 122, 83, 239, 155, 108, 21, 89, 136, 99, 209, 205, 19, 214, 203, 119, 7, 120, 142, 97, 115, 112, 160, 158, 7, 240, 248, 230, 26, 74, 210, 124, 78, 3, 66, 204, 169, 227, 177, 251, 21, 62, 16, 9, 144, 73, 253, 31, 148, 188, 103, 184, 198, 57, 132, 34, 7, 20, 153, 69, 67, 131, 253, 35, 105, 191, 137, 159, 187, 253, 3, 77, 190, 233, 85, 135, 105, 90, 119, 2, 149, 92, 165, 2, 28, 129, 141, 104, 18, 0, 18, 75, 176, 44, 105, 182, 44, 27, 76, 28, 174, 126, 81, 103, 43, 210, 0, 106, 137, 113, 180, 191, 190, 12, 246, 175, 233, 156, 187, 225, 170, 81, 164, 111, 58, 128, 34, 120, 15, 112, 55, 122, 73, 105, 80);
    static final String NATIVE_MESSAGE = z(2026082107, 215, 175, 237, 56, 100, 173, 26, 208, 204, 161, 207, 135, 147, 19, 211, 135, 150, 5, 94, 60, 221, 236, 230, 223, 139, 209, 221, 174, 160, 18, 36, 129, 103, 253, 141, 249, 112, 3, 27, 107, 150, 113, 46, 255, 195, 118, 250, 181, 216, 50, 92, 186, 72, 123, 130, 239, 216, 238, 149, 26, 122, 87, 237, 134, 77, 175, 182, 193, 236, 175, 94, 49, 154, 77, 23, 156, 157, 197, 170, 158, 194, 86, 196, 49, 206, 32, 188);
    static final String ROOT_MESSAGE = z(2026082108, 223, 165, 231, 53, 109, 164, 234, 140, 58, 159, 238, 110, 201, 22, 49, 161, 45, 59, 44, 20, 69, 242, 212, 153, 96, 179, 201, 159, 52, 17, 64, 56, 132, 13, 67, 79, 200, 11, 75, 237, 219, 120, 35, 247, 219, 138, 140, 163, 74, 97, 238, 254, 71, 46, 151, 242, 151, 48, 249, 161, 247, 45, 108, 32, 80, 197, 103, 66, 39, 141, 10, 131, 213, 15, 126, 243, 145, 221, 188, 34, 173, 91, 220, 182, 7, 111, 233, 104, 27, 147, 93, 133, 227, 21, 28, 103, 61, 45, 86, 72, 202, 9, 29, 69, 51, 114, 195, 111, 40, 189, 204, 247, 191, 239, 58, 78, 158, 246, 188);

    private static final String[] HOOK_CLASSES = {
            "de.robv.android.xposed.XposedBridge",
            "de.robv.android.xposed.XC_MethodHook",
            "org.lsposed.lspd.core.Main",
            "com.saurik.substrate.MS$2"
    };
    private static final String[] RUNTIME_MARKERS = {
            "frida", "gum-js-loop", "frida-gadget", "xposed", "lsposed",
            "edxp", "riru", "zygisk", "substrate", "sandhook", "linjector"
    };
    private static final String[] THREAD_MARKERS = {
            "frida", "gum-js-loop", "linjector", "xposed", "lsposed"
    };

    private ParallaxBhaiya() {}

    private static String z(int seed, int... encrypted) {
        return ParallaxHuYaarBhai.z(seed, encrypted);
    }

    static boolean shouldBlockDeveloperMode(Context context) {
        return isDeveloperOptionsEnabled(context) && !isDeviceAuthorized(context);
    }

    static boolean isDeveloperOptionsEnabled(Context context) {
        if (context == null) return false;
        try {
            return Settings.Global.getInt(context.getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1;
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
            String id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (TextUtils.isEmpty(id)) return "";
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(id.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) out.append(String.format(Locale.US, "%02x", b & 0xff));
            return out.toString();
        } catch (Throwable ignored) {
            return "";
        }
    }

    static void refreshJavaSecurityState(Context context) {
        if (context == null) return;
        if (isVirtualEnvironment(context)) Global.sVirtualBlocked = true;
        if (hasRuntimeInstrumentation()) Global.sRuntimeBlocked = true;
        Global.sDeveloperBlocked = !Global.sVirtualBlocked && !Global.sRuntimeBlocked && shouldBlockDeveloperMode(context);
    }

    static boolean refreshNativeSecurityState() {
        try {
            boolean runtimeOk = nativeGate();
            boolean virtualOk = nativeVirtualGate();
            if (!runtimeOk) Global.sRuntimeBlocked = true;
            if (!virtualOk) Global.sVirtualBlocked = true;
            return runtimeOk && virtualOk;
        } catch (Throwable ignored) {
            Global.sNativeBlocked = true;
            return false;
        }
    }

    static boolean nativeSecurityPassed() {
        return refreshNativeSecurityState();
    }

    static boolean anyProtectionBlocked() {
        return Global.sDeveloperBlocked || Global.sNativeBlocked || Global.sVirtualBlocked || Global.sRuntimeBlocked;
    }

    static String protectionMessage(Context context) {
        if (Global.sVirtualBlocked) return VIRTUAL_MESSAGE;
        if (Global.sRuntimeBlocked) return RUNTIME_MESSAGE;
        if (Global.sDeveloperBlocked) {
            return DEV_MESSAGE + "\n\n" + HASH_LABEL + getDeviceAuthorizationHash(context);
        }
        return NATIVE_MESSAGE;
    }

    private static boolean hasRuntimeInstrumentation() {
        if (isDebuggerAttached()) return true;
        ClassLoader loader = ParallaxBhaiya.class.getClassLoader();
        for (String className : HOOK_CLASSES) {
            try {
                Class.forName(className, false, loader);
                return true;
            } catch (Throwable ignored) {
            }
        }
        try {
            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                String name = element.getClassName().toLowerCase(Locale.US);
                if (containsAny(name, RUNTIME_MARKERS)) return true;
            }
        } catch (Throwable ignored) {
        }
        return procFileContains("/proc/self/maps", RUNTIME_MARKERS) || suspiciousThreadNames();
    }

    private static boolean suspiciousThreadNames() {
        File taskDir = new File("/proc/self/task");
        File[] tasks = taskDir.listFiles();
        if (tasks == null) return false;
        for (File task : tasks) {
            File comm = new File(task, "comm");
            try (BufferedReader reader = new BufferedReader(new FileReader(comm))) {
                String line = reader.readLine();
                if (line != null && containsAny(line.toLowerCase(Locale.US), THREAD_MARKERS)) return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private static boolean procFileContains(String path, String[] markers) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (containsAny(line.toLowerCase(Locale.US), markers)) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean isVirtualEnvironment(Context context) {
        if ("1".equals(systemProperty("ro.kernel.qemu")) || "1".equals(systemProperty("ro.boot.qemu"))) return true;

        String hardware = lower(Build.HARDWARE);
        String model = lower(Build.MODEL);
        String manufacturer = lower(Build.MANUFACTURER);
        String product = lower(Build.PRODUCT);
        String fingerprint = lower(Build.FINGERPRINT);
        String brand = lower(Build.BRAND);
        String device = lower(Build.DEVICE);

        if (containsAny(hardware, new String[]{"goldfish", "ranchu", "qemu", "vbox86"})) return true;
        if (containsAny(model, new String[]{"google_sdk", "emulator", "android sdk built for", "sdk_gphone", "genymotion"})) return true;
        if (containsAny(manufacturer, new String[]{"genymotion"})) return true;
        if (containsAny(product, new String[]{"sdk_gphone", "sdk_google", "sdk_x86", "vbox86", "emulator", "simulator"})) return true;
        if (containsAny(fingerprint, new String[]{"generic/sdk", "generic_x86", "sdk_gphone", "emulator"})) return true;
        if (brand.startsWith("generic") && device.startsWith("generic")) return true;

        String[] files = {"/dev/qemu_pipe", "/dev/socket/qemud", "/sys/qemu_trace", "/system/bin/qemu-props"};
        for (String path : files) {
            try {
                if (new File(path).exists()) return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private static String systemProperty(String key) {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method method = clazz.getDeclaredMethod("get", String.class, String.class);
            method.setAccessible(true);
            Object value = method.invoke(null, key, "");
            return value instanceof String ? (String) value : "";
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US);
    }

    private static boolean containsAny(String value, String[] markers) {
        if (value == null) return false;
        for (String marker : markers) {
            if (marker != null && value.contains(marker)) return true;
        }
        return false;
    }

    static void installLegacyActivityBlocker(final Application application) {
        if (application == null || legacyInstalled) return;
        synchronized (ParallaxBhaiya.class) {
            if (legacyInstalled) return;
            legacyInstalled = true;
        }
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            private void block(Activity activity) {
                if (activity == null || activity.isFinishing() || activity instanceof ParallaxKaBhaiJangu || activity instanceof ParallaxHuMaalik) return;
                if (!anyProtectionBlocked()) return;
                try {
                    activity.startActivity(new Intent(activity, ParallaxKaBhaiJangu.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                } catch (Throwable ignored) {
                    showProtectionDialog(activity, protectionMessage(activity));
                }
            }
            @Override public void onActivityCreated(Activity activity, android.os.Bundle state) { block(activity); }
            @Override public void onActivityStarted(Activity activity) { block(activity); }
            @Override public void onActivityResumed(Activity activity) { block(activity); }
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, android.os.Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }

    static void installRootActivityBlocker(final Application application) {
        if (application == null || rootLegacyInstalled) return;
        synchronized (ParallaxBhaiya.class) {
            if (rootLegacyInstalled) return;
            rootLegacyInstalled = true;
        }
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            private void block(Activity activity) {
                if (activity == null || activity.isFinishing() || activity instanceof ParallaxHuMaalik) return;
                try {
                    activity.startActivity(new Intent(activity, ParallaxHuMaalik.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                } catch (Throwable ignored) {
                    showRootDialog(activity);
                }
            }
            @Override public void onActivityCreated(Activity activity, android.os.Bundle state) { block(activity); }
            @Override public void onActivityStarted(Activity activity) { block(activity); }
            @Override public void onActivityResumed(Activity activity) { block(activity); }
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, android.os.Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }

    static void showDeveloperDialog(final Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        refreshJavaSecurityState(activity);
        if (!anyProtectionBlocked()) {
            activity.finish();
            return;
        }
        showProtectionDialog(activity, protectionMessage(activity));
    }

    static void showRootDialog(final Activity activity) {
        showProtectionDialog(activity, ROOT_MESSAGE);
    }

    private static void showProtectionDialog(final Activity activity, final String message) {
        if (activity == null || activity.isFinishing()) return;
        activity.runOnUiThread(new Runnable() {
            @Override public void run() {
                if (activity.isFinishing()) return;
                synchronized (DIALOGS) {
                    AlertDialog existing = DIALOGS.get(activity);
                    if (existing != null && existing.isShowing()) return;
                    AlertDialog dialog = new AlertDialog.Builder(activity)
                            .setTitle(PROTECTION_TITLE)
                            .setMessage(message)
                            .setCancelable(false)
                            .setPositiveButton(CLOSE_LABEL, new DialogInterface.OnClickListener() {
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

    public static native boolean nativeGate();
    public static native boolean nativeVirtualGate();
}
