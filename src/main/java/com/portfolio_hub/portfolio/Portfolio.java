package com.portfolio_hub.portfolio;

import com.portfolio_hub.utils.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
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
  name = "portfolios",
  indexes = {
    @Index(name = "idx_portfolio_owner", columnList = "ownerId", unique = true),
    @Index(
      name = "idx_portfolio_username",
      columnList = "username",
      unique = true
    ),
    @Index(name = "idx_portfolio_status", columnList = "status"),
  }
)
public class Portfolio extends BaseEntity {

  public enum Theme {
    ORBIT,
    EDITORIAL,
    SPATIAL,
  }

  public enum FontStyle {
    GEIST,
    EDITORIAL,
    MONO,
  }

  public enum Motion {
    FULL,
    REDUCED,
    NONE,
  }

  public enum PublicationStatus {
    DRAFT,
    PUBLISHED,
  }

  @Column(nullable = false, unique = true, columnDefinition = "TEXT")
  private String ownerId;

  @Column(nullable = false, unique = true, columnDefinition = "TEXT")
  private String username;

  @Column(columnDefinition = "TEXT")
  private String headline;

  @Column(columnDefinition = "TEXT")
  private String introduction;

  @Column(columnDefinition = "TEXT")
  private String note;

  @Column(columnDefinition = "TEXT")
  private String availability;

  @Column(columnDefinition = "TEXT")
  private String avatarUrl;

  @Column(columnDefinition = "TEXT")
  private String cvUrl;

  @Column(columnDefinition = "TEXT")
  private String introVideoUrl;

  @Column(columnDefinition = "TEXT")
  private String websiteUrl;

  @Column(columnDefinition = "TEXT")
  private String githubUsername;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private Theme theme;

  @Column(columnDefinition = "TEXT")
  private String accent;

  @Column(columnDefinition = "TEXT")
  private String background;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private FontStyle font;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private Motion motion;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private PublicationStatus status;

  private LocalDateTime publishedAt;
}
