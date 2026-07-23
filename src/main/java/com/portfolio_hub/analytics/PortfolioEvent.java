package com.portfolio_hub.analytics;

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
  name = "portfolio_events",
  indexes = {
    @Index(name = "idx_event_owner_time", columnList = "ownerId,createdAt"),
    @Index(name = "idx_event_owner_type", columnList = "ownerId,eventType"),
  }
)
public class PortfolioEvent extends BaseEntity {

  public enum EventType {
    VIEW,
    PROJECT_CLICK,
    CV_DOWNLOAD,
    SOCIAL_CLICK,
    WEBSITE_CLICK,
    EXPORT_DOWNLOAD,
    ENQUIRY,
  }

  @Column(nullable = false, columnDefinition = "TEXT")
  private String ownerId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String portfolioId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private EventType eventType;

  @Column(columnDefinition = "TEXT")
  private String targetId;

  @Column(columnDefinition = "TEXT")
  private String source;

  @Column(columnDefinition = "TEXT")
  private String country;

  @Column(columnDefinition = "TEXT")
  private String city;
}
