package com.leungcheng.spring_simple_backend.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ProductTest {

  private static Product.Builder productBuilder() {
    return new Product.Builder().name("Default Product").price(2.0).quantity(3);
  }

  @Test
  void shouldCreateProduct() {
    Product product = productBuilder().name("Product 1").price(1.0).quantity(50).build();

    assertEquals("Product 1", product.getName());
    assertEquals(1.0, product.getPrice());
    assertEquals(50, product.getQuantity());
  }

  @Test
  void shouldIdIsDifferentForEachBuild() {
    Product product1 = productBuilder().build();
    Product product2 = productBuilder().build();

    assertNotEquals(product1.getId(), product2.getId());
  }

  @Test
  void shouldRaiseExceptionIfViolateTheValidationConstraints() {
    Class<IllegalArgumentException> expected = IllegalArgumentException.class;
    assertThrows(
        expected,
        () -> {
          productBuilder().quantity(-1).build();
        });
    assertThrows(
        expected,
        () -> {
          productBuilder().price(-1).build();
        });
    assertThrows(
        expected,
        () -> {
          productBuilder().name("").build();
        });
  }

  @Test
  void shouldNotRaiseExceptionIfZero() {
    assertDoesNotThrow(
        () -> {
          productBuilder().quantity(0).price(0).build();
        });
  }
}
