package com.portfolio_hub.utils.fileupload;

import com.portfolio_hub.portfolio.PortfolioRepository;
import com.portfolio_hub.profile.ProfileEntryRepository;
import com.portfolio_hub.profile.SkillRepository;
import com.portfolio_hub.userauthmgt.user.User;
import com.portfolio_hub.userauthmgt.user.UserRepository;
import com.portfolio_hub.utils.exception.UnauthorizedException;
import com.portfolio_hub.utils.fileupload.response.ManagedFileResponse;
import com.portfolio_hub.work.WorkRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FileManagementService {

  private final ManagedFileRepository managedFileRepository;
  private final FileUploadService fileUploadService;
  private final UserRepository userRepository;
  private final PortfolioRepository portfolioRepository;
  private final ProfileEntryRepository profileEntryRepository;
  private final SkillRepository skillRepository;
  private final WorkRepository workRepository;

  @Transactional(readOnly = true)
  public List<ManagedFileResponse> mine() {
    User user = currentUser();
    return managedFileRepository
      .findAllByOwnerIdOrderByCreatedAtDesc(user.getId())
      .stream()
      .map(this::map)
      .toList();
  }

  @Transactional
  public int deleteMine(List<String> requestedUrls) {
    User user = currentUser();
    var urls = new LinkedHashSet<>(
      requestedUrls == null
        ? List.<String>of()
        : requestedUrls
          .stream()
          .filter(Objects::nonNull)
          .map(String::trim)
          .filter(value -> !value.isBlank())
          .toList()
    );
    if (urls.isEmpty()) return 0;

    var deletedUrls = new LinkedHashSet<String>();
    for (String url : urls) {
      var owned = managedFileRepository
        .findByFileUrl(url)
        .filter(file -> user.getId().equals(file.getOwnerId()));
      if (owned.isPresent()) {
        try {
          fileUploadService.deleteFileByUrl(url);
          deletedUrls.add(url);
        } catch (RuntimeException ignored) {
          // Keep the database reference when the cloud asset could not be deleted.
        }
      }
    }
    if (deletedUrls.isEmpty()) return 0;

    portfolioRepository
      .findByOwnerId(user.getId())
      .ifPresent(portfolio -> {
        if (
          deletedUrls.contains(portfolio.getAvatarUrl())
        ) portfolio.setAvatarUrl(null);
        if (deletedUrls.contains(portfolio.getCvUrl())) portfolio.setCvUrl(
          null
        );
        if (
          deletedUrls.contains(portfolio.getIntroVideoUrl())
        ) portfolio.setIntroVideoUrl(null);
      });

    var entries =
      profileEntryRepository.findByOwnerIdOrderByTypeAscSortOrderAscStartDateDesc(
        user.getId()
      );
    entries.forEach(entry -> {
      if (deletedUrls.contains(entry.getThumbnailUrl())) entry.setThumbnailUrl(
        null
      );
      if (
        deletedUrls.contains(entry.getSupportingDocumentUrl())
      ) entry.setSupportingDocumentUrl(null);
    });

    var skills =
      skillRepository.findByOwnerIdOrderByCategoryAscSortOrderAscNameAsc(
        user.getId()
      );
    skills.forEach(skill -> {
      if (deletedUrls.contains(skill.getIconUrl())) skill.setIconUrl(null);
    });

    var works =
      workRepository.findByOwnerIdAndDeletedFalseOrderBySortOrderAscCreatedAtDesc(
        user.getId()
      );
    works.forEach(work -> {
      if (deletedUrls.contains(work.getThumbnailUrl())) work.setThumbnailUrl(
        null
      );
      if (work.getGalleryUrls() != null) work
        .getGalleryUrls()
        .removeIf(deletedUrls::contains);
    });
    return deletedUrls.size();
  }

  private User currentUser() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (
      authentication == null || !authentication.isAuthenticated()
    ) throw new UnauthorizedException("Authentication required");
    return userRepository
      .findByEmailAddressIgnoreCaseAndDeletedFalse(authentication.getName())
      .orElseThrow(() -> new UnauthorizedException("Authentication required"));
  }

  private ManagedFileResponse map(ManagedFile file) {
    return new ManagedFileResponse(
      file.getId(),
      file.getFileUrl(),
      file.getOriginalFileName(),
      file.getFileSizeBytes(),
      file.getCategory(),
      file.getUsageType(),
      file.getCreatedAt()
    );
  }
}
