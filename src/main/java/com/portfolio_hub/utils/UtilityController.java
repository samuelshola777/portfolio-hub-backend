package com.portfolio_hub.utils;

import com.portfolio_hub.utils.fileupload.FileManagementService;
import com.portfolio_hub.utils.fileupload.FileUploadCategory;
import com.portfolio_hub.utils.fileupload.FileUploadService;
import com.portfolio_hub.utils.fileupload.FileUsageType;
import com.portfolio_hub.utils.fileupload.request.FileDeleteRequest;
import com.portfolio_hub.utils.fileupload.response.FileUploadResponse;
import com.portfolio_hub.utils.fileupload.response.ManagedFileResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/utilities")
@RequiredArgsConstructor
public class UtilityController {

  private final FileUploadService fileUploadService;
  private final FileManagementService fileManagementService;

  @PostMapping("/private/file/upload")
  public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
    @RequestParam("file") MultipartFile file,
    @RequestParam("category") FileUploadCategory category,
    @RequestParam(value = "usageType", required = false) FileUsageType usageType
  ) {
    return ResponseEntity.ok(
      ApiResponse.success(
        "File uploaded successfully",
        fileUploadService.uploadFile(file, category, usageType)
      )
    );
  }

  @GetMapping("/private/files")
  public ResponseEntity<ApiResponse<List<ManagedFileResponse>>> files() {
    return ResponseEntity.ok(
      ApiResponse.success("Files fetched", fileManagementService.mine())
    );
  }

  @PostMapping("/private/files/delete")
  public ResponseEntity<ApiResponse<Integer>> deleteFiles(
    @Valid @RequestBody FileDeleteRequest request
  ) {
    int deleted = fileManagementService.deleteMine(request.fileUrls());
    return ResponseEntity.ok(
      ApiResponse.success(
        deleted == 1 ? "File deleted" : deleted + " files deleted",
        deleted
      )
    );
  }
}
