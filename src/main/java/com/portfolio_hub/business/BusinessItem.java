package com.portfolio_hub.business;

import com.portfolio_hub.utils.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
  name = "business_items",
  indexes = {
    @Index(
      name = "idx_business_item_list",
      columnList = "businessId,type,status,sortOrder"
    ),
  }
)
public class BusinessItem extends BaseEntity {

  public enum Type {
    PAGE,
    SECTION,
    PRODUCT,
    PRODUCT_CATEGORY,
    SERVICE,
    MUSIC_TRACK,
    ALBUM,
    VIDEO,
    EVENT,
    TEAM_MEMBER,
    TESTIMONIAL,
    PROJECT,
    CREDENTIAL,
    PARTNER,
    FAQ,
  }

  public enum Status {
    DRAFT,
    PUBLISHED,
    HIDDEN,
  }

  @Column(nullable = false, columnDefinition = "TEXT")
  private String businessId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private Type type;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String title;

  @Column(columnDefinition = "TEXT")
  private String category;

  @Column(columnDefinition = "TEXT")
  private String summary;

  @Lob
  private String description;

  @Column(columnDefinition = "TEXT")
  private String thumbnailUrl;

  @Lob
  private String mediaJson;

  @Lob
  private String configurationJson;

  private BigDecimal price;
  private BigDecimal discountPrice;
  private Integer quantity;
  private boolean featured;

  @Column(nullable = false)
  private int sortOrder;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private Status status;

  private boolean deleted;
}
