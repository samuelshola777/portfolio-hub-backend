package com.portfolio_hub.utils.fileupload;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.portfolio_hub.userauthmgt.user.UserRepository;
import com.portfolio_hub.utils.exception.ErrorProcessingRequestException;
import com.portfolio_hub.utils.exception.InvalidInputException;
import com.portfolio_hub.utils.exception.InvalidOperationException;
import com.portfolio_hub.utils.exception.ResourceNotFoundException;
import com.portfolio_hub.utils.exception.UnauthorizedException;
import com.portfolio_hub.utils.fileupload.response.FileUploadResponse;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryFileUploadServiceImpl implements FileUploadService {

  private static final Set<String> SAFE_DOCUMENT_TYPES = Set.of(
    "application/pdf",
    "text/plain",
    "text/csv",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
  );

  @Value("${application.cloudinary.cloud-name}")
  private String cloudName;

  @Value("${application.cloudinary.api-key}")
  private String apiKey;

  @Value("${application.cloudinary.api-secret}")
  private String apiSecret;

  private final ManagedFileRepository managedFileRepository;
  private final UserRepository userRepository;
  private Cloudinary cloudinary;

  @PostConstruct
  void initialize() {
    cloudinary = new Cloudinary(
      ObjectUtils.asMap(
        "cloud_name",
        cloudName,
        "api_key",
        apiKey,
        "api_secret",
        apiSecret
      )
    );
  }

  @Override
  public FileUploadResponse uploadFile(
    MultipartFile file,
    FileUploadCategory category,
    FileUsageType usageType
  ) {
    if (
      file == null || file.isEmpty()
    ) throw new ErrorProcessingRequestException("File is required");
    if (
      cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()
    ) throw new ErrorProcessingRequestException("Cloudinary is not configured");
    FileUploadCategory resolvedCategory = category == null
      ? FileUploadCategory.OTHER
      : category;
    FileUsageType resolvedUsage = usageType == null
      ? FileUsageType.GENERAL
      : usageType;
    String ownerId = currentOwnerId();
    validateFile(file, resolvedCategory, resolvedUsage);
    try {
      Map<String, Object> uploadOptions = new HashMap<>();
      uploadOptions.put("folder", buildFolder(resolvedCategory, resolvedUsage));
      uploadOptions.put("resource_type", resourceTypeFor(resolvedCategory));
      uploadOptions.put("access_mode", "public");

      /*
       * Cloudinary may classify PDFs sent with resource_type=auto as image
       * assets. Some Cloudinary accounts block public PDF delivery through
       * /image/upload, which produces HTTP 401 in browsers and iframes.
       * Documents belong in Cloudinary's raw resource type and must retain
       * their extension so browsers receive the correct content type. The
       * Cloudinary product environment must also allow PDF delivery.
       */
      if (resolvedCategory == FileUploadCategory.DOCUMENT) {
        uploadOptions.put("public_id", UUID.randomUUID() + fileExtension(file));
        uploadOptions.put(
          "filename_override",
          safeFileName(file.getOriginalFilename())
        );
      }

      Map<?, ?> result = cloudinary
        .uploader()
        .upload(file.getBytes(), uploadOptions);
      ManagedFile saved = managedFileRepository.save(
        ManagedFile.builder()
          .ownerId(ownerId)
          .originalFileName(file.getOriginalFilename())
          .fileUrl(String.valueOf(result.get("secure_url")))
          .publicId(String.valueOf(result.get("public_id")))
          .format(value(result.get("format")))
          .resourceType(value(result.get("resource_type")))
          .fileSizeBytes(longValue(result.get("bytes")))
          .category(resolvedCategory)
          .usageType(resolvedUsage)
          .build()
      );
      return map(saved);
    } catch (Exception exception) {
      log.error("Cloudinary upload failed", exception);
      throw new ErrorProcessingRequestException(
        "Error uploading file, please try again later"
      );
    }
  }

  @Override
  public void deleteFileByUrl(String fileUrl) {
    if (fileUrl == null || fileUrl.isBlank()) return;
    ManagedFile file = managedFileRepository
      .findByFileUrl(fileUrl)
      .orElseThrow(() -> new ResourceNotFoundException("File not found"));
    try {
      cloudinary
        .uploader()
        .destroy(
          file.getPublicId(),
          ObjectUtils.asMap("resource_type", file.getResourceType())
        );
      managedFileRepository.delete(file);
    } catch (Exception exception) {
      throw new ErrorProcessingRequestException(
        "Error deleting file from Cloudinary"
      );
    }
  }

  @Override
  public void deleteOldFileIfChanged(String oldUrl, String newUrl) {
    if (
      oldUrl != null &&
      !oldUrl.isBlank() &&
      newUrl != null &&
      !newUrl.isBlank() &&
      !oldUrl.equals(newUrl)
    ) deleteFileAsync(oldUrl);
  }

  @Override
  @Async
  public void deleteFileAsync(String fileUrl) {
    try {
      deleteFileByUrl(fileUrl);
    } catch (Exception exception) {
      log.warn("Unable to delete file {}", fileUrl);
    }
  }

  @Override
  @Async
  public void deleteFileAsync(Collection<String> urls) {
    if (urls != null) urls.forEach(this::deleteFileAsync);
  }

  private String buildFolder(FileUploadCategory category, FileUsageType usage) {
    return (
      "portfolio-hub/" +
      category.name().toLowerCase() +
      "/" +
      usage.name().toLowerCase() +
      "/" +
      LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
    );
  }

  private String resourceTypeFor(FileUploadCategory category) {
    return category == FileUploadCategory.DOCUMENT ? "raw" : "auto";
  }

  private String fileExtension(MultipartFile file) {
    String originalName = file.getOriginalFilename();
    if (originalName != null) {
      int dot = originalName.lastIndexOf('.');
      if (dot >= 0 && dot < originalName.length() - 1) {
        String extension = originalName.substring(dot).toLowerCase(Locale.ROOT);
        if (extension.matches("\\.[a-z0-9]{1,10}")) return extension;
      }
    }
    return "application/pdf".equalsIgnoreCase(file.getContentType())
      ? ".pdf"
      : ".bin";
  }

  private String safeFileName(String originalName) {
    if (originalName == null || originalName.isBlank()) return "document";
    return originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
  }

  private FileUploadResponse map(ManagedFile f) {
    return new FileUploadResponse(
      f.getFileUrl(),
      f.getPublicId(),
      f.getOriginalFileName(),
      f.getFormat(),
      f.getResourceType(),
      f.getFileSizeBytes(),
      f.getCategory(),
      f.getUsageType()
    );
  }

  private String value(Object value) {
    return value == null ? null : value.toString();
  }

  private Long longValue(Object value) {
    return value == null ? null : Long.parseLong(value.toString());
  }

  private void validateFile(
    MultipartFile file,
    FileUploadCategory category,
    FileUsageType usage
  ) {
    String contentType = file.getContentType() == null
      ? ""
      : file.getContentType().toLowerCase(Locale.ROOT);
    if (contentType.isBlank()) throw new InvalidInputException(
      "The selected file type could not be verified"
    );
    if (
      category == FileUploadCategory.IMAGE && !contentType.startsWith("image/")
    ) throw new InvalidInputException("Choose a valid image file");
    if (
      category == FileUploadCategory.IMAGE &&
      contentType.equals("image/svg+xml")
    ) throw new InvalidInputException("SVG uploads are not allowed");
    if (
      category == FileUploadCategory.VIDEO && !contentType.startsWith("video/")
    ) throw new InvalidInputException("Choose a valid video file");
    if (
      category == FileUploadCategory.AUDIO && !contentType.startsWith("audio/")
    ) throw new InvalidInputException("Choose a valid audio file");
    if (
      category == FileUploadCategory.DOCUMENT &&
      !SAFE_DOCUMENT_TYPES.contains(contentType)
    ) throw new InvalidInputException(
      "Choose a PDF, text, Word, Excel or PowerPoint document"
    );
    if (
      usage == FileUsageType.CV && !"application/pdf".equals(contentType)
    ) throw new InvalidInputException("Your CV must be a PDF");
    if (
      contentType.contains("javascript") ||
      contentType.contains("text/html") ||
      contentType.contains("x-msdownload") ||
      contentType.contains("x-sh")
    ) throw new InvalidInputException("That file type is not allowed");

    long maximumBytes = switch (usage) {
      case PROFILE_IMAGE, BACKGROUND_THUMBNAIL -> 5L * 1024 * 1024;
      case SKILL_ICON -> 2L * 1024 * 1024;
      case WORK_THUMBNAIL -> 8L * 1024 * 1024;
      case CV -> 10L * 1024 * 1024;
      case GENERAL, BILLING_TRANSFER_PROOF -> 25L * 1024 * 1024;
      case PROFILE_VIDEO, BUSINESS_MEDIA -> 50L * 1024 * 1024;
      case WORK_GALLERY_IMAGE, WORK_GALLERY_VIDEO, WORK_DOCUMENT -> 100L *
      1024 *
      1024;
    };
    if (file.getSize() > maximumBytes) throw new InvalidInputException(
      "The selected file is larger than the allowed limit"
    );
  }

  private String currentOwnerId() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (
      authentication == null ||
      !authentication.isAuthenticated() ||
      "anonymousUser".equals(authentication.getName())
    ) throw new UnauthorizedException("Authentication required");
    var user = userRepository
      .findByEmailAddressIgnoreCaseAndDeletedFalse(authentication.getName())
      .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    if (
      user.getStatus() !=
      com.portfolio_hub.userauthmgt.user.User.AccountStatus.ACTIVE
    ) throw new InvalidOperationException("Account access is restricted");
    if (!user.isEmailVerified()) throw new InvalidOperationException(
      "Verify your email before uploading files"
    );
    return user.getId();
  }
}
