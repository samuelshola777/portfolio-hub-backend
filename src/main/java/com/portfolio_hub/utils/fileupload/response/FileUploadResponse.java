package com.portfolio_hub.utils.fileupload.response;

import com.portfolio_hub.utils.fileupload.FileUploadCategory;
import com.portfolio_hub.utils.fileupload.FileUsageType;

public record FileUploadResponse(
  String fileUrl,
  String publicId,
  String originalFileName,
  String format,
  String resourceType,
  Long fileSizeBytes,
  FileUploadCategory category,
  FileUsageType usageType
) {}
