package com.leungcheng.spring_simple_backend.domain.order;

import com.google.common.collect.ImmutableMap;
import com.leungcheng.spring_simple_backend.domain.Product;
import com.leungcheng.spring_simple_backend.domain.ProductRepository;
import com.leungcheng.spring_simple_backend.domain.User;
import com.leungcheng.spring_simple_backend.domain.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
  private final UserRepository userRepository;
  private final ProductRepository productRepository;
  private final OrderRepository orderRepository;

  public OrderService(
      UserRepository userRepository,
      ProductRepository productRepository,
      OrderRepository orderRepository) {
    this.userRepository = userRepository;
    this.productRepository = productRepository;
    this.orderRepository = orderRepository;
  }

  public Order createOrder(String userId, PurchaseItems purchaseItems) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User does not exist"));

    double totalCost = 0;
    ImmutableMap<String, Integer> productIdToQuantity = purchaseItems.getProductIdToQuantity();
    for (String productId : productIdToQuantity.keySet()) {
      Product product =
          productRepository
              .findById(productId)
              .orElseThrow(
                  () -> new IllegalArgumentException("Product: " + productId + " does not exist"));
      totalCost += product.getPrice() * productIdToQuantity.get(productId);
    }

    if (user.getBalance() < totalCost) {
      throw new IllegalArgumentException("Insufficient balance");
    }

    throw new UnsupportedOperationException("Not implemented");
  }
}
