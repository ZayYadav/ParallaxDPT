package com.parallax.shell;

import android.app.Activity;
import android.app.Application;

/** Compatibility shim for source call-sites. */
@Deprecated
final class ShellGuard {
    static final String ROOT_MESSAGE = ParallaxBhaiya.ROOT_MESSAGE;

    private ShellGuard() {}

    static boolean isRootedDevice() {
        return ParallaxHuYaarBhai.isRootedDevice();
    }

    static void installLegacyActivityBlocker(Application application) {
        ParallaxBhaiya.installRootActivityBlocker(application);
    }

    static void showRootDialog(Activity activity) {
        ParallaxBhaiya.showRootDialog(activity);
    }
}
