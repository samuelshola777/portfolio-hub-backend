package com.portfolio_hub.announcement;

import com.portfolio_hub.utils.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
  name = "announcement_recipients",
  indexes = @Index(
    name = "idx_announcement_recipient",
    columnList = "userId,createdAt"
  ),
  uniqueConstraints = @UniqueConstraint(
    name = "uk_announcement_user",
    columnNames = { "announcementId", "userId" }
  )
)
public class AnnouncementRecipient extends BaseEntity {

  @Column(nullable = false, columnDefinition = "TEXT")
  private String announcementId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String userId;

  private LocalDateTime readAt;
}
