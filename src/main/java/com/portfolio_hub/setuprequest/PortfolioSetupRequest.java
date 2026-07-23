package com.portfolio_hub.setuprequest;

import com.portfolio_hub.utils.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
  name = "portfolio_setup_requests",
  indexes = {
    @Index(
      name = "idx_setup_request_status_created",
      columnList = "status,createdAt"
    ),
    @Index(name = "idx_setup_request_email", columnList = "email"),
  }
)
public class PortfolioSetupRequest extends BaseEntity {

  public enum TargetType {
    PORTFOLIO,
    BUSINESS,
  }

  public enum Status {
    NEW,
    CONTACTED,
    IN_PROGRESS,
    COMPLETED,
    CLOSED,
  }

  @Column(nullable = false, columnDefinition = "TEXT")
  private String fullName;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String email;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String whatsAppNumber;

  @Column(columnDefinition = "TEXT")
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private Status status;

  @Column(columnDefinition = "TEXT")
  private String adminNote;

  @Enumerated(EnumType.STRING)
  @Column(columnDefinition = "TEXT")
  private TargetType targetType;

  @Column(columnDefinition = "TEXT")
  private String ownerId;

  @Column(columnDefinition = "TEXT")
  private String workspaceId;
}
