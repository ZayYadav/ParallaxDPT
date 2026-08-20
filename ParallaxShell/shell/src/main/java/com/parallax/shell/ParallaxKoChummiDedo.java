package com.parallax.shell;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.parallax.shell.util.FileUtils;

/** Proxy Application used by the Parallax shell. */
public class ParallaxKoChummiDedo extends Application {
    private static final String TAG = ParallaxKoChummiDedo.class.getSimpleName();
    private String realApplicationName = "";
    private Application realApplication = null;

    private void replaceApplication() {
        if (Global.sRootBlocked) {
            return;
        }
        if (Global.sNeedCalledApplication && !TextUtils.isEmpty(realApplicationName)) {
            realApplication = (Application) ParallaxJaRaha.ra(realApplicationName);
            Log.d(TAG, "applicationExchange: " + realApplicationName + ", realApplication: " + realApplication.getClass().getName());
            ParallaxJaRaha.craoc(realApplicationName);
            Global.sNeedCalledApplication = false;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Global.sRootBlocked || ShellGuard.isRootedDevice()) {
            Global.sRootBlocked = true;
            if (Build.VERSION.SDK_INT < 28) {
                ShellGuard.installLegacyActivityBlocker(this);
            }
            return;
        }
        Log.d(TAG, "parallax onCreate");
        replaceApplication();
    }

    @Override
    public Context createPackageContext(String packageName, int flags) throws PackageManager.NameNotFoundException {
        if (Global.sRootBlocked) {
            return super.createPackageContext(packageName, flags);
        }
        Log.d(TAG, "createPackageContext: " + realApplicationName);
        if (!TextUtils.isEmpty(realApplicationName)) {
            replaceApplication();
            return realApplication;
        }
        return super.createPackageContext(packageName, flags);
    }

    @Override
    public String getPackageName() {
        if (Global.sRootBlocked) {
            return super.getPackageName();
        }
        if (!TextUtils.isEmpty(realApplicationName)) {
            return "";
        }
        return super.getPackageName();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Global.sRootBlocked = ShellGuard.isRootedDevice();
        Log.d(TAG, "parallax attachBaseContext classloader = " + base.getClassLoader());

        // Android 9+ is blocked before native shell loading through AppComponentFactory.
        if (Global.sRootBlocked && Build.VERSION.SDK_INT >= 28) {
            Log.w(TAG, ShellGuard.ROOT_MESSAGE);
            return;
        }

        if (!Global.sIsReplacedClassLoader) {
            ApplicationInfo applicationInfo = base.getApplicationInfo();
            if (applicationInfo == null) {
                throw new NullPointerException("application info is null");
            }
            FileUtils.unzipLibs(applicationInfo.sourceDir, applicationInfo.dataDir);
            ParallaxJaRaha.loadShellLibs(applicationInfo.dataDir);
            Log.d(TAG, "ParallaxKoChummiDedo init");
            ParallaxJaRaha.ia();
            ClassLoader targetClassLoader = base.getClassLoader();
            ParallaxJaRaha.cbde(targetClassLoader);
            Global.sIsReplacedClassLoader = true;
        }

        if (!Global.sRootBlocked) {
            realApplicationName = ParallaxJaRaha.rapn();
        }
    }
}
