# Parallax shell rules.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int e(...);
    public static int w(...);
    public static int i(...);
    public static int v(...);
}

-repackageclasses com.parallax.shell

-keep class com.parallax.shell.ParallaxJaRaha { *; }
-keep class com.parallax.shell.ParallaxKoChummiDedo { *; }
-keep class com.parallax.shell.ParallaxLovers { *; }
-keep class com.parallax.shell.ParallaxLovers$* { *; }
-keep class com.parallax.shell.ParallaxHuMaalik { *; }
-keep class com.parallax.shell.ParallaxKaBhaiJangu { *; }

# Preserve requested security class names while allowing ordinary helper members
# to be optimized. The JNI bridge method name must remain stable.
-keepnames class com.parallax.shell.ParallaxHuYaarBhai
-keepnames class com.parallax.shell.ParallaxBhaiya
-keepclassmembers class com.parallax.shell.ParallaxBhaiya {
    public static native boolean nativeGate();
}

-keep class com.parallax.parallax.ParallaxAaGaya { *; }
