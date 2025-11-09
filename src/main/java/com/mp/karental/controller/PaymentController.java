package com.mp.karental.controller;

import com.mp.karental.payment.dto.response.IpnResponse;
import com.mp.karental.payment.service.IpnHandler;
import com.mp.karental.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {

    private final IpnHandler ipnHandler;
    private final TransactionService transactionService;

    @Value("${front-end.base-url}")
    private String frontendBaseUrl;

    @GetMapping("/vnpay_ipn")
    IpnResponse processIpn(@RequestParam Map<String, String> params) {
        log.info("[VNPay Ipn] Params: {}", params);
        return ipnHandler.process(params);
    }

    @GetMapping("/vnpay_return/{transactionId}")
    public RedirectView processReturnUrl(
            @PathVariable String transactionId,
            @RequestParam Map<String, String> params) {
        log.info("[VNPay Return] TransactionId: {}, Params: {}", transactionId, params);
        
        // Build redirect URL with VNPay params preserved for frontend
        StringBuilder redirectUrl = new StringBuilder(frontendBaseUrl + "/#/my-wallet");
        
        try {
            // Process the transaction status (no authentication required)
            transactionService.processTransactionFromReturnUrl(transactionId, params);
            
            // Add success status and preserve important VNPay params for frontend
            redirectUrl.append("?status=success");
            redirectUrl.append("&transactionId=").append(URLEncoder.encode(transactionId, StandardCharsets.UTF_8));
            
            // Preserve VNPay response params for frontend validation (URL encoded)
            if (params.containsKey("vnp_ResponseCode")) {
                redirectUrl.append("&vnp_ResponseCode=")
                        .append(URLEncoder.encode(params.get("vnp_ResponseCode"), StandardCharsets.UTF_8));
            }
            if (params.containsKey("vnp_TransactionNo")) {
                redirectUrl.append("&vnp_TransactionNo=")
                        .append(URLEncoder.encode(params.get("vnp_TransactionNo"), StandardCharsets.UTF_8));
            }
            if (params.containsKey("vnp_TxnRef")) {
                redirectUrl.append("&vnp_TxnRef=")
                        .append(URLEncoder.encode(params.get("vnp_TxnRef"), StandardCharsets.UTF_8));
            }
            if (params.containsKey("vnp_Amount")) {
                redirectUrl.append("&vnp_Amount=")
                        .append(URLEncoder.encode(params.get("vnp_Amount"), StandardCharsets.UTF_8));
            }
            
            log.info("[VNPay Return] Redirecting to: {}", redirectUrl.toString());
            return new RedirectView(redirectUrl.toString());
        } catch (Exception e) {
            log.error("[VNPay Return] Error processing return URL for transactionId: {}", transactionId, e);
            
            // Add error status and preserve transaction ID
            redirectUrl.append("?status=error");
            redirectUrl.append("&transactionId=").append(URLEncoder.encode(transactionId, StandardCharsets.UTF_8));
            
            // Preserve error info if available (URL encoded)
            if (params.containsKey("vnp_ResponseCode")) {
                redirectUrl.append("&vnp_ResponseCode=")
                        .append(URLEncoder.encode(params.get("vnp_ResponseCode"), StandardCharsets.UTF_8));
            }
            
            return new RedirectView(redirectUrl.toString());
        }
    }
}