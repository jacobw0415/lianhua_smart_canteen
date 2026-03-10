package com.lianhua.erp.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 提供簡單的字串加解密，用於保護資料庫中的敏感欄位（如 MFA 秘鑰）。
 *
 * 採用 AES/GCM/NoPadding：
 * - 每次加密隨機產生 12 bytes IV
 * - 實際儲存格式為 base64( IV(12 bytes) + cipherBytes )
 */
@Component
@Slf4j
public class EncryptionService {

    private static final String ALGO = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptionService(@Value("${app.security.mfa-encryption-key:}") String encryptionKeyConfig) {
        if (encryptionKeyConfig == null || encryptionKeyConfig.isBlank()) {
            log.warn("未設定 app.security.mfa-encryption-key，將使用啟動時隨機產生的金鑰（重啟後將無法解密既有資料，僅適用開發環境）。");
            byte[] keyBytes = new byte[32];
            secureRandom.nextBytes(keyBytes);
            this.secretKey = new SecretKeySpec(keyBytes, ALGO);
        } else {
            byte[] keyBytes = encryptionKeyConfig.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < 16) {
                throw new IllegalStateException("app.security.mfa-encryption-key 長度過短，至少需 16 bytes。");
            }
            // 規範為 16/24/32 bytes，過長則截斷至 32 bytes
            if (keyBytes.length > 32) {
                byte[] truncated = new byte[32];
                System.arraycopy(keyBytes, 0, truncated, 0, 32);
                keyBytes = truncated;
            }
            this.secretKey = new SecretKeySpec(keyBytes, ALGO);
        }
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("加密失敗，將回傳原始字串以避免中斷流程：{}", e.getMessage());
            return plainText;
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);
            if (combined.length <= IV_LENGTH) {
                return null;
            }
            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherBytes = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密失敗，將回傳 null 以避免洩露錯誤內容：{}", e.getMessage());
            return null;
        }
    }
}

