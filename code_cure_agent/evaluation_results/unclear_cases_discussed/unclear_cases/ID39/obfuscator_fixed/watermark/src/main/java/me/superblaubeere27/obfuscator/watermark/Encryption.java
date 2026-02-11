package me.superblaubeere27.obfuscator.watermark;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;

public class Encryption {

    public static String decrypt(String obj, String key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)), "AES");

            // Use AES with GCM mode and NoPadding for secure encryption and integrity
            Cipher des = Cipher.getInstance("AES/GCM/NoPadding");
            // Decode the Base64 input
            byte[] decoded = Base64.getDecoder().decode(obj.getBytes(StandardCharsets.UTF_8));
            // Extract the 12-byte IV from the beginning
            byte[] iv = new byte[12];
            System.arraycopy(decoded, 0, iv, 0, 12);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            // Extract the ciphertext after the IV
            byte[] cipherText = new byte[decoded.length - 12];
            System.arraycopy(decoded, 12, cipherText, 0, cipherText.length);
            des.init(Cipher.DECRYPT_MODE, keySpec, spec);
            return new String(des.doFinal(cipherText), StandardCharsets.UTF_8);

            return new String(des.doFinal(Base64.getDecoder().decode(obj.getBytes(StandardCharsets.UTF_8))), StandardCharsets.UTF_8);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String encrypt(String obj, String key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)), "AES");

            // Use AES with GCM mode and NoPadding for secure encryption and integrity
            Cipher des = Cipher.getInstance("AES/GCM/NoPadding");
            // Generate a random 12-byte IV
            byte[] iv = new byte[12];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            des.init(Cipher.ENCRYPT_MODE, keySpec, spec);
            byte[] encrypted = des.doFinal(obj.getBytes(StandardCharsets.UTF_8));
            // Prepend IV to the encrypted bytes
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return new String(Base64.getEncoder().encode(combined), StandardCharsets.UTF_8);

            return new String(Base64.getEncoder().encode(des.doFinal(obj.getBytes(StandardCharsets.UTF_8))), StandardCharsets.UTF_8);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
