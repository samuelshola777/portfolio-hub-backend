package com.portfolio_hub.subscription;

import com.portfolio_hub.utils.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
  name = "subscription_plans",
  indexes = {
    @Index(
      name = "idx_subscription_plan_code",
      columnList = "code",
      unique = true
    ),
    @Index(
      name = "idx_subscription_plan_listing",
      columnList = "workspaceType,active,publicVisible,sortOrder"
    ),
  }
)
public class SubscriptionPlan extends BaseEntity {

  @Column(
    nullable = false,
    unique = true,
    updatable = false,
    columnDefinition = "TEXT"
  )
  private String code;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String name;

  @Lob
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private WorkspaceType workspaceType;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal monthlyPrice;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String currency;

  @Column(nullable = false)
  private boolean free;

  @Column(nullable = false)
  private boolean active;

  @Column(nullable = false)
  private boolean publicVisible;

  @Column(nullable = false)
  private int sortOrder;
}
