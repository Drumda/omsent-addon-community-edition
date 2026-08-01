package com.omsent.addon.Utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import java.util.Base64;

public class AESUtils {
    private AESUtils() {}

    public static String encrypt(String text, byte[] key) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(key));

        byte[] cipherBytes = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));

        byte[] ivAndCipher = new byte[16 + cipherBytes.length];
        System.arraycopy(key, 0, ivAndCipher, 0, 16);
        System.arraycopy(cipherBytes, 0, ivAndCipher, 16, cipherBytes.length);

        return Base64.getEncoder().encodeToString(ivAndCipher);
    }
    public static String encrypt(String text, String key) throws Exception {
        return encrypt(text, key.getBytes(StandardCharsets.UTF_8));
    }

    public static String decrypt(String text, byte[] key) throws Exception {
        byte[] ivAndCipher = Base64.getDecoder().decode(text);

        if (ivAndCipher.length < 16) {
            throw new IllegalArgumentException("invalid length");
        }

        byte[] iv = new byte[16];
        System.arraycopy(ivAndCipher, 0, iv, 0, 16);

        byte[] cipherBytes = new byte[ivAndCipher.length - 16];
        System.arraycopy(ivAndCipher, 16, cipherBytes, 0, cipherBytes.length);

        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));

        byte[] textBytes = cipher.doFinal(cipherBytes);
        return new String(textBytes, StandardCharsets.UTF_8);
    }
    public static String decrypt(String text, String key) throws Exception {
        return decrypt(text, key.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] getRandByteKey() {
        byte[] key = new byte[16];
        new SecureRandom().nextBytes(key);
        return key;
    }
}
