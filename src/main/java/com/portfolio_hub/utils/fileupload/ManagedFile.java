package com.portfolio_hub.utils.fileupload;

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
  name = "managed_files",
  indexes = {
    @Index(
      name = "idx_managed_file_url",
      columnList = "fileUrl",
      unique = true
    ),
    @Index(
      name = "idx_managed_file_public_id",
      columnList = "publicId",
      unique = true
    ),
  }
)
public class ManagedFile extends BaseEntity {

  @Column(columnDefinition = "TEXT")
  private String ownerId;

  private String originalFileName;

  @Column(nullable = false, unique = true, columnDefinition = "TEXT")
  private String fileUrl;

  @Column(nullable = false, unique = true)
  private String publicId;

  private String format;
  private String resourceType;
  private Long fileSizeBytes;

  @Enumerated(EnumType.STRING)
  private FileUploadCategory category;

  @Enumerated(EnumType.STRING)
  private FileUsageType usageType;
}
