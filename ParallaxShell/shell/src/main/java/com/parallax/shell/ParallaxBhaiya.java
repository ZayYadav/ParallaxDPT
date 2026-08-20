package com.parallax.shell;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Debug;
import android.provider.Settings;

import com.parallax.shell.util.EnvUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** Central Parallax device-policy and runtime protection gate. */
final class ParallaxBhaiya {
    static final int CLEAR = 0;
    static final int ROOT = 1;
    static final int DEVELOPER_UNAUTHORIZED = 2;
    static final int VIRTUAL = 3;
    static final int DEBUG_OR_TAMPER = 4;

    private static final int NATIVE_TRACER = 1;
    private static final int NATIVE_VIRTUAL = 2;

    /*
     * Put SHA-256(ANDROID_ID) values here to authorize selected phones while
     * Developer Options is enabled. Empty means no Developer Options device
     * is authorized.
     */
    private static final String[] AUTHORIZED_DEVICE_SHA256 = {};

    private ParallaxBhaiya() {
    }

    private static native int nativeEnvironmentState();

    static int evaluateEarly() {
        if (Global.sProtectionBlockReason != CLEAR) {
            return Global.sProtectionBlockReason;
        }
        if (ParallaxHuYaarBhai.isRootedDevice()) return block(ROOT);
        if (isVirtualEnvironment()) return block(VIRTUAL);
        if (isDebuggerOrDebuggable()) return block(DEBUG_OR_TAMPER);
        return CLEAR;
    }

    static int evaluate(Context context) {
        int reason = evaluateEarly();
        if (reason != CLEAR) return reason;
        if (context != null && isDeveloperOptionsEnabled(context) && !isAuthorizedDevice(context)) {
            return block(DEVELOPER_UNAUTHORIZED);
        }
        return CLEAR;
    }

    static int evaluateNativeAfterLoad() {
        if (Global.sProtectionBlockReason != CLEAR) {
            return Global.sProtectionBlockReason;
        }
        try {
            int flags = nativeEnvironmentState();
            if ((flags & NATIVE_VIRTUAL) != 0) return block(VIRTUAL);
            if ((flags & NATIVE_TRACER) != 0) return block(DEBUG_OR_TAMPER);
        } catch (Throwable ignored) {
        }
        return CLEAR;
    }

    static Activity newBlockedActivity(int reason) {
        if (reason == ROOT) return new ParallaxHuMaalik();
        if (reason == DEVELOPER_UNAUTHORIZED) return new ParallaxKaBhaiJangu();
        if (reason == VIRTUAL) return new ParallaxVirtualBhaiya();
        return new ParallaxBhaiKiSecurity();
    }

    static void installActivityBlocker(Application application) {
        ParallaxDialogBhaiya.installActivityBlocker(application);
    }

    static void showProtectionDialog(Activity activity, int reason) {
        ParallaxDialogBhaiya.show(activity, reason);
    }

    private static int block(int reason) {
        Global.sProtectionBlockReason = reason;
        Global.sRootBlocked = reason == ROOT;
        return reason;
    }

    private static boolean isDeveloperOptionsEnabled(Context context) {
        try {
            return Settings.Global.getInt(
                    context.getContentResolver(),
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                    0) == 1;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isAuthorizedDevice(Context context) {
        if (AUTHORIZED_DEVICE_SHA256.length == 0) return false;
        try {
            String id = Settings.Secure.getString(
                    context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (id == null || id.length() == 0) return false;

            MessageDigest digest = MessageDigest.getInstance(
                    ParallaxHuYaarBhai.z(1194276548, 130, 213, 36, 80, 154, 146, 197));
            byte[] hash = digest.digest(id.getBytes(StandardCharsets.UTF_8));
            char[] hex = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
            StringBuilder out = new StringBuilder(64);
            for (byte b : hash) {
                int v = b & 0xff;
                out.append(hex[(v >>> 4) & 15]).append(hex[v & 15]);
            }
            String actual = out.toString();
            for (String allowed : AUTHORIZED_DEVICE_SHA256) {
                if (allowed != null && actual.equalsIgnoreCase(allowed.replace(":", "").trim())) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean isDebuggerOrDebuggable() {
        try {
            if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) return true;
        } catch (Throwable ignored) {
        }
        try {
            ApplicationInfo info = EnvUtils.getApplicationInfo();
            return info != null && (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isVirtualEnvironment() {
        if ("1".equals(readProperty(
                ParallaxHuYaarBhai.z(675763901, 135, 15, 113, 206, 167, 13, 227, 178, 220, 254, 83, 10, 173, 166)))) {
            return true;
        }

        String hardware = lower(Build.HARDWARE);
        if (contains(hardware,
                ParallaxHuYaarBhai.z(187500931, 126, 164, 169, 139, 248, 125, 42, 244),
                ParallaxHuYaarBhai.z(517674022, 108, 176, 95, 61, 251, 174))) {
            return true;
        }

        int hints = 0;
        if (contains(lower(Build.FINGERPRINT),
                ParallaxHuYaarBhai.z(1538747612, 153, 1, 53, 100, 155, 71, 92),
                ParallaxHuYaarBhai.z(1493875674, 182, 143, 66, 74, 237, 84, 55, 96))) hints++;
        if (contains(lower(Build.MODEL),
                ParallaxHuYaarBhai.z(1493875674, 182, 143, 66, 74, 237, 84, 55, 96),
                ParallaxHuYaarBhai.z(1377800929, 34, 145, 116, 204, 12, 122, 209, 240, 61, 174))) hints++;
        if (contains(lower(Build.PRODUCT),
                ParallaxHuYaarBhai.z(1377800929, 34, 145, 116, 204, 12, 122, 209, 240, 61, 174),
                ParallaxHuYaarBhai.z(215503132, 78, 167, 93, 151))) hints++;
        if (lower(Build.BRAND).startsWith(
                ParallaxHuYaarBhai.z(1538747612, 153, 1, 53, 100, 155, 71, 92))
                && lower(Build.DEVICE).startsWith(
                ParallaxHuYaarBhai.z(1538747612, 153, 1, 53, 100, 155, 71, 92))) {
            hints += 2;
        }
        return hints >= 3;
    }

    private static String readProperty(String key) {
        try {
            Class<?> c = Class.forName(ParallaxHuYaarBhai.z(
                    494759633, 237, 141, 94, 158, 135, 249, 157, 169, 144, 70, 84,
                    188, 235, 16, 229, 60, 186, 175, 70, 173, 236, 142, 75, 38, 215, 202, 239));
            java.lang.reflect.Method m = c.getDeclaredMethod(
                    ParallaxHuYaarBhai.z(843257123, 116, 215, 50), String.class);
            Object v = m.invoke(null, key);
            return v instanceof String ? (String) v : "";
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.US);
    }

    private static boolean contains(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }
}
