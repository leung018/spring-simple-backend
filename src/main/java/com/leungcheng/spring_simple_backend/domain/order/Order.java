package com.leungcheng.spring_simple_backend.domain.order;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {
  @Id private final String id = java.util.UUID.randomUUID().toString();
  private String userId;
  private PurchaseItems purchaseItems;

  Order() {} // FIXME: Change to private

  public String getId() {
    return id;
  }

  public String getUserId() {
    return userId;
  }

  public PurchaseItems getPurchaseItems() {
    return purchaseItems;
  }
}
