/*
 * Copyright (c) 2017-2019 superblaubeere27, Sam Sun, MarcoMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package me.superblaubeere27.jobf.processors.encryption.string;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class DESEncryptionAlgorithm implements IStringEncryptionAlgorithm {

    // Length of AES key in bytes (128 bits)
    private static final int AES_KEY_SIZE = 16;
    // GCM nonce length
    private static final int GCM_NONCE_LENGTH = 12;
    // GCM tag length (in bits)
    private static final int GCM_TAG_LENGTH = 128;

    public static String decrypt(String obj, String key) {
        try {
            byte[] decoded = Base64.getDecoder().decode(obj.getBytes(StandardCharsets.UTF_8));
            // Extract nonce and ciphertext
            byte[] nonce = Arrays.copyOfRange(decoded, 0, GCM_NONCE_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(decoded, GCM_NONCE_LENGTH, decoded.length);

            byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec keySpec = new SecretKeySpec(Arrays.copyOf(keyBytes, AES_KEY_SIZE), "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String encrypt(String obj, String key) {
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec keySpec = new SecretKeySpec(Arrays.copyOf(keyBytes, AES_KEY_SIZE), "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            byte[] nonce = new byte[GCM_NONCE_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(nonce);

            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);

            byte[] cipherText = cipher.doFinal(obj.getBytes(StandardCharsets.UTF_8));

            // Prepend nonce to cipher text for use in decryption
            byte[] combined = new byte[nonce.length + cipherText.length];
            System.arraycopy(nonce, 0, combined, 0, nonce.length);
            System.arraycopy(cipherText, 0, combined, nonce.length, cipherText.length);

            return new String(Base64.getEncoder().encode(combined), StandardCharsets.UTF_8);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}