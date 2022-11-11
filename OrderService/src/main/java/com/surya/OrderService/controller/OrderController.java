package com.surya.OrderService.controller;

import com.netflix.discovery.converters.Auto;
import com.surya.OrderService.model.OrderRequest;
import com.surya.OrderService.model.OrderResponse;
import com.surya.OrderService.service.OrderService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@Log4j2
public class OrderController {

    //object of business layer
    @Autowired
    private OrderService orderService;

    @PreAuthorize("hasAuthority('Customer')")
    @PostMapping("/placeOrder")
    public ResponseEntity<Long> placeOrder(@RequestBody OrderRequest orderRequest){
        long orderId = orderService.placeOrder(orderRequest);
        log.info("Order id: {}", orderId);
        return new ResponseEntity<>(orderId, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('Customer') || hasAuthority('Admin')")
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderDetails(@PathVariable long orderId){
        OrderResponse orderResponse =
                orderService.getOrderDetails(orderId);

        return new ResponseEntity<>(orderResponse,HttpStatus.OK);
    }
}
