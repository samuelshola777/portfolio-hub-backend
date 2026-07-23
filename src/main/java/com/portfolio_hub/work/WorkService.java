package com.portfolio_hub.work;

import com.portfolio_hub.portfolio.Portfolio;
import com.portfolio_hub.portfolio.PortfolioRepository;
import com.portfolio_hub.userauthmgt.user.User;
import com.portfolio_hub.userauthmgt.user.UserRepository;
import com.portfolio_hub.utils.PaginatedData;
import com.portfolio_hub.utils.exception.InvalidInputException;
import com.portfolio_hub.utils.exception.InvalidOperationException;
import com.portfolio_hub.utils.exception.ResourceExistsException;
import com.portfolio_hub.utils.exception.ResourceNotFoundException;
import com.portfolio_hub.utils.exception.UnauthorizedException;
import com.portfolio_hub.work.request.WorkCreateRequest;
import com.portfolio_hub.work.request.WorkUpdateRequest;
import com.portfolio_hub.work.response.WorkResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkService {

  private final WorkRepository workRepository;
  private final PortfolioRepository portfolioRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public List<WorkResponse> listMine() {
    User user = currentUser();
    return workRepository
      .findByOwnerIdAndDeletedFalseOrderBySortOrderAscCreatedAtDesc(
        user.getId()
      )
      .stream()
      .map(WorkService::mapResponse)
      .toList();
  }

  @Transactional(readOnly = true)
  public PaginatedData<WorkResponse> pageMine(int page, int size) {
    User user = currentUser();
    var result = workRepository.findByOwnerIdAndDeletedFalse(
      user.getId(),
      PageRequest.of(
        Math.max(0, page - 1),
        Math.min(100, Math.max(1, size)),
        Sort.by(Sort.Direction.ASC, "sortOrder").and(
          Sort.by(Sort.Direction.DESC, "createdAt")
        )
      )
    );
    return PaginatedData.from(result, WorkService::mapResponse);
  }

  @Transactional
  public WorkResponse create(WorkCreateRequest request) {
    User user = currentUser();
    workRepository
      .findByOwnerIdAndSlugIgnoreCase(user.getId(), request.slug())
      .ifPresent(existing -> {
        if (!existing.isDeleted()) throw new ResourceExistsException(
          "That work URL is already in use"
        );
        workRepository.delete(existing);
        workRepository.flush();
      });
    boolean ongoing = Boolean.TRUE.equals(request.ongoing());
    if (
      !ongoing && request.completedAt() == null
    ) throw new InvalidInputException(
      "Completion date is required unless the work is ongoing"
    );
    Portfolio portfolio = portfolioRepository
      .findByOwnerId(user.getId())
      .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
    Work work = Work.builder()
      .ownerId(user.getId())
      .portfolioId(portfolio.getId())
      .title(request.title().trim())
      .slug(request.slug().trim().toLowerCase())
      .summary(request.summary().trim())
      .description(clean(request.description()))
      .challengeText(clean(request.challenge()))
      .processText(clean(request.process()))
      .resultsText(clean(request.results()))
      .category(request.category().trim())
      .roleName(clean(request.role()))
      .startedAt(request.startedAt())
      .completedAt(ongoing ? null : request.completedAt())
      .ongoing(ongoing)
      .projectUrl(cleanUrl(request.projectUrl()))
      .sourceUrl(cleanUrl(request.sourceUrl()))
      .thumbnailUrl(cleanUrl(request.thumbnailUrl()))
      .galleryUrls(cleanUrls(request.galleryUrls()))
      .technologyStack(cleanTechnologies(request.technologyStack()))
      .featured(Boolean.TRUE.equals(request.featured()))
      .sortOrder(
        (int) workRepository.countByOwnerIdAndDeletedFalse(user.getId())
      )
      .status(
        request.status() == null
          ? Work.PublicationStatus.PUBLISHED
          : request.status()
      )
      .deleted(false)
      .build();
    return mapResponse(workRepository.save(work));
  }

  @Transactional
  public WorkResponse update(String id, WorkUpdateRequest request) {
    User user = currentUser();
    Work work = find(id, user);
    if (request.title() != null) work.setTitle(request.title().trim());
    if (request.summary() != null) work.setSummary(request.summary().trim());
    if (request.description() != null) work.setDescription(
      clean(request.description())
    );
    if (request.challenge() != null) work.setChallengeText(
      clean(request.challenge())
    );
    if (request.process() != null) work.setProcessText(
      clean(request.process())
    );
    if (request.results() != null) work.setResultsText(
      clean(request.results())
    );
    if (request.category() != null) work.setCategory(request.category().trim());
    if (request.role() != null) work.setRoleName(clean(request.role()));
    if (request.startedAt() != null) work.setStartedAt(request.startedAt());
    if (request.completedAt() != null) work.setCompletedAt(
      request.completedAt()
    );
    if (request.ongoing() != null) {
      work.setOngoing(request.ongoing());
      if (request.ongoing()) work.setCompletedAt(null);
    }
    if (request.projectUrl() != null) work.setProjectUrl(
      cleanUrl(request.projectUrl())
    );
    if (request.sourceUrl() != null) work.setSourceUrl(
      cleanUrl(request.sourceUrl())
    );
    if (request.thumbnailUrl() != null) work.setThumbnailUrl(
      cleanUrl(request.thumbnailUrl())
    );
    if (request.galleryUrls() != null) replaceContents(
      work.getGalleryUrls(),
      cleanUrls(request.galleryUrls())
    );
    if (request.technologyStack() != null) replaceContents(
      work.getTechnologyStack(),
      cleanTechnologies(request.technologyStack())
    );
    if (request.featured() != null) work.setFeatured(request.featured());
    if (request.sortOrder() != null) work.setSortOrder(
      Math.max(0, request.sortOrder())
    );
    if (request.status() != null) work.setStatus(request.status());
    if (
      !work.isOngoing() && work.getCompletedAt() == null
    ) throw new InvalidInputException(
      "Completion date is required unless the work is ongoing"
    );
    /*
     * The work returned by find(...) is already managed by this transaction.
     * Calling save here invokes EntityManager.merge(), which makes Hibernate
     * replace collection elements and fails if an immutable list was ever
     * assigned to the entity. Flush the managed entity directly instead.
     */
    workRepository.flush();
    return mapResponse(work);
  }

  @Transactional
  public void delete(String id) {
    User user = currentUser();
    Work work = find(id, user);
    workRepository.delete(work);
    workRepository.flush();
  }

  private Work find(String id, User user) {
    return workRepository
      .findByIdAndOwnerIdAndDeletedFalse(id, user.getId())
      .orElseThrow(() -> new ResourceNotFoundException("Work not found"));
  }

  private User currentUser() {
    String email = SecurityContextHolder.getContext()
      .getAuthentication()
      .getName();
    User user = userRepository
      .findByEmailAddressIgnoreCaseAndDeletedFalse(email)
      .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    if (
      user.getStatus() != User.AccountStatus.ACTIVE
    ) throw new InvalidOperationException("Account access is restricted");
    return user;
  }

  public static WorkResponse mapResponse(Work w) {
    return new WorkResponse(
      w.getId(),
      w.getTitle(),
      w.getSlug(),
      w.getSummary(),
      w.getDescription(),
      w.getChallengeText(),
      w.getProcessText(),
      w.getResultsText(),
      w.getCategory(),
      w.getRoleName(),
      w.getStartedAt(),
      w.getCompletedAt(),
      w.isOngoing(),
      w.getProjectUrl(),
      w.getSourceUrl(),
      w.getThumbnailUrl(),
      List.copyOf(w.getGalleryUrls()),
      List.copyOf(w.getTechnologyStack()),
      w.isFeatured(),
      w.getSortOrder(),
      w.getStatus(),
      w.getCreatedAt(),
      w.getUpdatedAt()
    );
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String cleanUrl(String value) {
    if (value == null || value.isBlank()) return null;
    String link = value.trim();
    if (
      link.regionMatches(true, 0, "https://", 0, 8) ||
      link.regionMatches(true, 0, "http://", 0, 7)
    ) return link;
    if (link.startsWith("//")) return "https:" + link;
    return "https://" + link;
  }

  private List<String> cleanUrls(List<String> values) {
    if (values == null) return new ArrayList<>();
    if (values.size() > 20) throw new InvalidInputException(
      "A work entry can have at most 20 gallery files"
    );
    return values
      .stream()
      .filter(value -> value != null && !value.isBlank())
      .map(this::cleanUrl)
      .collect(Collectors.toCollection(ArrayList::new));
  }

  private List<String> cleanTechnologies(List<String> values) {
    if (values == null) return new ArrayList<>();
    if (values.size() > 30) throw new InvalidInputException(
      "A work entry can have at most 30 technologies"
    );
    return values
      .stream()
      .filter(value -> value != null && !value.isBlank())
      .map(String::trim)
      .distinct()
      .collect(Collectors.toCollection(ArrayList::new));
  }

  private void replaceContents(List<String> target, List<String> replacement) {
    target.clear();
    target.addAll(replacement);
  }
}
