package com.surya.OrderService.service;

import brave.messaging.ProducerResponse;
import com.surya.OrderService.entity.Order;
import com.surya.OrderService.exception.CustomException;
import com.surya.OrderService.external.client.PaymentService;
import com.surya.OrderService.external.client.ProductService;
import com.surya.OrderService.external.request.PaymentRequest;
import com.surya.OrderService.external.response.PaymentResponse;
import com.surya.OrderService.external.response.ProductResponse;
import com.surya.OrderService.model.OrderRequest;
import com.surya.OrderService.model.OrderResponse;
import com.surya.OrderService.repository.OrderRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@Log4j2
public class OrderServiceImpl implements OrderService{

    // we need the object of the repository to connect with it to save the data.
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RestTemplate restTemplate;
    @Override
    public long placeOrder(OrderRequest orderRequest) {
        // create order entity -> save the data with status order created
        // call product service -> block the products(reduce quantity)
        // payment service -> payment -> success -> (Completed , Else Cancelled)

        log.info("Placing order request: {}", orderRequest);

        productService.reduceQuantity(orderRequest.getProductId(),orderRequest.getQuantity());
        log.info("Creating order with status CREATED");
        Order order = Order.builder()
                .productId(orderRequest.getProductId())
                .amount(orderRequest.getTotalAmount())
                .orderDate(Instant.now())
                .orderStatus("CREATED")
                .quantity(orderRequest.getQuantity())
                .build();

        order = orderRepository.save(order);

        log.info("Calling the payment service to complete the payment");

        PaymentRequest paymentRequest =
                PaymentRequest.builder()
                        .orderId(order.getId())
                        .amount(orderRequest.getTotalAmount())
                        .paymentMode(orderRequest.getPaymentMode())
                        .build();

        String orderStatus = null;

        try{
            paymentService.doPayment(paymentRequest);
            log.info("Payment done successfully, changing order status to PLACED");
            orderStatus = "PLACED";
        }catch (Exception e){
            log.error("Error occurred in payment, changing order status to PAYMENT_FAILED");
            orderStatus = "PAYMENT_FAILED";
        }

        order.setOrderStatus(orderStatus);
        orderRepository.save(order);

        log.info("Order placed successfully with order id : {}", order.getId());
        return order.getId();
    }

    @Override
    public OrderResponse getOrderDetails(long orderId) {
        log.info("Get order details for order id : {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(
                        "Order not found for the order id : " + orderId,
                        "NOT_FOUND",
                        404
                ));

        log.info("Invoking the product service to the get the product for id : {}", order.getProductId());
        ProductResponse productResponse =
                restTemplate.getForObject(
                        "http://PRODUCT-SERVICE/product/" + order.getProductId(),
                        ProductResponse.class
                );

        log.info("Getting payment details from the payment service");
        PaymentResponse paymentResponse =
                restTemplate.getForObject(
                        "http://PAYMENT-SERVICE/payment/order/" + orderId,
                        PaymentResponse.class
                );

        OrderResponse.ProductDetails productDetails =
                OrderResponse.ProductDetails.builder()
                        .productId(productResponse.getProductId())
                        .productName(productResponse.getProductName())
                        .build();

        OrderResponse.PaymentDetails paymentDetails =
                OrderResponse.PaymentDetails.builder()
                        .paymentId(paymentResponse.getPaymentId())
                        .paymentStatus(paymentResponse.getStatus())
                        .paymentDate(paymentResponse.getPaymentDate())
                        .paymentMode(paymentResponse.getPaymentMode())
                        .build();

        OrderResponse orderResponse =
                OrderResponse.builder()
                        .orderId(order.getId())
                        .orderStatus(order.getOrderStatus())
                        .orderDate(order.getOrderDate())
                        .amount(order.getAmount())
                        .productDetails(productDetails)
                        .paymentDetails(paymentDetails)
                        .build();
        return orderResponse;
    }
}
