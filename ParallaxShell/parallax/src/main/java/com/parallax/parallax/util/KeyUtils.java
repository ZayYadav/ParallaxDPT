package com.parallax.parallax.util;

import java.security.SecureRandom;

public class KeyUtils {

    public static byte[] generateIV(byte[] key) {
        byte[] newKey = new byte[key.length];
        System.arraycopy(key, 0, newKey, 0, newKey.length);
        newKey[3] = 0x2f;
        newKey[9] = 0x76;
        return newKey;
    }

    public static byte[] generateKey() {
        byte[] encryptionKey = new byte[16];
        new SecureRandom().nextBytes(encryptionKey);
        return encryptionKey;
    }
}
