package com.portfolio_hub.profile;

import com.portfolio_hub.utils.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
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
  name = "profile_entries",
  indexes = @Index(
    name = "idx_profile_entry_owner_type",
    columnList = "ownerId,type,sortOrder"
  )
)
public class ProfileEntry extends BaseEntity {

  public enum EntryType {
    EXPERIENCE,
    EDUCATION,
    CERTIFICATION,
    ACHIEVEMENT,
    PROFESSIONAL_MEMBERSHIP,
    VOLUNTEER,
    LANGUAGE,
    PUBLICATION,
    RESEARCH,
    CONFERENCE_SPEAKING,
  }

  @Column(nullable = false, columnDefinition = "TEXT")
  private String ownerId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private EntryType type;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String title;

  @Column(columnDefinition = "TEXT")
  private String organization;

  @Column(columnDefinition = "TEXT")
  private String subtitle;

  @Column(columnDefinition = "TEXT")
  private String location;

  private LocalDate startDate;
  private LocalDate endDate;
  private boolean current;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(columnDefinition = "TEXT")
  private String url;

  @Column(columnDefinition = "TEXT")
  private String thumbnailUrl;

  @Column(columnDefinition = "TEXT")
  private String supportingDocumentUrl;

  private boolean published;
  private int sortOrder;
}
