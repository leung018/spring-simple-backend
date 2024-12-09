package com.leungcheng.spring_simple_backend.domain.order;

import com.google.common.collect.ImmutableMap;
import jakarta.persistence.*;
import java.util.Map;

@Embeddable
public class PurchaseItems {
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "purchase_items",
      joinColumns = {@JoinColumn(name = "order_id", referencedColumnName = "id")})
  @MapKeyColumn(name = "product_id")
  @Column(name = "quantity")
  private final Map<String, Integer> productIdToQuantity = new java.util.HashMap<>();

  public void setPurchaseItem(String productId, int quantity) {
    if (quantity < 1) {
      throw new IllegalArgumentException("Quantity must be greater than 0");
    }
    productIdToQuantity.put(productId, quantity);
  }

  public ImmutableMap<String, Integer> getProductIdToQuantity() {
    return ImmutableMap.copyOf(productIdToQuantity);
  }
}
