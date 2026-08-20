package com.parallax.shell;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AppComponentFactory;
import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parallax.shell.util.EnvUtils;
import com.parallax.shell.util.FileUtils;

import java.lang.reflect.Method;

@TargetApi(28)
public class ParallaxLovers extends AppComponentFactory {
    private static final String TAG = "parallax " + ParallaxLovers.class.getSimpleName();
    private static AppComponentFactory sAppComponentFactory;

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
        if (SecurityGate.isBlocked()) {
            Global.sSecurityBlocked = true;
            return new ParallaxAaGaya();
        }

        AppComponentFactory targetAppComponentFactory = getTargetAppComponentFactory(cl);
        if (targetAppComponentFactory != null) {
            try {
                Method method = AppComponentFactory.class.getDeclaredMethod(
                        "instantiateActivity", ClassLoader.class, String.class, Intent.class);
                return (Activity) method.invoke(targetAppComponentFactory, cl, className, intent);
            } catch (Exception ignored) {
            }
        }
        return super.instantiateActivity(cl, className, intent);
    }

    @Override
    public Application instantiateApplication(@NonNull ClassLoader cl, @NonNull String className)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (SecurityGate.evaluate()) {
            Global.sSecurityBlocked = true;
            return super.instantiateApplication(cl, className);
        }

        if (!Global.sIsReplacedClassLoader) {
            if (EnvUtils.getApplicationInfo() == null) {
                throw new NullPointerException("application info is null");
            }
            String dataDir = EnvUtils.getApplicationInfo().dataDir;
            String sourceDir = EnvUtils.getApplicationInfo().sourceDir;
            FileUtils.unzipLibs(sourceDir, dataDir);
            ParallaxJaRaha.loadShellLibs(dataDir);
        }
        ParallaxJaRaha.ia();

        String applicationName = ParallaxJaRaha.rapn();
        AppComponentFactory targetAppComponentFactory;
        if (!Global.sIsReplacedClassLoader) {
            ParallaxJaRaha.cbde(cl);
            Global.sIsReplacedClassLoader = true;
            targetAppComponentFactory = getTargetAppComponentFactory(cl);
        } else {
            targetAppComponentFactory = getTargetAppComponentFactory(cl);
        }

        Global.sNeedCalledApplication = false;

        if (targetAppComponentFactory != null) {
            try {
                Method method = targetAppComponentFactory.getClass().getDeclaredMethod(
                        "instantiateApplication", ClassLoader.class, String.class);
                if (!TextUtils.isEmpty(applicationName)) {
                    return (Application) method.invoke(targetAppComponentFactory, cl, applicationName);
                }
                return (Application) method.invoke(targetAppComponentFactory, cl, className);
            } catch (Exception e) {
                Log.e(TAG, "instantiateApplication", e);
            }
        }

        if (!TextUtils.isEmpty(applicationName)) {
            try {
                Class.forName(applicationName, false, cl);
            } catch (ClassNotFoundException e) {
                if (EnvUtils.getApplicationInfo() != null) {
                    applicationName = EnvUtils.getApplicationInfo().packageName + "." + applicationName;
                }
            }
            return super.instantiateApplication(cl, applicationName);
        }
        return super.instantiateApplication(cl, className);
    }

    @Override
    public ClassLoader instantiateClassLoader(@NonNull ClassLoader cl, @NonNull ApplicationInfo aInfo) {
        if (SecurityGate.evaluate()) {
            Global.sSecurityBlocked = true;
            return super.instantiateClassLoader(cl, aInfo);
        }

        FileUtils.unzipLibs(aInfo.sourceDir, aInfo.dataDir);
        ParallaxJaRaha.loadShellLibs(aInfo.dataDir);
        ParallaxJaRaha.ia();

        AppComponentFactory targetAppComponentFactory = getTargetAppComponentFactory(cl);
        ParallaxJaRaha.cbde(cl);
        Global.sIsReplacedClassLoader = true;

        if (targetAppComponentFactory != null) {
            try {
                Method method = AppComponentFactory.class.getDeclaredMethod(
                        "instantiateClassLoader", ClassLoader.class, ApplicationInfo.class);
                return (ClassLoader) method.invoke(targetAppComponentFactory, cl, aInfo);
            } catch (Exception ignored) {
            }
        }
        return super.instantiateClassLoader(cl, aInfo);
    }

    @Override
    public BroadcastReceiver instantiateReceiver(@NonNull ClassLoader cl, @NonNull String className, Intent intent)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (SecurityGate.isBlocked()) {
            return new BlockedReceiver();
        }
        AppComponentFactory targetAppComponentFactory = getTargetAppComponentFactory(cl);
        if (targetAppComponentFactory != null) {
            try {
                Method method = AppComponentFactory.class.getDeclaredMethod(
                        "instantiateReceiver", ClassLoader.class, String.class, Intent.class);
                return (BroadcastReceiver) method.invoke(targetAppComponentFactory, cl, className, intent);
            } catch (Exception ignored) {
            }
        }
        return super.instantiateReceiver(cl, className, intent);
    }

    @Override
    public Service instantiateService(@NonNull ClassLoader cl, @NonNull String className, Intent intent)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (SecurityGate.isBlocked()) {
            return new BlockedService();
        }
        AppComponentFactory targetAppComponentFactory = getTargetAppComponentFactory(cl);
        if (targetAppComponentFactory != null) {
            try {
                Method method = AppComponentFactory.class.getDeclaredMethod(
                        "instantiateService", ClassLoader.class, String.class, Intent.class);
                return (Service) method.invoke(targetAppComponentFactory, cl, className, intent);
            } catch (Exception ignored) {
            }
        }
        return super.instantiateService(cl, className, intent);
    }

    @Override
    public ContentProvider instantiateProvider(@NonNull ClassLoader cl, @NonNull String className)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (SecurityGate.isBlocked()) {
            return new BlockedProvider();
        }
        AppComponentFactory targetAppComponentFactory = getTargetAppComponentFactory(cl);
        if (targetAppComponentFactory != null) {
            try {
                Method method = AppComponentFactory.class.getDeclaredMethod(
                        "instantiateProvider", ClassLoader.class, String.class);
                return (ContentProvider) method.invoke(targetAppComponentFactory, cl, className);
            } catch (Exception ignored) {
            }
        }
        return super.instantiateProvider(cl, className);
    }

    public static final class BlockedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
        }
    }

    public static final class BlockedService extends Service {
        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    public static final class BlockedProvider extends ContentProvider {
        @Override
        public boolean onCreate() {
            return true;
        }

        @Nullable
        @Override
        public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                            @Nullable String[] selectionArgs, @Nullable String sortOrder) {
            return null;
        }

        @Nullable
        @Override
        public String getType(@NonNull Uri uri) {
            return null;
        }

        @Nullable
        @Override
        public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
            return null;
        }

        @Override
        public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
            return 0;
        }

        @Override
        public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                          @Nullable String[] selectionArgs) {
            return 0;
        }
    }
}
