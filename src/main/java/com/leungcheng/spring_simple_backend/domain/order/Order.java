package com.leungcheng.spring_simple_backend.domain.order;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {
  @Id private final String id = java.util.UUID.randomUUID().toString();
  private String buyerUserId;
  private PurchaseItems purchaseItems;

  private Order() {}

  Order (String buyerUserId, PurchaseItems purchaseItems) {
    this.buyerUserId = buyerUserId;
    this.purchaseItems = purchaseItems;
  }

  public String getId() {
    return id;
  }

  public String getBuyerUserId() {
    return buyerUserId;
  }

  public PurchaseItems getPurchaseItems() {
    return purchaseItems;
  }
}
