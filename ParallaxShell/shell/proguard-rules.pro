# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int e(...);
    public static int w(...);
    public static int i(...);
    public static int v(...);
}

-repackageclasses com.parallax.shell

# JNI RegisterNatives depends on both this class name and its native method names.
-keep class com.parallax.shell.ParallaxJaRaha {
    native <methods>;
    public static void loadShellLibs(java.lang.String);
}

# These names are written into protected APK/AAB manifests and must survive R8.
-keepnames class com.parallax.shell.ParallaxKoChummiDedo
-keepnames class com.parallax.shell.ParallaxLovers
-keepnames class com.parallax.shell.ParallaxAaGaya
-keepnames class com.parallax.shell.SecurityGate

# Security gate members are called from multiple early-startup paths.
-keepclassmembers class com.parallax.shell.SecurityGate { *; }
