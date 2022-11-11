package com.surya.ProductService.service;

import com.surya.ProductService.model.ProductRequest;
import com.surya.ProductService.model.ProductResponse;

public interface ProductService {
    long addProduct(ProductRequest productRequest);

    ProductResponse getProductById(long productId);

    void reduceQuantity(long productId, long quantity);
}
