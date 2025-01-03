package com.leungcheng.spring_e_commerce_backend.domain.order;

import static com.leungcheng.spring_e_commerce_backend.testutil.CustomAssertions.assertBigDecimalEquals;
import static org.junit.jupiter.api.Assertions.*;

import com.leungcheng.spring_e_commerce_backend.domain.Product;
import com.leungcheng.spring_e_commerce_backend.domain.ProductRepository;
import com.leungcheng.spring_e_commerce_backend.domain.User;
import com.leungcheng.spring_e_commerce_backend.domain.UserRepository;
import com.leungcheng.spring_e_commerce_backend.domain.order.OrderService.CreateOrderException;
import com.leungcheng.spring_e_commerce_backend.testutil.DefaultBuilders;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderServiceTest {
  private @Autowired UserRepository userRepository;
  private @Autowired ProductRepository productRepository;
  private @Autowired OrderRepository orderRepository;
  private @Autowired OrderService orderService;

  private Product.Builder productBuilder() {
    return new Product.Builder()
        .name("Default Product")
        .price(new BigDecimal(20))
        .userId(seedSeller.getId())
        .quantity(10);
  }

  private User.Builder uniqueUsernameUserBuilder() {
    return DefaultBuilders.userBuilder().username("user" + userCount++);
  }

  private int userCount = 0;

  private final User seedSeller = uniqueUsernameUserBuilder().build();

  private final UUID seedRequestId1 = UUID.randomUUID();
  private final UUID seedRequestId2 = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    orderRepository.deleteAll();
    userRepository.deleteAll();
    productRepository.deleteAll();

    userRepository.save(seedSeller);
  }

  @Test
  void shouldRejectCreateOrderWithNonExistingBuyer() {
    Product product = productBuilder().build();
    productRepository.save(product);

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product.getId(), 1);

    CreateOrderException exception =
        assertThrows(
            CreateOrderException.class, () -> createOrder(UUID.randomUUID(), purchaseItems));
    assertEquals("Buyer does not exist", exception.getMessage());
  }

  @Test
  void shouldRejectCreateOrderWithNullRequestId() {
    Product product = productBuilder().price(BigDecimal.valueOf(1)).quantity(99).build();
    productRepository.save(product);

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product.getId(), 1);

    User buyer = uniqueUsernameUserBuilder().balance(BigDecimal.valueOf(999)).build();
    userRepository.save(buyer);

    CreateOrderException exception =
        assertThrows(
            CreateOrderException.class, () -> createOrder(buyer.getId(), purchaseItems, null));
    assertEquals("Request ID cannot be null", exception.getMessage());
  }

  @Test
  void shouldRejectCreateOrderWithEmptyPurchaseItems() {
    User buyer = uniqueUsernameUserBuilder().build();
    userRepository.save(buyer);

    PurchaseItems purchaseItems = new PurchaseItems();

    CreateOrderException exception =
        assertThrows(CreateOrderException.class, () -> createOrder(buyer.getId(), purchaseItems));
    assertEquals("Purchase items cannot be empty", exception.getMessage());
  }

  @Test
  void shouldRejectCreateOrderWithNonExistingProduct() {
    User buyer = uniqueUsernameUserBuilder().build();
    userRepository.save(buyer);

    UUID nonExistingProductId = UUID.randomUUID();

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(nonExistingProductId, 1);

    CreateOrderException exception =
        assertThrows(CreateOrderException.class, () -> createOrder(buyer.getId(), purchaseItems));
    assertEquals("Product: " + nonExistingProductId + " does not exist", exception.getMessage());
  }

  @Test
  void shouldRejectCreateOrderWithInsufficientBalance() {
    User buyer = uniqueUsernameUserBuilder().balance(new BigDecimal("9.99999")).build();
    userRepository.save(buyer);

    Product product = productBuilder().price(new BigDecimal(5)).quantity(999).build();
    productRepository.save(product);

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product.getId(), 2);

    CreateOrderException exception =
        assertThrows(CreateOrderException.class, () -> createOrder(buyer.getId(), purchaseItems));
    assertEquals("Insufficient balance", exception.getMessage());

    // product quantity should not be reduced
    assertEquals(999, productRepository.findById(product.getId()).orElseThrow().getQuantity());
  }

  @Test
  void shouldRejectOrderWithInsufficientProductQuantity() {
    User buyer = uniqueUsernameUserBuilder().balance(new BigDecimal(999)).build();
    userRepository.save(buyer);

    Product product = productBuilder().quantity(1).price(BigDecimal.ONE).build();
    productRepository.save(product);

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product.getId(), 2);

    CreateOrderException exception =
        assertThrows(CreateOrderException.class, () -> createOrder(buyer.getId(), purchaseItems));
    assertEquals("Insufficient stock for product: " + product.getId(), exception.getMessage());

    // buyer balance should not be reduced
    assertBigDecimalEquals(
        new BigDecimal(999), userRepository.findById(buyer.getId()).orElseThrow().getBalance());
  }

  @Test
  void shouldNotThrowExceptionIfStockAndBuyerBalanceIsJustEnough() {
    User buyer = uniqueUsernameUserBuilder().balance(new BigDecimal(10)).build();
    userRepository.save(buyer);

    Product product = productBuilder().quantity(1).price(new BigDecimal(10)).build();
    productRepository.save(product);

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product.getId(), 1);

    createOrder(buyer.getId(), purchaseItems); // should not throw exception
  }

  @Test
  void shouldReduceProductQuantityAndBuyerBalanceWhenOrderIsSuccessful() {
    User buyer = uniqueUsernameUserBuilder().balance(new BigDecimal(25)).build();
    userRepository.save(buyer);

    Product product1 = productBuilder().quantity(10).price(new BigDecimal("5.2")).build();
    Product product2 = productBuilder().quantity(10).price(new BigDecimal("3.5")).build();
    productRepository.save(product1);
    productRepository.save(product2);

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product1.getId(), 2);
    purchaseItems.setPurchaseItem(product2.getId(), 3);

    createOrder(buyer.getId(), purchaseItems);

    assertEquals(8, productRepository.findById(product1.getId()).orElseThrow().getQuantity());
    assertEquals(7, productRepository.findById(product2.getId()).orElseThrow().getQuantity());

    assertBigDecimalEquals(
        new BigDecimal("4.1"),
        userRepository
            .findById(buyer.getId())
            .orElseThrow()
            .getBalance()); // 20 - (5.2 * 2 + 3.5 * 3)
  }

  @Test
  void shouldIncreaseSellersBalance() {
    User buyer = uniqueUsernameUserBuilder().balance(new BigDecimal(999)).build();
    User seller1 = uniqueUsernameUserBuilder().balance(new BigDecimal(5)).build();
    User seller2 = uniqueUsernameUserBuilder().balance(new BigDecimal(10)).build();
    userRepository.saveAll(List.of(buyer, seller1, seller2));

    Product product1 =
        productBuilder().quantity(999).price(new BigDecimal(5)).userId(seller1.getId()).build();
    Product product2 =
        productBuilder().quantity(999).price(new BigDecimal(3)).userId(seller2.getId()).build();
    productRepository.saveAll(List.of(product1, product2));

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product1.getId(), 2);
    purchaseItems.setPurchaseItem(product2.getId(), 3);

    createOrder(buyer.getId(), purchaseItems);

    assertBigDecimalEquals(
        new BigDecimal(15), userRepository.findById(seller1.getId()).orElseThrow().getBalance());
    assertBigDecimalEquals(
        new BigDecimal(19), userRepository.findById(seller2.getId()).orElseThrow().getBalance());
  }

  @Test
  void shouldCreateOrder() {
    User buyer = uniqueUsernameUserBuilder().balance(new BigDecimal(999)).build();
    userRepository.save(buyer);

    Product product1 = productBuilder().quantity(999).price(new BigDecimal(5)).build();
    Product product2 = productBuilder().quantity(999).price(new BigDecimal(3)).build();
    productRepository.saveAll(List.of(product1, product2));

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product1.getId(), 2);
    purchaseItems.setPurchaseItem(product2.getId(), 5);

    Order order = createOrder(buyer.getId(), purchaseItems);

    assertEquals(buyer.getId(), order.getBuyerUserId());
    assertEquals(
        purchaseItems.getProductIdToQuantity(), order.getPurchaseItems().getProductIdToQuantity());

    Order savedOrder = orderRepository.findById(order.getId()).orElseThrow();
    assertOrderEquals(order, savedOrder);
  }

  @Test
  void shouldEachCreatedOrderHasDifferentId() {
    User buyer = uniqueUsernameUserBuilder().balance(new BigDecimal(999)).build();
    userRepository.save(buyer);

    Product product = productBuilder().quantity(999).price(new BigDecimal(5)).build();
    productRepository.save(product);

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product.getId(), 1);

    Order order1 = createOrder(buyer.getId(), purchaseItems, seedRequestId1);
    Order order2 = createOrder(buyer.getId(), purchaseItems, seedRequestId2);

    assertNotEquals(order1.getId(), order2.getId());
  }

  @Test
  void shouldOneOrderBeingCreatedOnly_IfCreateOrderWithSameRequestIdTwice() {
    User buyer = uniqueUsernameUserBuilder().balance(new BigDecimal(10)).build();
    userRepository.save(buyer);

    Product product = productBuilder().quantity(5).price(new BigDecimal(1)).build();
    productRepository.save(product);

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product.getId(), 1);

    Order order1 = createOrder(buyer.getId(), purchaseItems, seedRequestId1);
    Order order2 = createOrder(buyer.getId(), purchaseItems, seedRequestId1);

    assertOrderEquals(order2, order1);
    assertEquals(4, productRepository.findById(product.getId()).orElseThrow().getQuantity());
    assertBigDecimalEquals(
        new BigDecimal(9), userRepository.findById(buyer.getId()).orElseThrow().getBalance());
  }

  @Test
  void shouldDifferentBuyerUsingSameRequestId_WillCreateTwoOrders() {
    User buyer1 = uniqueUsernameUserBuilder().balance(new BigDecimal(10)).build();
    User buyer2 = uniqueUsernameUserBuilder().balance(new BigDecimal(10)).build();
    userRepository.saveAll(List.of(buyer1, buyer2));

    Product product = productBuilder().quantity(5).price(new BigDecimal(1)).build();
    productRepository.save(product);

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product.getId(), 1);

    Order order1 = createOrder(buyer1.getId(), purchaseItems, seedRequestId1);
    Order order2 = createOrder(buyer2.getId(), purchaseItems, seedRequestId1);

    assertNotEquals(order1.getId(), order2.getId());
  }

  @Test
  void shouldAutoRetry_WhenOneThreadMayFailJustDueToRacing() {
    User seller = uniqueUsernameUserBuilder().balance(new BigDecimal(999)).build();
    User buyer = uniqueUsernameUserBuilder().balance(new BigDecimal(999)).build();
    userRepository.saveAll(List.of(seller, buyer));

    Product product =
        productBuilder().quantity(2).price(new BigDecimal(5)).userId(seller.getId()).build();
    productRepository.save(product);

    PurchaseItems purchaseItems = new PurchaseItems();
    purchaseItems.setPurchaseItem(product.getId(), 1);

    // 2 threads try to buy the same product at the same time
    Thread thread1 = new Thread(() -> createOrder(buyer.getId(), purchaseItems, seedRequestId1));
    Thread thread2 = new Thread(() -> createOrder(buyer.getId(), purchaseItems, seedRequestId2));

    thread1.start();
    thread2.start();

    try {
      thread1.join();
      thread2.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    assertEquals(0, productRepository.findById(product.getId()).orElseThrow().getQuantity());
  }

  private Order createOrder(UUID buyerUserId, PurchaseItems purchaseItems) {
    return orderService.createOrder(buyerUserId, purchaseItems, UUID.randomUUID());
  }

  private Order createOrder(UUID buyerUserId, PurchaseItems purchaseItems, UUID requestId) {
    return orderService.createOrder(buyerUserId, purchaseItems, requestId);
  }

  private void assertOrderEquals(Order expected, Order actual) {
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getBuyerUserId(), actual.getBuyerUserId());
    assertEquals(
        expected.getPurchaseItems().getProductIdToQuantity(),
        actual.getPurchaseItems().getProductIdToQuantity());
    assertEquals(expected.getRequestId(), actual.getRequestId());
  }
}
