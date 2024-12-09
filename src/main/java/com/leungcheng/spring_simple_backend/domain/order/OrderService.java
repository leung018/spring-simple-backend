package com.leungcheng.spring_simple_backend.domain.order;

import com.google.common.collect.ImmutableMap;
import com.leungcheng.spring_simple_backend.domain.Product;
import com.leungcheng.spring_simple_backend.domain.ProductRepository;
import com.leungcheng.spring_simple_backend.domain.User;
import com.leungcheng.spring_simple_backend.domain.UserRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
  private @Autowired UserRepository userRepository;
  private @Autowired ProductRepository productRepository;
  private @Autowired OrderRepository orderRepository;

  public static class CreateOrderException extends IllegalArgumentException {
    public CreateOrderException(String message) {
      super(message);
    }
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  public Order createOrder(String buyerUserId, PurchaseItems purchaseItems) {
    User buyer =
        getUser(buyerUserId).orElseThrow(() -> new CreateOrderException("Buyer does not exist"));

    BigDecimal totalCost = processPurchaseItems(purchaseItems);

    if (buyer.getBalance().compareTo(totalCost) < 0) {
      throw new CreateOrderException("Insufficient balance");
    }
    saveNewBalance(buyer, buyer.getBalance().subtract(totalCost));

    return addNewOrder(buyerUserId, purchaseItems);
  }

  private Optional<User> getUser(String userId) {
    return userRepository.findById(userId);
  }

  private void saveNewBalance(User buyer, BigDecimal newBalance) {
    User updatedBuyer = buyer.toBuilder().balance(newBalance).build();
    userRepository.save(updatedBuyer);
  }

  private Order addNewOrder(String buyerUserId, PurchaseItems purchaseItems) {
    Order order = new Order(buyerUserId, purchaseItems);
    return orderRepository.save(order);
  }

  private BigDecimal processPurchaseItems(PurchaseItems purchaseItems) {
    ImmutableMap<String, Integer> productIdToQuantity = purchaseItems.getProductIdToQuantity();
    if (productIdToQuantity.isEmpty()) {
      throw new CreateOrderException("Purchase items cannot be empty");
    }

    BigDecimal totalCost = BigDecimal.ZERO;
    for (Map.Entry<String, Integer> entry : productIdToQuantity.entrySet()) {
      String productId = entry.getKey();
      int purchaseQuantity = entry.getValue();
      Product product = getProduct(productId);

      reduceProductStock(product, purchaseQuantity);
      addProfitToSeller(product, purchaseQuantity);

      BigDecimal itemCost = product.getPrice().multiply(BigDecimal.valueOf(purchaseQuantity));
      totalCost = totalCost.add(itemCost);
    }
    return totalCost;
  }

  private void reduceProductStock(Product product, int purchaseQuantity) {
    if (purchaseQuantity > product.getQuantity()) {
      throw new CreateOrderException("Insufficient stock for product: " + product.getId());
    }
    int newQuantity = product.getQuantity() - purchaseQuantity;
    Product updatedProduct = product.toBuilder().quantity(newQuantity).build();
    productRepository.save(updatedProduct);
  }

  private void addProfitToSeller(Product product, int purchaseQuantity) {
    User seller = getUser(product.getUserId()).orElseThrow();
    BigDecimal profit = product.getPrice().multiply(BigDecimal.valueOf(purchaseQuantity));
    BigDecimal newBalance = seller.getBalance().add(profit);
    saveNewBalance(seller, newBalance);
  }

  private Product getProduct(String productId) {
    return productRepository
        .findById(productId)
        .orElseThrow(() -> new CreateOrderException("Product: " + productId + " does not exist"));
  }
}
