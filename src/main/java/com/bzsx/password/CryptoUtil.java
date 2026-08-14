package com.bzsx.password;

import android.util.Base64;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int KEY_LENGTH = 256;
    private static final int PBKDF2_ITERATIONS = 100000;
    private static final int SALT_LENGTH = 16;

    /**
     * 使用 PBKDF2 对主密码进行哈希（替代旧版 SHA-256）
     * 格式：salt(16字节) + hash(32字节) → Base64
     */
    public static String hashPassword(String password) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();

            byte[] combined = new byte[salt.length + hash.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hash, 0, combined, salt.length, hash.length);

            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 验证主密码（兼容旧版 SHA-256 和新版 PBKDF2）
     */
    public static boolean verifyPassword(String password, String storedHash) {
        try {
            byte[] combined = Base64.decode(storedHash, Base64.NO_WRAP);

            // 判断是新版 PBKDF2 (32字节hash) 还是旧版 SHA-256 (combined-salt=16+hash=32 共48字节？不对)
            // 旧版：16字节salt + 32字节SHA-256 = 48字节
            // 新版：16字节salt + 32字节PBKDF2 = 48字节
            // 字节数一样，但算法不同。先尝试新版 PBKDF2，失败再尝试旧版 SHA-256
            if (combined.length < 16) {
                return false;
            }

            byte[] salt = new byte[SALT_LENGTH];
            System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);

            // 先试新版 PBKDF2
            try {
                PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                byte[] hash = factory.generateSecret(spec).getEncoded();

                byte[] storedHashBytes = new byte[combined.length - SALT_LENGTH];
                System.arraycopy(combined, SALT_LENGTH, storedHashBytes, 0, storedHashBytes.length);

                if (java.security.MessageDigest.isEqual(hash, storedHashBytes)) {
                    // PBKDF2 验证通过
                    return true;
                }
            } catch (Exception ignored) {
                // PBKDF2 失败，尝试旧版 SHA-256
            }

            // 再试旧版 SHA-256（向后兼容）
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                digest.update(salt);
                byte[] sha256Hash = digest.digest(password.getBytes());

                byte[] storedHashBytes = new byte[combined.length - SALT_LENGTH];
                System.arraycopy(combined, SALT_LENGTH, storedHashBytes, 0, storedHashBytes.length);

                return java.security.MessageDigest.isEqual(sha256Hash, storedHashBytes);
            } catch (Exception e) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static SecretKey deriveKey(String masterPassword, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(masterPassword.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static String encrypt(String plaintext, String masterPassword) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);
            byte[] iv = new byte[GCM_IV_LENGTH];
            random.nextBytes(iv);

            SecretKey key = deriveKey(masterPassword, salt);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

            byte[] combined = new byte[salt.length + iv.length + ciphertext.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(iv, 0, combined, salt.length, iv.length);
            System.arraycopy(ciphertext, 0, combined, salt.length + iv.length, ciphertext.length);

            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public static String decrypt(String encryptedData, String masterPassword) {
        try {
            byte[] combined = Base64.decode(encryptedData, Base64.NO_WRAP);
            byte[] salt = new byte[SALT_LENGTH];
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - salt.length - iv.length];

            System.arraycopy(combined, 0, salt, 0, salt.length);
            System.arraycopy(combined, salt.length, iv, 0, iv.length);
            System.arraycopy(combined, salt.length + iv.length, ciphertext, 0, ciphertext.length);

            SecretKey key = deriveKey(masterPassword, salt);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}