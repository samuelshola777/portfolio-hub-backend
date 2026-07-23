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
  name = "business_enquiries",
  indexes = {
    @Index(
      name = "idx_business_enquiry",
      columnList = "businessId,status,createdAt"
    ),
  }
)
public class BusinessEnquiry extends BaseEntity {

  public enum Type {
    GENERAL,
    QUOTE,
    CONSULTATION,
    PARTNERSHIP,
    VENDOR,
    TENDER,
    JOB,
    INTERNSHIP,
  }

  public enum Status {
    NEW,
    READ,
    ARCHIVED,
  }

  @Column(nullable = false, columnDefinition = "TEXT")
  private String businessId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private Type type;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String name;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String email;

  @Column(columnDefinition = "TEXT")
  private String company;

  @Column(columnDefinition = "TEXT")
  private String phone;

  @Lob
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private Status status;
}
