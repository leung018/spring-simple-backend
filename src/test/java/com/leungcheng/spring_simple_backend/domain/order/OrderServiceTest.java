package com.leungcheng.spring_simple_backend.domain.order;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.leungcheng.spring_simple_backend.domain.ProductRepository;
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

  @Test
  void shouldRejectCreateOrderWithNonExistingUser() {
    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem("product_id", 1);

    assertThrows(
        IllegalArgumentException.class,
        () -> orderService.createOrder("non_existing_user_id", purchaseItems));
  }
}
