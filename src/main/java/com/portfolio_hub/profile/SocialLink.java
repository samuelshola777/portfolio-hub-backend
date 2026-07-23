package com.portfolio_hub.profile;

import com.portfolio_hub.utils.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
  name = "social_links",
  indexes = @Index(
    name = "idx_social_owner_order",
    columnList = "ownerId,sortOrder"
  )
)
public class SocialLink extends BaseEntity {

  @Column(nullable = false, columnDefinition = "TEXT")
  private String ownerId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String platform;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String url;

  private int sortOrder;
}
