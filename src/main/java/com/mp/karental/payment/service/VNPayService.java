package com.mp.karental.payment.service;

import com.mp.karental.payment.configuration.PaymentConfig;
import com.mp.karental.payment.constant.VNPayParams;
import com.mp.karental.payment.dto.request.InitPaymentRequest;
import com.mp.karental.payment.dto.response.InitPaymentResponse;
import com.mp.karental.payment.util.DateUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class VNPayService implements PaymentService{
    //according to VNPAY rules
    public static final String VERSION = "2.1.0";

    public static final String ORDER_TYPE = "190000";
    public static final long DEFAULT_MULTIPLIER = 100L;

    //get instance from payment config
    private final  PaymentConfig paymentConfig;

    private String tmnCode ;

    private String initPaymentPrefixUrl;

    private String returnUrlFormat;

    private Integer paymentTimeout;

    @PostConstruct
    public void init() {
        this.tmnCode = paymentConfig.getTmnCode();
        this.initPaymentPrefixUrl = paymentConfig.getInitPaymentUrl();
        this.returnUrlFormat = paymentConfig.getReturnUrl();
        this.paymentTimeout = paymentConfig.getTimeout();

        // Validate configuration
        validateConfiguration();

        log.info("VNPay Config Loaded: tmnCode={}, initPaymentUrl={}, returnUrlFormat={}, timeout={} minutes", 
                tmnCode, initPaymentPrefixUrl, returnUrlFormat, paymentTimeout);
    }
    
    /**
     * Validate VNPay configuration to ensure all required fields are set
     */
    private void validateConfiguration() {
        if (tmnCode == null || tmnCode.isEmpty()) {
            log.error("VNPay TMN Code is not configured! Payment URL generation will fail.");
            throw new IllegalStateException("VNPay TMN Code must be configured");
        }
        if (paymentConfig.getSecretKey() == null || paymentConfig.getSecretKey().isEmpty()) {
            log.error("VNPay Secret Key is not configured! Payment URL generation will fail.");
            throw new IllegalStateException("VNPay Secret Key must be configured");
        }
        if (initPaymentPrefixUrl == null || initPaymentPrefixUrl.isEmpty()) {
            log.error("VNPay Init Payment URL is not configured! Payment URL generation will fail.");
            throw new IllegalStateException("VNPay Init Payment URL must be configured");
        }
        if (returnUrlFormat == null || returnUrlFormat.isEmpty()) {
            log.error("VNPay Return URL format is not configured! Payment URL generation will fail.");
            throw new IllegalStateException("VNPay Return URL format must be configured");
        }
        if (paymentTimeout == null || paymentTimeout <= 0) {
            log.warn("VNPay payment timeout is not set or invalid, using default: 15 minutes");
            this.paymentTimeout = 15;
        }
    }

    private final CryptoService cryptoService;

    @Override
    public InitPaymentResponse initPayment(InitPaymentRequest request) {
        try {
            // Validate request
            if (request == null) {
                throw new IllegalArgumentException("InitPaymentRequest cannot be null");
            }
            if (request.getTxnRef() == null || request.getTxnRef().isEmpty()) {
                throw new IllegalArgumentException("Transaction reference cannot be null or empty");
            }
            if (request.getAmount() <= 0) {
                throw new IllegalArgumentException("Amount must be greater than 0");
            }
            
            var amount = request.getAmount() * DEFAULT_MULTIPLIER;  // 1. amount * 100
            var txnRef = request.getTxnRef();                       // 2. transactionId
            var returnUrl = buildReturnUrl(txnRef);                 // 3. FE redirect by returnUrl
            
            // Get current time in Vietnam timezone (GMT+7) - DO NOT add 7 hours as calendar is already in GMT+7
            var vnCalendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            var createdDate = DateUtils.formatVnTime(vnCalendar);
            vnCalendar.add(Calendar.MINUTE, paymentTimeout);
            var expiredDate = DateUtils.formatVnTime(vnCalendar);    // 4. expiredDate for secure
            
            var orderInfo = String.format("Top-Up transaction %s", request.getTxnRef());
            // Normalize IP address - convert IPv6 localhost to IPv4 if needed
            var ipAddress = normalizeIpAddress(request.getIpAddress());
            var requestId = request.getRequestId();

            // Validate critical values
            if (createdDate == null || createdDate.length() != 14) {
                throw new IllegalStateException("Invalid created date format: " + createdDate);
            }
            if (expiredDate == null || expiredDate.length() != 14) {
                throw new IllegalStateException("Invalid expired date format: " + expiredDate);
            }
            if (returnUrl == null || returnUrl.isEmpty()) {
                throw new IllegalStateException("Return URL cannot be null or empty");
            }

            Map<String, String> params = new HashMap<>();

            params.put(VNPayParams.VERSION, VERSION);
            params.put(VNPayParams.COMMAND, "pay");
            params.put(VNPayParams.TMN_CODE, tmnCode);
            params.put(VNPayParams.AMOUNT, String.valueOf(amount));
            params.put(VNPayParams.CURRENCY, "VND");

            params.put(VNPayParams.TXN_REF, txnRef);
            params.put(VNPayParams.RETURN_URL, returnUrl);

            params.put(VNPayParams.CREATED_DATE, createdDate);
            params.put(VNPayParams.EXPIRE_DATE, expiredDate);

            params.put(VNPayParams.IP_ADDRESS, ipAddress);
            params.put(VNPayParams.LOCALE, "vn");
            
            // Bank code is not set - VNPay will show payment method selection page to users
            // This allows users to choose their preferred payment method (ATM, QR, etc.)
            
            params.put(VNPayParams.ORDER_INFO, orderInfo);
            params.put(VNPayParams.ORDER_TYPE, ORDER_TYPE);
            
            // Build and validate payment URL
            var initPaymentUrl = buildInitPaymentUrl(params);
            
            // Validate the generated URL
            if (initPaymentUrl == null || initPaymentUrl.isEmpty()) {
                throw new IllegalStateException("Generated payment URL is null or empty");
            }
            if (!initPaymentUrl.startsWith("http://") && !initPaymentUrl.startsWith("https://")) {
                throw new IllegalStateException("Invalid payment URL format: " + initPaymentUrl);
            }
            
            log.info("[request_id={}] Payment URL generated successfully for transaction: {}", requestId, txnRef);
            log.debug("[request_id={}] Payment URL: {}", requestId, initPaymentUrl);
            
            return InitPaymentResponse.builder()
                    .vnpUrl(initPaymentUrl)
                    .build();
        } catch (Exception e) {
            log.error("Error generating VNPay payment URL for transaction: {}", 
                    request != null ? request.getTxnRef() : "unknown", e);
            throw e;
        }
    }
    public boolean verifyIpn(Map<String, String> params) {
        var reqSecureHash = params.get(VNPayParams.SECURE_HASH);
        if (reqSecureHash == null || reqSecureHash.isEmpty()) {
            log.warn("VNPay IPN verification: Missing secure hash");
            return false;
        }
        
        // Create a copy to avoid modifying the original map
        Map<String, String> paramsCopy = new HashMap<>(params);
        paramsCopy.remove(VNPayParams.SECURE_HASH);
        paramsCopy.remove(VNPayParams.SECURE_HASH_TYPE);
        
        // Filter and sort parameters - only include non-empty values
        List<String> fieldNames = new ArrayList<>();
        for (String key : paramsCopy.keySet()) {
            String value = paramsCopy.get(key);
            if (value != null && !value.isEmpty()) {
                fieldNames.add(key);
            }
        }
        Collections.sort(fieldNames);

        // Build hash payload
        StringBuilder hashPayload = new StringBuilder();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = paramsCopy.get(fieldName);
            
            if (i > 0) {
                hashPayload.append("&");
            }
            hashPayload.append(fieldName);
            hashPayload.append("=");
            hashPayload.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
        }

        var secureHash = cryptoService.sign(hashPayload.toString());
        boolean isValid = secureHash.equals(reqSecureHash);
        
        if (!isValid) {
            log.error("VNPay IPN verification failed - Hash mismatch. Expected: {}, Got: {}", reqSecureHash, secureHash);
        }
        
        return isValid;
    }
    private String buildReturnUrl(String txnRef) {
        if (txnRef == null || txnRef.isEmpty()) {
            throw new IllegalArgumentException("Transaction reference cannot be null or empty for return URL");
        }
        if (returnUrlFormat == null || returnUrlFormat.isEmpty()) {
            throw new IllegalStateException("Return URL format is not configured");
        }
        try {
            String returnUrl = String.format(returnUrlFormat, txnRef);
            if (returnUrl == null || returnUrl.isEmpty()) {
                throw new IllegalStateException("Generated return URL is null or empty");
            }
            return returnUrl;
        } catch (Exception e) {
            log.error("Error building return URL for transaction: {}", txnRef, e);
            throw new IllegalStateException("Failed to build return URL: " + e.getMessage(), e);
        }
    }
    
    /**
     * Normalize IP address for VNPay compatibility
     * Converts IPv6 localhost (0:0:0:0:0:0:0:1 or ::1) to IPv4 (127.0.0.1)
     */
    private String normalizeIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return "127.0.0.1";
        }
        // Convert IPv6 localhost to IPv4
        if (ipAddress.equals("0:0:0:0:0:0:0:1") || ipAddress.equals("::1")) {
            return "127.0.0.1";
        }
        return ipAddress;
    }

    @SneakyThrows
    private String buildInitPaymentUrl(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("Parameters cannot be null or empty");
        }
        
        // Filter and sort parameters - only include non-empty values
        List<String> fieldNames = new ArrayList<>();
        for (String key : params.keySet()) {
            String value = params.get(key);
            if (value != null && !value.isEmpty()) {
                fieldNames.add(key);
            }
        }
        
        if (fieldNames.isEmpty()) {
            throw new IllegalArgumentException("No valid parameters to build payment URL");
        }
        
        Collections.sort(fieldNames);   // 1. Sort field names alphabetically

        // Build hash payload and query string
        StringBuilder hashPayload = new StringBuilder();
        StringBuilder query = new StringBuilder();
        
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = params.get(fieldName);
            
            if (fieldValue == null || fieldValue.isEmpty()) {
                continue; // Skip empty values
            }
            
            // URL encode the field value for hash calculation
            String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII);
            
            // Build hash data: fieldName=encodedValue
            if (hashPayload.length() > 0) {
                hashPayload.append("&");
            }
            hashPayload.append(fieldName);
            hashPayload.append("=");
            hashPayload.append(encodedValue);
            
            // Build query string: URL encode both field name and value
            if (query.length() > 0) {
                query.append("&");
            }
            query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
            query.append("=");
            query.append(encodedValue);
        }

        // Validate hash payload
        if (hashPayload.length() == 0) {
            throw new IllegalStateException("Hash payload is empty - cannot generate secure hash");
        }

        // Calculate secure hash from hash data
        String hashDataString = hashPayload.toString();
        log.debug("VNPay Payment URL - Hash data string: {}", hashDataString);
        log.debug("VNPay Payment URL - Parameters count: {}", fieldNames.size());
        
        var secureHash = cryptoService.sign(hashDataString);
        
        if (secureHash == null || secureHash.isEmpty()) {
            throw new IllegalStateException("Secure hash is null or empty");
        }
        
        log.debug("VNPay Payment URL - Secure hash: {}", secureHash);

        // Append secure hash to query string (NOT URL encoded)
        query.append("&vnp_SecureHash=");
        query.append(secureHash);

        String finalUrl = initPaymentPrefixUrl + "?" + query.toString();
        
        // Validate final URL
        if (finalUrl.length() > 2000) {
            log.warn("Payment URL is very long ({} characters) - may cause issues", finalUrl.length());
        }
        
        log.debug("VNPay Payment URL - Final URL length: {} characters", finalUrl.length());
        return finalUrl;
    }
}
