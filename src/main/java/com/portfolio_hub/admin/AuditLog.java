package com.portfolio_hub.admin;

import com.portfolio_hub.utils.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
  name = "audit_logs",
  indexes = @Index(
    name = "idx_audit_actor_created",
    columnList = "actorId,createdAt"
  )
)
public class AuditLog extends BaseEntity {

  @Column(nullable = false, columnDefinition = "TEXT")
  private String actorId;

  @Column(columnDefinition = "TEXT")
  private String targetUserId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String action;

  @Column(columnDefinition = "TEXT")
  private String description;
}
