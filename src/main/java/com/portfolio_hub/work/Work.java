package com.portfolio_hub.work;

import com.portfolio_hub.utils.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
  name = "portfolio_works",
  indexes = {
    @Index(
      name = "idx_work_owner_slug",
      columnList = "ownerId,slug",
      unique = true
    ),
    @Index(
      name = "idx_work_owner_status_order",
      columnList = "ownerId,status,sortOrder"
    ),
  }
)
public class Work extends BaseEntity {

  public enum PublicationStatus {
    DRAFT,
    PUBLISHED,
  }

  @Column(nullable = false, columnDefinition = "TEXT")
  private String ownerId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String portfolioId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String slug;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String summary;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(columnDefinition = "TEXT")
  private String challengeText;

  @Column(columnDefinition = "TEXT")
  private String processText;

  @Column(columnDefinition = "TEXT")
  private String resultsText;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String category;

  @Column(columnDefinition = "TEXT")
  private String roleName;

  private LocalDate startedAt;
  private LocalDate completedAt;
  private boolean ongoing;

  @Column(columnDefinition = "TEXT")
  private String projectUrl;

  @Column(columnDefinition = "TEXT")
  private String sourceUrl;

  @Column(columnDefinition = "TEXT")
  private String thumbnailUrl;

  private boolean featured;
  private int sortOrder;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private PublicationStatus status;

  private boolean deleted;

  @ElementCollection
  @CollectionTable(
    name = "work_gallery_urls",
    joinColumns = @JoinColumn(name = "work_id")
  )
  @OrderColumn(name = "display_order")
  @Column(name = "gallery_url", columnDefinition = "TEXT")
  @Builder.Default
  private List<String> galleryUrls = new ArrayList<>();

  @ElementCollection
  @CollectionTable(
    name = "work_technology_stack",
    joinColumns = @JoinColumn(name = "work_id")
  )
  @OrderColumn(name = "display_order")
  @Column(name = "technology", columnDefinition = "TEXT")
  @Builder.Default
  private List<String> technologyStack = new ArrayList<>();
}
