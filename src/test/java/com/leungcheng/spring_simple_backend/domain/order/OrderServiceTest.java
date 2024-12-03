package com.leungcheng.spring_simple_backend.domain.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.leungcheng.spring_simple_backend.domain.Product;
import com.leungcheng.spring_simple_backend.domain.ProductRepository;
import com.leungcheng.spring_simple_backend.domain.User;
import com.leungcheng.spring_simple_backend.domain.UserRepository;
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
    return new Product.Builder().name("Default Product").price(20).quantity(10);
  }

  private static User.Builder userBuilder() {
    return new User.Builder().username("user01").password("password").balance(100);
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
}
