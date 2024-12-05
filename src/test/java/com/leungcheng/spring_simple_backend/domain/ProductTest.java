package com.leungcheng.spring_simple_backend.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.leungcheng.spring_simple_backend.validation.ObjectValidator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductTest {

  private static Product.Builder productBuilder() {
    return new Product.Builder()
        .name("Default Product")
        .price(new BigDecimal("0.1"))
        .userId("user_01")
        .quantity(1);
  }

  @Test
  void shouldCreateProduct() {
    Product product =
        productBuilder()
            .name("Product 1")
            .price(new BigDecimal("1.0"))
            .quantity(50)
            .userId("user_001")
            .build();

    assertEquals("Product 1", product.getName());
    assertEquals(new BigDecimal("1.0"), product.getPrice());
    assertEquals("user_001", product.getUserId());
    assertEquals(50, product.getQuantity());
  }

  @Test
  void shouldIdIsDifferentForEachBuild() {
    Product product1 = productBuilder().build();
    Product product2 = productBuilder().build();

    assertNotEquals(product1.getId(), product2.getId());
  }

  @Test
  void shouldToBuilderAbleToBuildSameProduct() {
    Product product1 = productBuilder().build();
    Product product2 = product1.toBuilder().build();

    assertEquals(product1.getId(), product2.getId());
    assertEquals(product1.getName(), product2.getName());
    assertEquals(product1.getPrice(), product2.getPrice());
    assertEquals(product1.getQuantity(), product2.getQuantity());
    assertEquals(product1.getUserId(), product2.getUserId());
  }

  @Test
  void shouldRaiseExceptionWhenBuild_IfParamsViolateTheValidationConstraints() {
    assertThrowValidationException(productBuilder().quantity(-1));

    assertThrowValidationException(productBuilder().price(new BigDecimal("-1")));

    assertThrowValidationException(productBuilder().name(""));
    assertThrowValidationException(productBuilder().name(null));
  }

  private void assertThrowValidationException(Product.Builder builder) {
    Class<ObjectValidator.ObjectValidationException> expected =
        ObjectValidator.ObjectValidationException.class;
    assertThrows(expected, builder::build);
  }
}
