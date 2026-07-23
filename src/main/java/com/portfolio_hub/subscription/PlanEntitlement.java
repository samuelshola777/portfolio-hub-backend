package com.portfolio_hub.subscription;

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
  name = "plan_entitlements",
  uniqueConstraints = @UniqueConstraint(
    name = "uk_plan_entitlement_code",
    columnNames = { "planId", "code" }
  ),
  indexes = @Index(name = "idx_plan_entitlement", columnList = "planId,code")
)
public class PlanEntitlement extends BaseEntity {

  @Column(nullable = false, columnDefinition = "TEXT")
  private String planId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private EntitlementCode code;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private EntitlementValueType valueType;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String value;
}
