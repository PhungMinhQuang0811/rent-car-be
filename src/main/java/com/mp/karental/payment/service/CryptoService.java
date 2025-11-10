package com.mp.karental.payment.service;

import com.mp.karental.exception.AppException;
import com.mp.karental.exception.ErrorCode;
import com.mp.karental.payment.configuration.PaymentConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
@Slf4j
public class CryptoService {

    private final Mac mac;
    private final String secretKey;

    // Inject PaymentConfig through constructor
    public CryptoService(PaymentConfig paymentConfig) throws NoSuchAlgorithmException {
        this.mac = Mac.getInstance("HmacSHA512");
        this.secretKey = paymentConfig.getSecretKey(); // Now secretKey is properly set
    }

    @PostConstruct
    void init() throws InvalidKeyException {
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalStateException("VNPay secret key is not configured");
        }
        // Use UTF-8 encoding for secret key to ensure consistency
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), 
                "HmacSHA512"
        );
        mac.init(secretKeySpec);
        log.info("CryptoService initialized successfully with secret key length: {}", secretKey.length());
    }

    public static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public String sign(String data) {
        if (data == null || data.isEmpty()) {
            log.error("Cannot sign empty or null data");
            throw new IllegalArgumentException("Data to sign cannot be null or empty");
        }
        try {
            // Use UTF-8 encoding for hash calculation as per VNPay documentation
            byte[] dataBytes = data.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] hashBytes = mac.doFinal(dataBytes);
            String hash = toHexString(hashBytes);
            
            if (hash == null || hash.isEmpty()) {
                throw new IllegalStateException("Generated hash is null or empty");
            }
            
            log.debug("Hash calculation - Data length: {} bytes, Hash length: {} chars", 
                    dataBytes.length, hash.length());
            return hash;
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument for hash calculation", e);
            throw e;
        } catch (Exception e) {
            log.error("Error calculating hash for data length: {}", data != null ? data.length() : 0, e);
            throw new AppException(ErrorCode.VNPAY_SIGNING_FAILED);
        }
    }
}
