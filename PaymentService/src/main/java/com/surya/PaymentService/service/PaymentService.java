package com.surya.PaymentService.service;

import com.surya.PaymentService.model.PaymentRequest;
import com.surya.PaymentService.model.PaymentResponse;

public interface PaymentService {
    long doPayment(PaymentRequest paymentRequest);

    PaymentResponse getPaymentDetailsByOrderId(String orderId);
}
