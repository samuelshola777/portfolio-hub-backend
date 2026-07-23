package com.portfolio_hub.announcement;

import com.portfolio_hub.utils.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
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
  name = "announcements",
  indexes = @Index(
    name = "idx_announcement_creator",
    columnList = "createdById,createdAt"
  )
)
public class Announcement extends BaseEntity {

  @Column(nullable = false, columnDefinition = "TEXT")
  private String createdById;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String subject;

  @Lob
  @Column(nullable = false)
  private String message;

  @Lob
  private String attachmentsJson;
}
