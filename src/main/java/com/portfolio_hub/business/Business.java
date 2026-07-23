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
  name = "businesses",
  indexes = {
    @Index(name = "idx_business_slug", columnList = "slug", unique = true),
    @Index(name = "idx_business_owner", columnList = "ownerId,status"),
  }
)
public class Business extends BaseEntity {

  public enum Status {
    DRAFT,
    PUBLISHED,
    SUSPENDED,
  }

  public enum ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
  }

  public enum OnboardingStage {
    BASICS,
    BRAND,
    CONTACT,
    CONTENT,
    CATALOG,
    PREVIEW,
    COMPLETE,
  }

  @Column(nullable = false, columnDefinition = "TEXT")
  private String ownerId;

  @Column(
    nullable = false,
    unique = true,
    columnDefinition = "TEXT",
    updatable = false
  )
  private String slug;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String name;

  @Column(columnDefinition = "TEXT")
  private String tagline;

  @Lob
  private String description;

  @Column(columnDefinition = "TEXT")
  private String industry;

  @Column(columnDefinition = "TEXT")
  private String category;

  private Integer yearEstablished;

  @Column(columnDefinition = "TEXT")
  private String companySize;

  @Column(columnDefinition = "TEXT")
  private String registrationNumber;

  @Column(columnDefinition = "TEXT")
  private String logoUrl;

  @Column(columnDefinition = "TEXT")
  private String coverUrl;

  @Column(columnDefinition = "TEXT")
  private String email;

  @Column(columnDefinition = "TEXT")
  private String phone;

  @Column(columnDefinition = "TEXT")
  private String websiteUrl;

  @Column(columnDefinition = "TEXT")
  private String address;

  @Column(columnDefinition = "TEXT")
  private String socialLinksJson;

  @Column(columnDefinition = "TEXT")
  private String introVideoUrl;

  @Column(columnDefinition = "TEXT")
  private String templateKey;

  @Column(columnDefinition = "TEXT")
  private String accentColor;

  @Column(columnDefinition = "TEXT")
  private String lightBackground;

  @Column(columnDefinition = "TEXT")
  private String darkBackground;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private ThemeMode defaultMode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private Status status;

  @Enumerated(EnumType.STRING)
  @Column(columnDefinition = "TEXT")
  private OnboardingStage onboardingStage;
}
