package com.leungcheng.spring_simple_backend.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.leungcheng.spring_simple_backend.validation.ObjectValidator;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {
  public static class Builder {
    private String name;
    private BigDecimal price;
    private int quantity;
    private String userId;
    private String id = java.util.UUID.randomUUID().toString();

    private Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder price(BigDecimal price) {
      this.price = price;
      return this;
    }

    public Builder quantity(int quantity) {
      this.quantity = quantity;
      return this;
    }

    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public Product build() {
      Product product = new Product();
      product.id = id;
      product.name = name;
      product.price = price;
      product.quantity = quantity;
      product.userId = userId;
      ObjectValidator.validate(product);

      return product;
    }
  }

  private Product() {}

  public Builder toBuilder() {
    return new Builder().name(name).price(price).quantity(quantity).userId(userId).id(id);
  }

  @Id
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private String id;

  private String userId;

  @NotBlank private String name;

  @Min(0)
  private BigDecimal price;

  @Min(0)
  private int quantity;

  public String getId() {
    return id;
  }

  public String getUserId() {
    return userId;
  }

  public String getName() {
    return name;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public int getQuantity() {
    return quantity;
  }
}
