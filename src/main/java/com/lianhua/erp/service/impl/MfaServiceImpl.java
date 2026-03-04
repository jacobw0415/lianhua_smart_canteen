package com.lianhua.erp.service.impl;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.lianhua.erp.dto.auth.MfaSetupResponse;
import com.lianhua.erp.service.MfaService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
public class MfaServiceImpl implements MfaService {

    private static final int SECRET_BYTES = 20;
    private static final Duration TIME_STEP = Duration.ofSeconds(30);
    private static final int DIGITS = 6;

    private final TimeBasedOneTimePasswordGenerator totpGenerator;
    private final Base32 base32 = new Base32();

    public MfaServiceImpl() {
        this.totpGenerator = new TimeBasedOneTimePasswordGenerator(TIME_STEP, DIGITS);
    }

    @Override
    public MfaSetupResponse generateSetup(String issuer, String username) {
        byte[] secretBytes = new byte[SECRET_BYTES];
        new SecureRandom().nextBytes(secretBytes);
        String secretBase32 = base32.encodeAsString(secretBytes).replace("=", "");

        String label = issuer + ":" + username;
        String qrCodeUrl = "otpauth://totp/"
                + URLEncoder.encode(label, StandardCharsets.UTF_8)
                + "?secret=" + secretBase32
                + "&issuer=" + URLEncoder.encode(issuer, StandardCharsets.UTF_8)
                + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + TIME_STEP.getSeconds();

        return MfaSetupResponse.builder()
                .secret(secretBase32)
                .qrCodeUrl(qrCodeUrl)
                .build();
    }

    @Override
    public boolean verifyCode(String secretBase32, String code) {
        if (secretBase32 == null || code == null || code.length() != DIGITS) {
            return false;
        }
        try {
            String padded = secretBase32;
            while (padded.length() % 8 != 0) {
                padded += "=";
            }
            byte[] keyBytes = base32.decode(padded);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "HmacSHA1");
            Instant now = Instant.now();
            for (long diff = -1; diff <= 1; diff++) {
                Instant t = now.plusSeconds(diff * TIME_STEP.getSeconds());
                int expected = totpGenerator.generateOneTimePassword(key, t);
                if (String.format("%06d", expected).equals(code)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.debug("MFA verify failed: {}", e.getMessage());
            return false;
        }
    }
}
