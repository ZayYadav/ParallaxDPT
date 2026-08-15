package com.parallax.shell;

import androidx.annotation.Keep;

import com.parallax.parallax.BuildConfig;

/**
 * @author parallax
 */
public class Global {
    public static final String APACHE_HTTP_LIB = "/system/framework/org.apache.http.legacy.jar";
    public static final String ZIP_LIB_DIR = "Parallax";
    public static final String LIB_DIR = "parallax-libs";
    public static final String SHELL_SO_NAME = BuildConfig.SO_NAME;
    @Keep
    public volatile static boolean sIsReplacedClassLoader = false;
    @Keep
    public volatile static boolean sNeedCalledApplication = true;

}
