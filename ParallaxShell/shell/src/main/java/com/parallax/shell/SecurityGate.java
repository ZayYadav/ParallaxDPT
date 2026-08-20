package com.parallax.shell;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Early startup gate for rooted / instrumented environments.
 *
 * This is intentionally independent from the native risk thread: it runs before the
 * protected payload is restored so a rooted device can be redirected to the shell's
 * block screen instead of executing application code.
 */
public final class SecurityGate {
    private static final int UNKNOWN = 0;
    private static final int SAFE = 1;
    private static final int BLOCKED = 2;

    private static volatile int state = UNKNOWN;
    private static volatile boolean instrumentationInstalled;

    private static final String[] ROOT_PATHS = {
            "/system/app/Superuser.apk",
            "/system/app/SuperSU.apk",
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
            "/system_ext/bin/su",
            "/vendor/bin/su",
            "/data/local/bin/su",
            "/data/local/xbin/su",
            "/data/adb/magisk",
            "/data/adb/ksu",
            "/data/adb/ap",
            "/metadata/adb/magisk"
    };

    private static final String[] PROC_MARKERS = {
            "frida",
            "gum-js-loop",
            "gmain",
            "linjector",
            "magisk",
            "zygisk",
            "kernelsu",
            "apatch",
            "riru",
            "xposed",
            "lsposed",
            "edxp",
            "substrate"
    };

    private SecurityGate() {
    }

    public static boolean evaluate() {
        int cached = state;
        if (cached != UNKNOWN) {
            return cached == BLOCKED;
        }
        synchronized (SecurityGate.class) {
            if (state == UNKNOWN) {
                boolean blocked = hasRootArtifact()
                        || procContainsMarker("/proc/self/maps")
                        || procContainsMarker("/proc/self/mountinfo")
                        || hasTracer();
                state = blocked ? BLOCKED : SAFE;
                Global.sSecurityBlocked = blocked;
            }
        }
        return state == BLOCKED;
    }

    public static boolean isBlocked() {
        return state == BLOCKED || evaluate();
    }

    private static boolean hasRootArtifact() {
        for (String path : ROOT_PATHS) {
            try {
                if (new File(path).exists()) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }

        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String[] pathEntries = pathEnv.split(File.pathSeparator);
            for (String entry : pathEntries) {
                if (entry == null || entry.length() == 0) {
                    continue;
                }
                try {
                    File su = new File(entry, "su");
                    if (su.exists() && su.canExecute()) {
                        return true;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return false;
    }

    private static boolean procContainsMarker(String path) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String normalized = line.toLowerCase(Locale.US);
                for (String marker : PROC_MARKERS) {
                    if (normalized.contains(marker)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean hasTracer() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/self/status"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("TracerPid:")) {
                    continue;
                }
                String value = line.substring("TracerPid:".length()).trim();
                return !"0".equals(value);
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Fallback for Android versions where AppComponentFactory is unavailable.
     * Replaces ActivityThread's Instrumentation only after the process is already blocked,
     * so the first activity becomes ParallaxAaGaya instead of loading protected app code.
     */
    public static void installBlockingInstrumentation() {
        if (!isBlocked() || instrumentationInstalled) {
            return;
        }
        synchronized (SecurityGate.class) {
            if (instrumentationInstalled) {
                return;
            }
            try {
                Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
                Method currentActivityThread = activityThreadClass.getDeclaredMethod("currentActivityThread");
                currentActivityThread.setAccessible(true);
                Object activityThread = currentActivityThread.invoke(null);
                if (activityThread == null) {
                    return;
                }
                Field instrumentationField = activityThreadClass.getDeclaredField("mInstrumentation");
                instrumentationField.setAccessible(true);
                Object current = instrumentationField.get(activityThread);
                if (!(current instanceof BlockingInstrumentation)) {
                    instrumentationField.set(activityThread, new BlockingInstrumentation());
                }
                instrumentationInstalled = true;
            } catch (Throwable ignored) {
                // AppComponentFactory remains the primary path on modern Android.
            }
        }
    }

    private static final class BlockingInstrumentation extends Instrumentation {
        @Override
        public Activity newActivity(ClassLoader cl, String className, Intent intent)
                throws ClassNotFoundException, IllegalAccessException, InstantiationException {
            return new ParallaxAaGaya();
        }
    }
}
