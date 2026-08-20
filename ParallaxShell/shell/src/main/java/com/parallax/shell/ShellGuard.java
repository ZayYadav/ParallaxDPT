package com.parallax.shell;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Process;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.WeakHashMap;

final class ShellGuard {
    static final String ROOT_MESSAGE = "Parallax Shell Not Work On Rooted Device";

    private static final String[] ROOT_PATHS = {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
            "/vendor/bin/su",
            "/system/bin/.ext/.su",
            "/system/app/Superuser.apk",
            "/system/app/SuperSU.apk",
            "/data/adb/magisk",
            "/data/adb/modules",
            "/data/adb/ksu",
            "/data/adb/ap",
            "/debug_ramdisk/.magisk",
            "/sbin/.magisk"
    };

    private static final WeakHashMap<Activity, Dialog> ROOT_DIALOGS = new WeakHashMap<>();
    private static volatile int rootState = -1;
    private static volatile boolean legacyBlockerInstalled;

    private ShellGuard() {
    }

    static boolean isRootedDevice() {
        int cached = rootState;
        if (cached != -1) {
            return cached == 1;
        }

        boolean rooted = hasKnownRootPath() || hasSuInPath() || hasRootMountMarker();
        rootState = rooted ? 1 : 0;
        return rooted;
    }

    private static boolean hasKnownRootPath() {
        for (String path : ROOT_PATHS) {
            try {
                if (new File(path).exists()) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private static boolean hasSuInPath() {
        try {
            String path = System.getenv("PATH");
            if (path == null || path.length() == 0) {
                return false;
            }
            String[] dirs = path.split(File.pathSeparator);
            for (String dir : dirs) {
                if (dir != null && dir.length() != 0 && new File(dir, "su").exists()) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean hasRootMountMarker() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/self/mounts"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String lower = line.toLowerCase();
                if (lower.contains("magisk")
                        || lower.contains("kernelsu")
                        || lower.contains("/data/adb/modules")
                        || lower.contains("/data/adb/ksu")
                        || lower.contains("apatch")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    static void installLegacyActivityBlocker(Application application) {
        if (legacyBlockerInstalled) {
            return;
        }
        synchronized (ShellGuard.class) {
            if (legacyBlockerInstalled) {
                return;
            }
            legacyBlockerInstalled = true;
        }

        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                showRootDialog(activity);
            }

            @Override
            public void onActivityStarted(Activity activity) {
                showRootDialog(activity);
            }

            @Override
            public void onActivityResumed(Activity activity) {
                showRootDialog(activity);
            }

            @Override public void onActivityPaused(Activity activity) { }
            @Override public void onActivityStopped(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        });
    }

    static void showRootDialog(final Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (activity.isFinishing()) {
                    return;
                }
                synchronized (ROOT_DIALOGS) {
                    Dialog existing = ROOT_DIALOGS.get(activity);
                    if (existing != null && existing.isShowing()) {
                        return;
                    }

                    final Dialog dialog = new Dialog(activity);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.setCancelable(false);
                    dialog.setCanceledOnTouchOutside(false);

                    FrameLayout frame = new FrameLayout(activity);
                    int outer = dp(activity, 20);
                    frame.setPadding(outer, 0, outer, 0);

                    LinearLayout card = new LinearLayout(activity);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setGravity(Gravity.CENTER_HORIZONTAL);
                    int cardPadding = dp(activity, 24);
                    card.setPadding(cardPadding, cardPadding, cardPadding, cardPadding);

                    GradientDrawable cardBackground = new GradientDrawable();
                    cardBackground.setColor(Color.rgb(20, 21, 28));
                    cardBackground.setCornerRadius(dp(activity, 24));
                    cardBackground.setStroke(dp(activity, 1), Color.rgb(238, 75, 95));
                    card.setBackground(cardBackground);

                    TextView icon = new TextView(activity);
                    icon.setText("!");
                    icon.setTextColor(Color.WHITE);
                    icon.setTextSize(26);
                    icon.setTypeface(Typeface.DEFAULT_BOLD);
                    icon.setGravity(Gravity.CENTER);
                    GradientDrawable iconBackground = new GradientDrawable();
                    iconBackground.setShape(GradientDrawable.OVAL);
                    iconBackground.setColor(Color.rgb(238, 75, 95));
                    icon.setBackground(iconBackground);
                    LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(activity, 54), dp(activity, 54));
                    card.addView(icon, iconParams);

                    TextView title = new TextView(activity);
                    title.setText("Parallax Security");
                    title.setTextColor(Color.WHITE);
                    title.setTextSize(21);
                    title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                    title.setGravity(Gravity.CENTER);
                    LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    titleParams.topMargin = dp(activity, 18);
                    card.addView(title, titleParams);

                    TextView message = new TextView(activity);
                    message.setText(ROOT_MESSAGE);
                    message.setTextColor(Color.rgb(245, 245, 248));
                    message.setTextSize(16);
                    message.setGravity(Gravity.CENTER);
                    message.setLineSpacing(0, 1.15f);
                    LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    messageParams.topMargin = dp(activity, 12);
                    card.addView(message, messageParams);

                    TextView detail = new TextView(activity);
                    detail.setText("Root or system modification was detected. This protected build has been stopped for security.");
                    detail.setTextColor(Color.rgb(176, 178, 190));
                    detail.setTextSize(13);
                    detail.setGravity(Gravity.CENTER);
                    detail.setLineSpacing(0, 1.2f);
                    LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    detailParams.topMargin = dp(activity, 10);
                    card.addView(detail, detailParams);

                    Button closeButton = new Button(activity);
                    closeButton.setText("Close App");
                    closeButton.setAllCaps(false);
                    closeButton.setTextColor(Color.WHITE);
                    closeButton.setTextSize(15);
                    closeButton.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                    closeButton.setPadding(dp(activity, 18), dp(activity, 11), dp(activity, 18), dp(activity, 11));
                    GradientDrawable buttonBackground = new GradientDrawable();
                    buttonBackground.setColor(Color.rgb(216, 55, 78));
                    buttonBackground.setCornerRadius(dp(activity, 16));
                    closeButton.setBackground(buttonBackground);
                    closeButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            terminate(activity);
                        }
                    });
                    LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    buttonParams.topMargin = dp(activity, 22);
                    card.addView(closeButton, buttonParams);

                    frame.addView(card, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
                    dialog.setContentView(frame);
                    ROOT_DIALOGS.put(activity, dialog);
                    dialog.show();

                    Window window = dialog.getWindow();
                    if (window != null) {
                        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        WindowManager.LayoutParams params = window.getAttributes();
                        params.width = WindowManager.LayoutParams.MATCH_PARENT;
                        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                        params.dimAmount = 0.78f;
                        window.setAttributes(params);
                        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    }
                }
            }
        });
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
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
