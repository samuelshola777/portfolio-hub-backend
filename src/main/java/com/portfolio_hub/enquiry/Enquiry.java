package com.portfolio_hub.enquiry;

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
  name = "enquiries",
  indexes = @Index(
    name = "idx_enquiry_owner_status",
    columnList = "ownerId,status,createdAt"
  )
)
public class Enquiry extends BaseEntity {

  public enum Status {
    NEW,
    READ,
    ARCHIVED,
  }

  @Column(nullable = false, columnDefinition = "TEXT")
  private String ownerId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String portfolioId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String recruiterName;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String recruiterEmail;

  @Column(columnDefinition = "TEXT")
  private String company;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private Status status;
}
