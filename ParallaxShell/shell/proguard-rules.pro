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
-keep class com.parallax.shell.RootBlockActivity { *; }

# Preserve the requested public security class name, but allow R8 to optimize and
# obfuscate its members. ShellGuard is intentionally not kept so release builds
# can inline/remove the compatibility shim.
-keepnames class com.parallax.shell.ParallaxHuYaarBhai

-keep class com.parallax.parallax.ParallaxAaGaya { *; }
