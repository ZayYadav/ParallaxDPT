package com.parallax.shell;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AppComponentFactory;
import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.parallax.shell.util.EnvUtils;
import com.parallax.shell.util.FileUtils;

import java.lang.reflect.Method;

@TargetApi(28)
public class ParallaxLovers extends AppComponentFactory {
    private static final String TAG = "parallax " + ParallaxLovers.class.getSimpleName();
    private static AppComponentFactory sAppComponentFactory;

    private static boolean rootBlocked() {
        if (Global.sRootBlocked || ShellGuard.isRootedDevice()) {
            Global.sRootBlocked = true;
            return true;
        }
        return false;
    }

    private static boolean anyBlocked() {
        return Global.sDeveloperBlocked || Global.sNativeBlocked || rootBlocked();
    }

    private String getTargetClassName() {
        return ParallaxJaRaha.rcf();
    }

    private AppComponentFactory getTargetAppComponentFactory(ClassLoader appClassLoader) {
        if (sAppComponentFactory == null) {
            String targetClassName = getTargetClassName();
            Log.d(TAG, "targetClassName = " + targetClassName);
            if (!TextUtils.isEmpty(targetClassName)) {
                try {
                    sAppComponentFactory = (AppComponentFactory) Class.forName(targetClassName, true, appClassLoader).newInstance();
                    return sAppComponentFactory;
                } catch (Exception ignored) {
                }
            }
        }
        return sAppComponentFactory;
    }

    @Override
    public Activity instantiateActivity(@NonNull ClassLoader cl, @NonNull String className, Intent intent)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (Global.sDeveloperBlocked || Global.sNativeBlocked) {
            return new ParallaxKaBhaiJangu();
        }
        if (rootBlocked()) {
            return new ParallaxHuMaalik();
        }
        AppComponentFactory target = getTargetAppComponentFactory(cl);
        if (target != null) {
            try {
                Method method = AppComponentFactory.class.getDeclaredMethod("instantiateActivity", ClassLoader.class, String.class, Intent.class);
                return (Activity) method.invoke(target, cl, className, intent);
            } catch (Exception ignored) {
            }
        }
        return super.instantiateActivity(cl, className, intent);
    }

    @Override
    public Application instantiateApplication(@NonNull ClassLoader cl, @NonNull String className)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (anyBlocked()) {
            return super.instantiateApplication(cl, className);
        }
        if (!Global.sIsReplacedClassLoader) {
            ApplicationInfo info = EnvUtils.getApplicationInfo();
            if (info == null) {
                throw new NullPointerException("application info is null");
            }
            FileUtils.unzipLibs(info.sourceDir, info.dataDir);
            ParallaxJaRaha.loadShellLibs(info.dataDir);
            if (Global.sNativeBlocked) {
                return super.instantiateApplication(cl, className);
            }
        }
        ParallaxJaRaha.ia();
        String applicationName = ParallaxJaRaha.rapn();
        AppComponentFactory target;
        if (!Global.sIsReplacedClassLoader) {
            ParallaxJaRaha.cbde(cl);
            Global.sIsReplacedClassLoader = true;
            target = getTargetAppComponentFactory(cl);
        } else {
            target = getTargetAppComponentFactory(cl);
        }
        Global.sNeedCalledApplication = false;
        if (target != null) {
            try {
                Method method = target.getClass().getDeclaredMethod("instantiateApplication", ClassLoader.class, String.class);
                if (!TextUtils.isEmpty(applicationName)) {
                    return (Application) method.invoke(target, cl, applicationName);
                }
                return (Application) method.invoke(target, cl, className);
            } catch (Exception e) {
                Log.e(TAG, "instantiateApplication", e);
            }
        }
        if (!TextUtils.isEmpty(applicationName)) {
            try {
                Class.forName(applicationName, false, cl);
            } catch (ClassNotFoundException e) {
                ApplicationInfo info = EnvUtils.getApplicationInfo();
                if (info != null) {
                    applicationName = info.packageName + "." + applicationName;
                }
            }
            return super.instantiateApplication(cl, applicationName);
        }
        return super.instantiateApplication(cl, className);
    }

    @Override
    public ClassLoader instantiateClassLoader(@NonNull ClassLoader cl, @NonNull ApplicationInfo aInfo) {
        if (anyBlocked()) {
            return cl;
        }
        FileUtils.unzipLibs(aInfo.sourceDir, aInfo.dataDir);
        ParallaxJaRaha.loadShellLibs(aInfo.dataDir);
        if (Global.sNativeBlocked) {
            return cl;
        }
        ParallaxJaRaha.ia();
        AppComponentFactory target = getTargetAppComponentFactory(cl);
        ParallaxJaRaha.cbde(cl);
        Global.sIsReplacedClassLoader = true;
        if (target != null) {
            try {
                Method method = AppComponentFactory.class.getDeclaredMethod("instantiateClassLoader", ClassLoader.class, ApplicationInfo.class);
                return (ClassLoader) method.invoke(target, cl, aInfo);
            } catch (Exception ignored) {
            }
        }
        return super.instantiateClassLoader(cl, aInfo);
    }

    @Override
    public BroadcastReceiver instantiateReceiver(@NonNull ClassLoader cl, @NonNull String className, Intent intent)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (anyBlocked()) {
            return new RootBlockedReceiver();
        }
        AppComponentFactory target = getTargetAppComponentFactory(cl);
        if (target != null) {
            try {
                Method method = AppComponentFactory.class.getDeclaredMethod("instantiateReceiver", ClassLoader.class, String.class, Intent.class);
                return (BroadcastReceiver) method.invoke(target, cl, className, intent);
            } catch (Exception ignored) {
            }
        }
        return super.instantiateReceiver(cl, className, intent);
    }

    @Override
    public Service instantiateService(@NonNull ClassLoader cl, @NonNull String className, Intent intent)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (anyBlocked()) {
            return new RootBlockedService();
        }
        AppComponentFactory target = getTargetAppComponentFactory(cl);
        if (target != null) {
            try {
                Method method = AppComponentFactory.class.getDeclaredMethod("instantiateService", ClassLoader.class, String.class, Intent.class);
                return (Service) method.invoke(target, cl, className, intent);
            } catch (Exception ignored) {
            }
        }
        return super.instantiateService(cl, className, intent);
    }

    @Override
    public ContentProvider instantiateProvider(@NonNull ClassLoader cl, @NonNull String className)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (anyBlocked()) {
            return new RootBlockedProvider();
        }
        AppComponentFactory target = getTargetAppComponentFactory(cl);
        if (target != null) {
            try {
                Method method = AppComponentFactory.class.getDeclaredMethod("instantiateProvider", ClassLoader.class, String.class);
                return (ContentProvider) method.invoke(target, cl, className);
            } catch (Exception ignored) {
            }
        }
        return super.instantiateProvider(cl, className);
    }

    public static final class RootBlockedReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) { }
    }

    public static final class RootBlockedService extends Service {
        @Override public IBinder onBind(Intent intent) { return null; }
        @Override public int onStartCommand(Intent intent, int flags, int startId) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }
    }

    public static final class RootBlockedProvider extends ContentProvider {
        @Override public boolean onCreate() { return true; }
        @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) { return null; }
        @Override public String getType(Uri uri) { return null; }
        @Override public Uri insert(Uri uri, ContentValues values) { return null; }
        @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
        @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
    }
}
