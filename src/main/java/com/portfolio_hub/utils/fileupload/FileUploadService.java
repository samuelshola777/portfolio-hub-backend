package com.portfolio_hub.utils.fileupload;

import com.portfolio_hub.utils.fileupload.response.FileUploadResponse;
import java.util.Collection;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
  FileUploadResponse uploadFile(
    MultipartFile file,
    FileUploadCategory category,
    FileUsageType usageType
  );
  void deleteFileByUrl(String fileUrl);
  void deleteOldFileIfChanged(String oldFileUrl, String newFileUrl);
  void deleteFileAsync(String fileUrl);
  void deleteFileAsync(Collection<String> fileUrls);
}
