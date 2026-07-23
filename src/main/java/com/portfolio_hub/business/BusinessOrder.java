package com.portfolio_hub.business;

import com.portfolio_hub.utils.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
  name = "business_orders",
  indexes = {
    @Index(
      name = "idx_business_order",
      columnList = "businessId,status,createdAt"
    ),
  }
)
public class BusinessOrder extends BaseEntity {

  public enum Status {
    NEW,
    CONFIRMED,
    PROCESSING,
    READY,
    COMPLETED,
    CANCELLED,
  }

  @Column(nullable = false, columnDefinition = "TEXT")
  private String businessId;

  @Column(columnDefinition = "TEXT")
  private String itemId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String itemName;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String customerName;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String customerEmail;

  @Column(columnDefinition = "TEXT")
  private String customerPhone;

  @Column(columnDefinition = "TEXT")
  private String address;

  private int quantity;

  @Column(columnDefinition = "TEXT")
  private String variation;

  @Lob
  private String instructions;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private Status status;
}
