package com.surya.OrderService.service;

import com.surya.OrderService.model.OrderRequest;
import com.surya.OrderService.model.OrderResponse;

public interface OrderService {
    long placeOrder(OrderRequest orderRequest);

    OrderResponse getOrderDetails(long orderId);
}
