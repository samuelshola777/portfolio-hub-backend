package com.portfolio_hub.utils.fileupload.response;

import com.portfolio_hub.utils.fileupload.FileUploadCategory;
import com.portfolio_hub.utils.fileupload.FileUsageType;
import java.time.LocalDateTime;

public record ManagedFileResponse(
  String id,
  String fileUrl,
  String originalFileName,
  Long fileSizeBytes,
  FileUploadCategory category,
  FileUsageType usageType,
  LocalDateTime createdAt
) {}
