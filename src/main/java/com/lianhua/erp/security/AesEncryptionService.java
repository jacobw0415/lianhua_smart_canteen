package com.lianhua.erp.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 對稱加解密服務。
 *
 * 設計要點：
 * - 金鑰從設定檔 / 環境變數載入（lianhua.crypto.aesKey 或 AES_KEY），不硬編於程式中。
 * - 使用 AES/GCM/NoPadding，隨機 IV，將 IV + Cipher Text 一起以 Base64 字串回傳。
 * - 若金鑰未正確設定，服務會啟用失敗並在首次使用時丟出明確錯誤，提醒必須設定金鑰。
 *
 * 用法示例：
 *   String cipher = aesEncryptionService.encrypt("敏感資料");
 *   String plain  = aesEncryptionService.decrypt(cipher);
 */
@Service
@Slf4j
public class AesEncryptionService {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12; // GCM 建議 12 bytes

    /**
     * 以 Base64 編碼的 256-bit AES 金鑰字串。
     * 建議透過環境變數 AES_KEY 或設定檔 lianhua.crypto.aesKey 提供，例如：
     *   export AES_KEY=$(openssl rand -base64 32)
     */
    @Value("${lianhua.crypto.aesKey:}")
    private String aesKeyBase64;

    private SecretKey secretKey;
    private boolean enabled;

    @PostConstruct
    void init() {
        if (aesKeyBase64 == null || aesKeyBase64.isBlank()) {
            enabled = false;
            log.warn("AES 加密金鑰未設定（lianhua.crypto.aesKey / AES_KEY），AesEncryptionService 將停用。");
            return;
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(aesKeyBase64);
            if (keyBytes.length != 32) {
                enabled = false;
                log.error("AES 金鑰長度錯誤，期望 32 bytes (256-bit)，實際為 {} bytes。", keyBytes.length);
                return;
            }
            this.secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);
            this.enabled = true;
            log.info("AesEncryptionService 啟用完成（已載入 256-bit AES 金鑰）。");
        } catch (IllegalArgumentException e) {
            enabled = false;
            log.error("無法解析 AES 金鑰（非合法 Base64 字串），請檢查 lianhua.crypto.aesKey / AES_KEY 設定。", e);
        }
    }

    private void ensureEnabled() {
        if (!enabled || secretKey == null) {
            throw new IllegalStateException("AES 加密服務未啟用：請先正確設定 256-bit AES 金鑰（環境變數 AES_KEY 或 lianhua.crypto.aesKey）。");
        }
    }

    /**
     * 將明文字串加密為 Base64 字串（內容為 IV + Cipher Text）。
     */
    public String encrypt(String plainText) {
        ensureEnabled();
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 將 IV 與 Cipher 合併後做 Base64 編碼
            byte[] combined = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("AES 加密失敗", e);
            throw new IllegalStateException("AES 加密失敗", e);
        }
    }

    /**
     * 將 Base64 字串（IV + Cipher Text）解密為原始明文。
     */
    public String decrypt(String cipherText) {
        ensureEnabled();
        if (cipherText == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);
            if (combined.length <= IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("無效的 AES 密文字串：長度不足以包含 IV。");
            }

            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] cipherBytes = new byte[combined.length - IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(combined, IV_LENGTH_BYTES, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES 解密失敗", e);
            throw new IllegalStateException("AES 解密失敗", e);
        }
    }
}

