package com.mp.karental.payment.service;

import com.mp.karental.exception.AppException;
import com.mp.karental.exception.ErrorCode;
import com.mp.karental.payment.constant.VNPayIPNResponseConst;
import com.mp.karental.payment.constant.VNPayParams;
import com.mp.karental.payment.dto.response.IpnResponse;
import com.mp.karental.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class VNPayIpnHandler implements IpnHandler {

    private final VNPayService vnPayService;
    private final TransactionRepository transactionRepository;

    @Override
    public IpnResponse process(Map<String, String> params) {
        if (!vnPayService.verifyIpn(params)) {
            throw new AppException(ErrorCode.VNPAY_CHECKSUM_FAILED);
        }

        String txnRef = params.get(VNPayParams.TXN_REF);
        if (txnRef == null || txnRef.isEmpty()) {
            log.warn("VNPay IPN: Missing transaction reference");
            return VNPayIPNResponseConst.UNKNOWN_ERROR;
        }

        // Check if transaction exists
        if (!transactionRepository.existsById(txnRef)) {
            log.warn("VNPay IPN: Transaction not found: {}", txnRef);
            return VNPayIPNResponseConst.ORDER_NOT_FOUND;
        }

        // Check VNPay response code - "00" means success, others are errors
        String vnpResponseCode = params.get(VNPayParams.RESPONSE_CODE);
        String vnpTransactionStatus = params.get(VNPayParams.TRANSACTION_STATUS);
        
        // VNPay may send either vnp_ResponseCode or vnp_TransactionStatus
        // vnp_TransactionStatus: "00" = success, "02" = failed, "01" = pending
        // vnp_ResponseCode: "00" = success, other codes = various errors
        
        boolean isSuccess = false;
        String statusCode = null;
        String statusMessage = null;
        
        if (vnpResponseCode != null && !vnpResponseCode.isEmpty()) {
            // Primary: Check vnp_ResponseCode
            statusCode = vnpResponseCode;
            isSuccess = "00".equals(vnpResponseCode);
            statusMessage = isSuccess ? "Successful" : "VNPay error: " + vnpResponseCode;
            log.info("VNPay IPN: Using vnp_ResponseCode={} for transaction: {}", vnpResponseCode, txnRef);
        } else if (vnpTransactionStatus != null && !vnpTransactionStatus.isEmpty()) {
            // Fallback: Check vnp_TransactionStatus if vnp_ResponseCode is missing
            statusCode = vnpTransactionStatus;
            isSuccess = "00".equals(vnpTransactionStatus);
            statusMessage = isSuccess ? "Successful" : "Transaction status: " + vnpTransactionStatus;
            log.info("VNPay IPN: Using vnp_TransactionStatus={} for transaction: {} (vnp_ResponseCode missing)", 
                    vnpTransactionStatus, txnRef);
        } else {
            // Both are missing - this is an error
            log.warn("VNPay IPN: Missing both response code and transaction status for transaction: {}", txnRef);
            log.warn("VNPay IPN: Available params: {}", params.keySet());
            return VNPayIPNResponseConst.UNKNOWN_ERROR;
        }
        
        if (isSuccess) {
            log.info("VNPay IPN: Transaction successful: {}", txnRef);
            return VNPayIPNResponseConst.SUCCESS;
        } else {
            // Transaction failed
            log.warn("VNPay IPN: Transaction failed - statusCode={}, message={}, transaction: {}", 
                    statusCode, statusMessage, txnRef);
            return new IpnResponse(statusCode, statusMessage);
        }
    }
}
