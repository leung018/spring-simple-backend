package com.leungcheng.spring_simple_backend.domain.order;

import com.google.common.collect.ImmutableMap;
import com.leungcheng.spring_simple_backend.domain.Product;
import com.leungcheng.spring_simple_backend.domain.ProductRepository;
import com.leungcheng.spring_simple_backend.domain.User;
import com.leungcheng.spring_simple_backend.domain.UserRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

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

  @Transactional(isolation = Isolation.SERIALIZABLE)
  public Order createOrder(String buyerUserId, PurchaseItems purchaseItems) {
    User buyer = getBuyer(buyerUserId);

    BigDecimal totalCost = BigDecimal.ZERO;

    ImmutableMap<String, Integer> productIdToQuantity = purchaseItems.getProductIdToQuantity();
    if (productIdToQuantity.isEmpty()) {
      throw new IllegalArgumentException("Purchase items cannot be empty");
    }

    for (String productId : productIdToQuantity.keySet()) {
      Product product =
          productRepository
              .findById(productId)
              .orElseThrow(
                  () -> new IllegalArgumentException("Product: " + productId + " does not exist"));

      int purchaseQuantity = productIdToQuantity.get(productId);
      if (purchaseQuantity > product.getQuantity()) {
        throw new IllegalArgumentException("Insufficient stock for product: " + productId);
      }

      product = product.toBuilder().quantity(product.getQuantity() - purchaseQuantity).build();
      productRepository.save(product);
      User seller =
          userRepository
              .findById(product.getUserId())
              .orElseThrow(() -> new RuntimeException("Seller does not exist"));
      saveNewBalance(
          seller,
          seller
              .getBalance()
              .add(product.getPrice().multiply(BigDecimal.valueOf(purchaseQuantity))));

      totalCost = totalCost.add(product.getPrice().multiply(BigDecimal.valueOf(purchaseQuantity)));
    }

    if (buyer.getBalance().compareTo(totalCost) < 0) {
      throw new IllegalArgumentException("Insufficient balance");
    }
    saveNewBalance(buyer, buyer.getBalance().subtract(totalCost));

    return addNewOrder(buyerUserId, purchaseItems);
  }

  private User getBuyer(String buyerUserId) {
    return userRepository
        .findById(buyerUserId)
        .orElseThrow(() -> new IllegalArgumentException("Buyer does not exist"));
  }

  private void saveNewBalance(User buyer, BigDecimal newBalance) {
    User updatedBuyer = buyer.toBuilder().balance(newBalance).build();
    userRepository.save(updatedBuyer);
  }

  private Order addNewOrder(String buyerUserId, PurchaseItems purchaseItems) {
    Order order = new Order(buyerUserId, purchaseItems);
    return orderRepository.save(order);
  }
}
