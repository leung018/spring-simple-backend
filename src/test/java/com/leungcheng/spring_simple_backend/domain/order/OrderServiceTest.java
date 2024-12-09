package com.leungcheng.spring_simple_backend.domain.order;

import static org.junit.jupiter.api.Assertions.*;

import com.leungcheng.spring_simple_backend.domain.Product;
import com.leungcheng.spring_simple_backend.domain.ProductRepository;
import com.leungcheng.spring_simple_backend.domain.User;
import com.leungcheng.spring_simple_backend.domain.UserRepository;
import java.math.BigDecimal;
import java.util.List;
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

  private Product.Builder productBuilder() {
    return new Product.Builder()
        .name("Default Product")
        .price(new BigDecimal(20))
        .userId(seedSeller.getId())
        .quantity(10);
  }

  private User.Builder userBuilder() {
    return new User.Builder().username("user01").password("password").balance(new BigDecimal(100));
  }

  private final User seedSeller = userBuilder().build();

  @BeforeEach
  void setUp() {
    orderRepository.deleteAll();
    userRepository.deleteAll();
    productRepository.deleteAll();

    userRepository.save(seedSeller);
    orderService = new OrderService(userRepository, productRepository, orderRepository);
  }

  @Test
  void shouldRejectCreateOrderWithNonExistingBuyer() {
    Product product = productBuilder().build();
    productRepository.save(product);

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product.getId(), 1);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> orderService.createOrder("non_existing_buyer_id", purchaseItems));
    assertEquals("Buyer does not exist", exception.getMessage());
  }

  @Test
  void shouldRejectCreateOrderWithEmptyPurchaseItems() {
    User buyer = userBuilder().build();
    userRepository.save(buyer);

    PurchaseItems purchaseItems = new PurchaseItems();

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> orderService.createOrder(buyer.getId(), purchaseItems));
    assertEquals("Purchase items cannot be empty", exception.getMessage());
  }

  @Test
  void shouldRejectCreateOrderWithNonExistingProduct() {
    User buyer = userBuilder().build();
    userRepository.save(buyer);

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem("non_existing_product_id", 1);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> orderService.createOrder(buyer.getId(), purchaseItems));
    assertEquals("Product: non_existing_product_id does not exist", exception.getMessage());
  }

  @Test
  void shouldRejectCreateOrderWithInsufficientBalance() {
    User buyer = userBuilder().balance(new BigDecimal("9.99999")).build();
    userRepository.save(buyer);

    Product product = productBuilder().price(new BigDecimal(5)).quantity(999).build();
    productRepository.save(product);

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product.getId(), 2);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> orderService.createOrder(buyer.getId(), purchaseItems));
    assertEquals("Insufficient balance", exception.getMessage());

    // product quantity should not be reduced
    assertEquals(999, productRepository.findById(product.getId()).orElseThrow().getQuantity());
  }

  @Test
  void shouldRejectOrderWithInsufficientProductQuantity() {
    User buyer = userBuilder().balance(new BigDecimal(999)).build();
    userRepository.save(buyer);

    Product product = productBuilder().quantity(1).price(BigDecimal.ONE).build();
    productRepository.save(product);

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product.getId(), 2);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> orderService.createOrder(buyer.getId(), purchaseItems));
    assertEquals("Insufficient stock for product: " + product.getId(), exception.getMessage());

    // buyer balance should not be reduced
    assertEquals(
        new BigDecimal(999), userRepository.findById(buyer.getId()).orElseThrow().getBalance());
  }

  @Test
  void shouldNotThrowExceptionIfStockAndBuyerBalanceIsJustEnough() {
    User buyer = userBuilder().balance(new BigDecimal(10)).build();
    userRepository.save(buyer);

    Product product = productBuilder().quantity(1).price(new BigDecimal(10)).build();
    productRepository.save(product);

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product.getId(), 1);

    orderService.createOrder(buyer.getId(), purchaseItems); // should not throw exception
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

    assertEquals(8, productRepository.findById(product1.getId()).orElseThrow().getQuantity());
    assertEquals(7, productRepository.findById(product2.getId()).orElseThrow().getQuantity());

    assertEquals(
        new BigDecimal("4.1"),
        userRepository
            .findById(buyer.getId())
            .orElseThrow()
            .getBalance()); // 20 - (5.2 * 2 + 3.5 * 3)
  }

  @Test
  void shouldIncreaseSellersBalance() {
    User buyer = userBuilder().balance(new BigDecimal(999)).build();
    User seller1 = userBuilder().balance(new BigDecimal(5)).build();
    User seller2 = userBuilder().balance(new BigDecimal(10)).build();
    userRepository.saveAll(List.of(buyer, seller1, seller2));

    Product product1 =
        productBuilder().quantity(999).price(new BigDecimal(5)).userId(seller1.getId()).build();
    Product product2 =
        productBuilder().quantity(999).price(new BigDecimal(3)).userId(seller2.getId()).build();
    productRepository.saveAll(List.of(product1, product2));

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product1.getId(), 2);
    purchaseItems.setPurchaseItem(product2.getId(), 3);

    orderService.createOrder(buyer.getId(), purchaseItems);

    assertEquals(
        new BigDecimal(15), userRepository.findById(seller1.getId()).orElseThrow().getBalance());
    assertEquals(
        new BigDecimal(19), userRepository.findById(seller2.getId()).orElseThrow().getBalance());
  }

  @Test
  void shouldCreateOrder() {
    User buyer = userBuilder().balance(new BigDecimal(999)).build();
    userRepository.save(buyer);

    Product product1 = productBuilder().quantity(999).price(new BigDecimal(5)).build();
    Product product2 = productBuilder().quantity(999).price(new BigDecimal(3)).build();
    productRepository.saveAll(List.of(product1, product2));

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product1.getId(), 2);
    purchaseItems.setPurchaseItem(product2.getId(), 5);

    Order order = orderService.createOrder(buyer.getId(), purchaseItems);

    assertEquals(buyer.getId(), order.getBuyerUserId());
    assertEquals(
        purchaseItems.getProductIdToQuantity(), order.getPurchaseItems().getProductIdToQuantity());

    Order savedOrder = orderRepository.findById(order.getId()).orElseThrow();
    assertOrderEquals(order, savedOrder);
  }

  @Test
  void shouldEachCreatedOrderHasDifferentId() {
    User buyer = userBuilder().balance(new BigDecimal(999)).build();
    userRepository.save(buyer);

    Product product = productBuilder().quantity(999).price(new BigDecimal(5)).build();
    productRepository.save(product);

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product.getId(), 1);

    Order order1 = orderService.createOrder(buyer.getId(), purchaseItems);
    Order order2 = orderService.createOrder(buyer.getId(), purchaseItems);

    assertNotEquals(order1.getId(), order2.getId());
  }

  private void assertOrderEquals(Order expected, Order actual) {
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getBuyerUserId(), actual.getBuyerUserId());
    assertEquals(
        expected.getPurchaseItems().getProductIdToQuantity(),
        actual.getPurchaseItems().getProductIdToQuantity());
  }
}
