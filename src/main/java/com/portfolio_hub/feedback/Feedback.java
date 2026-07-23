package com.portfolio_hub.feedback;

import com.portfolio_hub.utils.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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
  name = "feedback_tickets",
  indexes = {
    @Index(
      name = "idx_feedback_owner_created",
      columnList = "ownerId,createdAt"
    ),
    @Index(
      name = "idx_feedback_status_created",
      columnList = "status,createdAt"
    ),
  }
)
public class Feedback extends BaseEntity {

  public enum Category {
    COMPLAINT,
    FEEDBACK,
    SUGGESTION,
    TECHNICAL_ISSUE,
  }

  public enum Status {
    OPEN,
    RESPONDED,
    CLOSED,
  }

  @Column(nullable = false, columnDefinition = "TEXT")
  private String ownerId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private Category category;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String subject;

  @Lob
  @Column(nullable = false)
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private Status status;

  @Lob
  private String adminResponse;

  @Column(columnDefinition = "TEXT")
  private String respondedById;

  private LocalDateTime respondedAt;
}
