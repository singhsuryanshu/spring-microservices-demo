package com.surya.OrderService.service;

import com.surya.OrderService.entity.Order;
import com.surya.OrderService.exception.CustomException;
import com.surya.OrderService.external.client.PaymentService;
import com.surya.OrderService.external.client.ProductService;
import com.surya.OrderService.external.request.PaymentRequest;
import com.surya.OrderService.external.response.PaymentResponse;
import com.surya.OrderService.external.response.ProductResponse;
import com.surya.OrderService.model.OrderRequest;
import com.surya.OrderService.model.OrderResponse;
import com.surya.OrderService.model.PaymentMode;
import com.surya.OrderService.repository.OrderRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Optional;

@SpringBootTest
public class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    OrderService orderService = new OrderServiceImpl();

    @Test
    @DisplayName("Get Order - Success Scenario")
    void test_When_Order_Success(){
        //mocking internal calls

        Order order = getMockOrder();
        Mockito.when(orderRepository.findById(ArgumentMatchers.anyLong()))
                .thenReturn(Optional.of(order));

        Mockito.when(restTemplate.getForObject(
                "http://PRODUCT-SERVICE/product/" + order.getProductId(),
                ProductResponse.class
        )).thenReturn(getMockProductResponse());

        Mockito.when(restTemplate.getForObject(
                "http://PAYMENT-SERVICE/payment/order/" + order.getId(),
                PaymentResponse.class
        )).thenReturn(getMockPaymentResponse());
        // actual function call
        OrderResponse orderResponse = orderService.getOrderDetails(1);

        // verification
        Mockito.verify(orderRepository , Mockito.times(1)).findById(ArgumentMatchers.anyLong());
        Mockito.verify(restTemplate,Mockito.times(1)).getForObject(
                "http://PRODUCT-SERVICE/product/" + order.getProductId(),
                ProductResponse.class
        );
        Mockito.verify(restTemplate,Mockito.times(1)).getForObject(
                "http://PAYMENT-SERVICE/payment/order/" + order.getId(),
                PaymentResponse.class
        );
        // assert
        Assertions.assertNotNull(orderResponse);
        Assertions.assertEquals(order.getId(), orderResponse.getOrderId());

    }

    @DisplayName("Get Order - Failure Scenario")
    @Test
    void test_When_Get_Order_NOT_FOUND_then_Not_Found(){

        Mockito.when(orderRepository.findById(ArgumentMatchers.anyLong())).thenReturn(Optional.ofNullable(null));

        CustomException exception =
                Assertions.assertThrows(CustomException.class ,
                        () -> orderService.getOrderDetails(1));

        Assertions.assertEquals("NOT_FOUND", exception.getErrorCode());
        Assertions.assertEquals(404, exception.getStatus());

        Mockito.verify(orderRepository, Mockito.times(1))
                .findById(ArgumentMatchers.anyLong());
    }

    @DisplayName("Place Order - Success scenario")
    @Test
    void test_When_Place_Order_Success(){
        Order order = getMockOrder();
        OrderRequest orderRequest = getMockOrderRequest();

        Mockito.when(orderRepository.save(ArgumentMatchers.any(Order.class)))
                .thenReturn(order);
        Mockito.when(productService.reduceQuantity(ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong()))
                .thenReturn(new ResponseEntity<Void>(HttpStatus.OK));
        Mockito.when(paymentService.doPayment(ArgumentMatchers.any(PaymentRequest.class)))
                .thenReturn(new ResponseEntity<Long>(1L, HttpStatus.OK));

        long orderId = orderService.placeOrder(orderRequest);

        Mockito.verify(orderRepository, Mockito.times(2))
                .save(ArgumentMatchers.any());
        Mockito.verify(productService, Mockito.times(1))
                .reduceQuantity(ArgumentMatchers.anyLong(),ArgumentMatchers.anyLong());
        Mockito.verify(paymentService, Mockito.times(1))
                .doPayment(ArgumentMatchers.any(PaymentRequest.class));

        Assertions.assertEquals(order.getId(), orderId);
    }

    @DisplayName("Place order - failure scenario")
    @Test
    void test_When_Place_Order_Payment_Fails_then_Order_Placed(){
        Order order = getMockOrder();
        OrderRequest orderRequest = getMockOrderRequest();

        Mockito.when(orderRepository.save(ArgumentMatchers.any(Order.class)))
                .thenReturn(order);
        Mockito.when(productService.reduceQuantity(ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong()))
                .thenReturn(new ResponseEntity<Void>(HttpStatus.OK));
        Mockito.when(paymentService.doPayment(ArgumentMatchers.any(PaymentRequest.class)))
                .thenThrow(new RuntimeException());

        long orderId = orderService.placeOrder(orderRequest);

        Mockito.verify(orderRepository, Mockito.times(2))
                .save(ArgumentMatchers.any());
        Mockito.verify(productService, Mockito.times(1))
                .reduceQuantity(ArgumentMatchers.anyLong(),ArgumentMatchers.anyLong());
        Mockito.verify(paymentService, Mockito.times(1))
                .doPayment(ArgumentMatchers.any(PaymentRequest.class));

        Assertions.assertEquals(order.getId(), orderId);
    }

    private OrderRequest getMockOrderRequest() {
        OrderRequest orderRequest =
                OrderRequest.builder()
                        .productId(1)
                        .paymentMode(PaymentMode.CASH)
                        .quantity(10)
                        .totalAmount(200)
                        .build();
        return orderRequest;
    }

    private PaymentResponse getMockPaymentResponse() {
        return PaymentResponse.builder()
                .paymentId(1)
                .paymentDate(Instant.now())
                .paymentMode(PaymentMode.CASH)
                .orderId(1)
                .amount(200)
                .status("ACCEPTED")
                .build();
    }

    private ProductResponse getMockProductResponse() {
        return ProductResponse.builder()
                .productId(2)
                .productName("iPhone")
                .price(100)
                .quantity(200)
                .build();
    }

    private Order getMockOrder() {
        Order order = Order.builder()
                .orderStatus("PLACED")
                .orderDate(Instant.now())
                .amount(200)
                .quantity(100)
                .id(1)
                .build();
        return order;
    }
}