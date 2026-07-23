package com.portfolio_hub.profile;

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
  name = "skills",
  indexes = @Index(
    name = "idx_skill_owner_category",
    columnList = "ownerId,category,sortOrder"
  )
)
public class Skill extends BaseEntity {

  public enum Proficiency {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    EXPERT,
  }

  @Column(nullable = false, columnDefinition = "TEXT")
  private String ownerId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String name;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String category;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private Proficiency proficiency;

  @Column(columnDefinition = "TEXT")
  private String iconUrl;

  private boolean featured;
  private int sortOrder;
}
