package com.parallax.shell;

import android.app.Activity;
import android.app.Application;

/**
 * Compatibility shim for source call-sites. Release R8 is allowed to inline and
 * remove this wrapper; the security implementation lives in ParallaxHuYaarBhai.
 */
@Deprecated
final class ShellGuard {
    static final String ROOT_MESSAGE = ParallaxHuYaarBhai.ROOT_MESSAGE;

    private ShellGuard() {
    }

    static boolean isRootedDevice() {
        return ParallaxHuYaarBhai.isRootedDevice();
    }

    static void installLegacyActivityBlocker(Application application) {
        ParallaxHuYaarBhai.installLegacyActivityBlocker(application);
    }

    static void showRootDialog(Activity activity) {
        ParallaxHuYaarBhai.showRootDialog(activity);
    }
}
