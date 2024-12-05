package com.leungcheng.spring_simple_backend.domain.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.leungcheng.spring_simple_backend.domain.Product;
import com.leungcheng.spring_simple_backend.domain.ProductRepository;
import com.leungcheng.spring_simple_backend.domain.User;
import com.leungcheng.spring_simple_backend.domain.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class OrderServiceTest {
  private @Autowired UserRepository userRepository;
  private @Autowired ProductRepository productRepository;
  private @Autowired OrderRepository orderRepository;
  private OrderService orderService;

  @BeforeEach
  void setUp() {
    orderRepository.deleteAll();
    userRepository.deleteAll();
    productRepository.deleteAll();

    orderService = new OrderService(userRepository, productRepository, orderRepository);
  }

  private static Product.Builder productBuilder() {
    return new Product.Builder().name("Default Product").price(new BigDecimal(20)).quantity(10);
  }

  private static User.Builder userBuilder() {
    return new User.Builder().username("user01").password("password").balance(new BigDecimal(100));
  }

  @Test
  void shouldRejectCreateOrderWithNonExistingUser() {
    Product product = productBuilder().build();
    productRepository.save(product);

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product.getId(), 1);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> orderService.createOrder("non_existing_user_id", purchaseItems));
    assertEquals("User does not exist", exception.getMessage());
  }

  @Test
  void shouldRejectCreateOrderWithNonExistingProduct() {
    User user = userBuilder().build();
    userRepository.save(user);

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem("non_existing_product_id", 1);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> orderService.createOrder(user.getId(), purchaseItems));
    assertEquals("Product: non_existing_product_id does not exist", exception.getMessage());
  }

  @Test
  void shouldRejectCreateOrderWithInsufficientBalance() {
    User user = userBuilder().balance(new BigDecimal(9)).build();
    userRepository.save(user);

    Product product = productBuilder().price(new BigDecimal(5)).quantity(999).build();
    productRepository.save(product);

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product.getId(), 2);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> orderService.createOrder(user.getId(), purchaseItems));
    assertEquals("Insufficient balance", exception.getMessage());
  }

  @Test
  void shouldRejectOrderWithInsufficientProductQuantity() {
    User user = userBuilder().balance(new BigDecimal(999)).build();
    userRepository.save(user);

    Product product = productBuilder().quantity(1).price(BigDecimal.ONE).build();
    productRepository.save(product);

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product.getId(), 2);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> orderService.createOrder(user.getId(), purchaseItems));
    assertEquals("Insufficient stock for product: " + product.getId(), exception.getMessage());
  }

  @Test
  void shouldReduceProductQuantityAndBuyerBalanceWhenOrderIsSuccessful() {
    User buyer = userBuilder().balance(new BigDecimal(25)).build();
    userRepository.save(buyer);

    Product product1 = productBuilder().quantity(10).price(new BigDecimal("5.2")).build();
    Product product2 = productBuilder().quantity(10).price(new BigDecimal("3.5")).build();
    productRepository.save(product1);
    productRepository.save(product2);

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product1.getId(), 2);
    purchaseItems.setPurchaseItem(product2.getId(), 3);

    orderService.createOrder(buyer.getId(), purchaseItems);

    assertEquals(8, productRepository.findById(product1.getId()).get().getQuantity());
    assertEquals(7, productRepository.findById(product2.getId()).get().getQuantity());

    assertEquals(
        new BigDecimal("4.1"),
        userRepository.findById(buyer.getId()).get().getBalance()); // 20 - (5.2 * 2 + 3.5 * 3)
  }
}
