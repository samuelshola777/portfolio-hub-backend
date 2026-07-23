package com.portfolio_hub.admin;

import com.portfolio_hub.admin.request.AccountStatusRequest;
import com.portfolio_hub.admin.request.AdminActivityRequest;
import com.portfolio_hub.admin.request.AdminUserUpdateRequest;
import com.portfolio_hub.admin.response.AdminActivityResponse;
import com.portfolio_hub.admin.response.AdminAnalyticsResponse;
import com.portfolio_hub.admin.response.AdminUserDetailResponse;
import com.portfolio_hub.analytics.PortfolioEvent;
import com.portfolio_hub.analytics.PortfolioEventRepository;
import com.portfolio_hub.announcement.AnnouncementRecipientRepository;
import com.portfolio_hub.business.Business;
import com.portfolio_hub.business.BusinessEnquiry;
import com.portfolio_hub.business.BusinessEnquiryRepository;
import com.portfolio_hub.business.BusinessItem;
import com.portfolio_hub.business.BusinessItemRepository;
import com.portfolio_hub.business.BusinessOrder;
import com.portfolio_hub.business.BusinessOrderRepository;
import com.portfolio_hub.business.BusinessRepository;
import com.portfolio_hub.enquiry.Enquiry;
import com.portfolio_hub.enquiry.EnquiryRepository;
import com.portfolio_hub.enquiry.response.EnquiryResponse;
import com.portfolio_hub.feedback.FeedbackRepository;
import com.portfolio_hub.portfolio.Portfolio;
import com.portfolio_hub.portfolio.PortfolioRepository;
import com.portfolio_hub.portfolio.PortfolioService;
import com.portfolio_hub.profile.ProfileContentService;
import com.portfolio_hub.profile.ProfileEntryRepository;
import com.portfolio_hub.profile.SkillRepository;
import com.portfolio_hub.profile.SocialLinkRepository;
import com.portfolio_hub.profile.response.ProfileEntryResponse;
import com.portfolio_hub.profile.response.SkillResponse;
import com.portfolio_hub.profile.response.SocialLinkResponse;
import com.portfolio_hub.subscription.WorkspaceSubscriptionRepository;
import com.portfolio_hub.userauthmgt.token.UserTokenRepository;
import com.portfolio_hub.userauthmgt.user.AdminUserSpecifications;
import com.portfolio_hub.userauthmgt.user.User;
import com.portfolio_hub.userauthmgt.user.UserRepository;
import com.portfolio_hub.userauthmgt.user.UserService;
import com.portfolio_hub.userauthmgt.user.UsernameAliasRepository;
import com.portfolio_hub.userauthmgt.user.response.UserResponse;
import com.portfolio_hub.utils.PaginatedData;
import com.portfolio_hub.utils.exception.InvalidOperationException;
import com.portfolio_hub.utils.exception.ResourceExistsException;
import com.portfolio_hub.utils.exception.ResourceNotFoundException;
import com.portfolio_hub.utils.fileupload.FileUploadService;
import com.portfolio_hub.utils.fileupload.ManagedFile;
import com.portfolio_hub.utils.fileupload.ManagedFileRepository;
import com.portfolio_hub.work.WorkRepository;
import com.portfolio_hub.work.WorkService;
import com.portfolio_hub.work.response.WorkResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

  private final UserRepository userRepository;
  private final UserService userService;
  private final AuditLogRepository auditLogRepository;
  private final PortfolioRepository portfolioRepository;
  private final ManagedFileRepository managedFileRepository;
  private final PortfolioEventRepository eventRepository;
  private final EnquiryRepository enquiryRepository;
  private final WorkRepository workRepository;
  private final ProfileEntryRepository entryRepository;
  private final SkillRepository skillRepository;
  private final SocialLinkRepository socialLinkRepository;
  private final BusinessRepository businessRepository;
  private final BusinessItemRepository businessItemRepository;
  private final BusinessOrderRepository businessOrderRepository;
  private final BusinessEnquiryRepository businessEnquiryRepository;
  private final UserTokenRepository tokenRepository;
  private final UsernameAliasRepository usernameAliasRepository;
  private final AnnouncementRecipientRepository announcementRecipientRepository;
  private final FeedbackRepository feedbackRepository;
  private final WorkspaceSubscriptionRepository workspaceSubscriptionRepository;
  private final FileUploadService fileUploadService;

  @Value("${application.front-end-url:http://localhost:3000}")
  private String frontEndUrl;

  @Transactional(readOnly = true)
  public PaginatedData<UserResponse> users(
    int page,
    int size,
    String search,
    User.AccountStatus status,
    Boolean verified,
    User.UserRole role,
    boolean includeSuperAdmins
  ) {
    currentActor();
    String searchTerm = cleanSearch(search);
    Pageable pageable = page(
      page,
      size,
      Sort.by(Sort.Direction.DESC, "createdAt")
    );
    Page<User> result = userRepository.findAll(
      AdminUserSpecifications.matching(
        searchTerm,
        status,
        verified,
        role,
        includeSuperAdmins
      ),
      pageable
    );
    return PaginatedData.from(result, userService::map);
  }

  @Transactional(readOnly = true)
  public AdminUserDetailResponse user(String userId) {
    currentActor();
    User user = findUser(userId);
    Portfolio portfolio = portfolioRepository
      .findByOwnerId(userId)
      .orElse(null);
    return new AdminUserDetailResponse(
      userService.map(user),
      user.getUpdatedAt(),
      portfolio == null
        ? null
        : frontEndUrl.replaceAll("/$", "") + "/" + portfolio.getUsername(),
      portfolio == null ? null : PortfolioService.map(portfolio),
      new AdminUserDetailResponse.ContentCounts(
        workRepository.countByOwnerIdAndDeletedFalse(userId),
        entryRepository.countByOwnerId(userId),
        skillRepository.countByOwnerId(userId),
        socialLinkRepository.countByOwnerId(userId),
        enquiryRepository.countByOwnerId(userId),
        eventRepository.countByOwnerId(userId),
        managedFileRepository.countByOwnerId(userId),
        businessRepository.countByOwnerId(userId),
        businessItemRepository.countAllForOwner(userId),
        businessOrderRepository.countAllForOwner(userId),
        businessEnquiryRepository.countAllForOwner(userId)
      )
    );
  }

  @Transactional(readOnly = true)
  public PaginatedData<WorkResponse> projects(
    String userId,
    int page,
    int size
  ) {
    requireUser(userId);
    return PaginatedData.from(
      workRepository.findByOwnerIdAndDeletedFalse(
        userId,
        page(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
      ),
      WorkService::mapResponse
    );
  }

  @Transactional(readOnly = true)
  public PaginatedData<ProfileEntryResponse> entries(
    String userId,
    int page,
    int size
  ) {
    requireUser(userId);
    return PaginatedData.from(
      entryRepository.findByOwnerId(
        userId,
        page(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
      ),
      ProfileContentService::map
    );
  }

  @Transactional(readOnly = true)
  public PaginatedData<SkillResponse> skills(
    String userId,
    int page,
    int size
  ) {
    requireUser(userId);
    return PaginatedData.from(
      skillRepository.findByOwnerId(
        userId,
        page(page, size, Sort.by(Sort.Direction.ASC, "category", "name"))
      ),
      ProfileContentService::map
    );
  }

  @Transactional(readOnly = true)
  public PaginatedData<SocialLinkResponse> socialLinks(
    String userId,
    int page,
    int size
  ) {
    requireUser(userId);
    return PaginatedData.from(
      socialLinkRepository.findByOwnerId(
        userId,
        page(page, size, Sort.by(Sort.Direction.ASC, "sortOrder"))
      ),
      ProfileContentService::map
    );
  }

  @Transactional(readOnly = true)
  public PaginatedData<EnquiryResponse> enquiries(
    String userId,
    int page,
    int size
  ) {
    requireUser(userId);
    return PaginatedData.from(
      enquiryRepository.findByOwnerId(
        userId,
        page(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
      ),
      this::mapEnquiry
    );
  }

  @Transactional(readOnly = true)
  public PaginatedData<PortfolioEvent> analyticsEvents(
    String userId,
    int page,
    int size
  ) {
    requireUser(userId);
    return PaginatedData.from(
      eventRepository.findByOwnerId(
        userId,
        page(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
      ),
      event -> event
    );
  }

  @Transactional(readOnly = true)
  public PaginatedData<Business> businesses(String userId, int page, int size) {
    requireUser(userId);
    return PaginatedData.from(
      businessRepository.findByOwnerId(
        userId,
        page(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
      ),
      business -> business
    );
  }

  @Transactional(readOnly = true)
  public PaginatedData<ManagedFile> files(String userId, int page, int size) {
    requireUser(userId);
    return PaginatedData.from(
      managedFileRepository.findByOwnerId(
        userId,
        page(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
      ),
      file -> file
    );
  }

  @Transactional(readOnly = true)
  public PaginatedData<BusinessItem> businessContent(
    String userId,
    int page,
    int size
  ) {
    requireUser(userId);
    return PaginatedData.from(
      businessItemRepository.findAllForOwner(
        userId,
        page(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
      ),
      item -> item
    );
  }

  @Transactional(readOnly = true)
  public PaginatedData<BusinessOrder> businessOrders(
    String userId,
    int page,
    int size
  ) {
    requireUser(userId);
    return PaginatedData.from(
      businessOrderRepository.findAllForOwner(
        userId,
        page(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
      ),
      order -> order
    );
  }

  @Transactional(readOnly = true)
  public PaginatedData<BusinessEnquiry> businessEnquiries(
    String userId,
    int page,
    int size
  ) {
    requireUser(userId);
    return PaginatedData.from(
      businessEnquiryRepository.findAllForOwner(
        userId,
        page(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
      ),
      enquiry -> enquiry
    );
  }

  @Transactional(readOnly = true)
  public PaginatedData<AdminActivityResponse> activity(int page, int size) {
    currentActor();
    return PaginatedData.from(
      auditLogRepository.findAllByOrderByCreatedAtDesc(
        page(page, size, Sort.unsorted())
      ),
      this::mapActivity
    );
  }

  @Transactional(readOnly = true)
  public PaginatedData<AdminActivityResponse> userActivity(
    String userId,
    int page,
    int size
  ) {
    requireUser(userId);
    return PaginatedData.from(
      auditLogRepository.findByActorIdOrTargetUserIdOrderByCreatedAtDesc(
        userId,
        userId,
        page(page, size, Sort.unsorted())
      ),
      this::mapActivity
    );
  }

  @Transactional
  public AdminActivityResponse createActivity(AdminActivityRequest request) {
    User actor = currentActor();
    String targetUserId = cleanSearch(request.targetUserId());
    if (targetUserId != null) findUser(targetUserId);
    AuditLog log = AuditLog.builder()
      .actorId(actor.getId())
      .targetUserId(targetUserId)
      .action(normalizeAction(request.action()))
      .description(request.description().trim())
      .build();
    return mapActivity(auditLogRepository.save(log));
  }

  @Transactional
  public AdminActivityResponse updateActivity(
    String activityId,
    AdminActivityRequest request
  ) {
    currentActor();
    AuditLog log = auditLogRepository
      .findById(activityId)
      .orElseThrow(() ->
        new ResourceNotFoundException("Activity log not found")
      );
    String targetUserId = cleanSearch(request.targetUserId());
    if (targetUserId != null) findUser(targetUserId);
    log.setAction(normalizeAction(request.action()));
    log.setDescription(request.description().trim());
    log.setTargetUserId(targetUserId);
    return mapActivity(auditLogRepository.save(log));
  }

  @Transactional
  public void permanentlyDeleteActivity(String activityId) {
    currentActor();
    if (
      !auditLogRepository.existsById(activityId)
    ) throw new ResourceNotFoundException("Activity log not found");
    auditLogRepository.deleteById(activityId);
    auditLogRepository.flush();
  }

  @Transactional
  public int permanentlyDeleteActivities(List<String> ids) {
    currentActor();
    List<String> uniqueIds = ids
      .stream()
      .filter(value -> value != null && !value.isBlank())
      .distinct()
      .toList();
    List<AuditLog> logs = auditLogRepository.findAllById(uniqueIds);
    auditLogRepository.deleteAllInBatch(logs);
    return logs.size();
  }

  @Transactional
  public UserResponse updateUser(
    String userId,
    AdminUserUpdateRequest request
  ) {
    User actor = currentActor();
    User target = findUser(userId);
    ensureRegularUser(target);

    String email = request.email().trim().toLowerCase();
    if (
      userRepository.existsByEmailAddressIgnoreCaseAndIdNot(
        email,
        target.getId()
      )
    ) {
      throw new ResourceExistsException("That email is already in use");
    }
    target.setFullName(request.fullName().trim());
    target.setEmailAddress(email);
    target.setWhatsAppNumber(
      com.portfolio_hub.userauthmgt.user.WhatsAppNumber.normalize(
        request.whatsAppNumber()
      )
    );
    target.setEmailVerified(request.emailVerified());
    userRepository.save(target);
    audit(
      actor,
      target,
      "USER_INFORMATION_UPDATED",
      "Account information updated for " + target.getEmailAddress()
    );
    return userService.map(target);
  }

  @Transactional
  public UserResponse changeStatus(
    String userId,
    AccountStatusRequest request
  ) {
    User actor = currentActor();
    User target = findUser(userId);
    if (
      actor.getId().equals(target.getId())
    ) throw new InvalidOperationException(
      "You cannot change your own account status"
    );
    ensureRegularUser(target);
    target.setStatus(request.status());
    userRepository.save(target);
    audit(
      actor,
      target,
      "USER_STATUS_CHANGED",
      "Status changed to " +
        request.status() +
        ". Reason: " +
        request.reason().trim()
    );
    return userService.map(target);
  }

  @Transactional
  public void permanentlyDeleteUser(String userId) {
    User actor = currentActor();
    User target = findUser(userId);
    if (actor.getId().equals(target.getId())) {
      throw new InvalidOperationException(
        "You cannot delete your own administrator account"
      );
    }
    ensureRegularUser(target);

    List<ManagedFile> files = managedFileRepository.findAllByOwnerId(userId);
    files.forEach(file -> {
      try {
        fileUploadService.deleteFileByUrl(file.getFileUrl());
      } catch (RuntimeException ignored) {
        /* Do not leave the account half-deleted because one cloud asset failed. */
      }
    });
    managedFileRepository.deleteAllByOwnerId(userId);
    workspaceSubscriptionRepository.deleteAllByOwnerId(userId);

    List<String> businessIds = businessRepository
      .findByOwnerIdOrderByCreatedAtAsc(userId)
      .stream()
      .map(Business::getId)
      .toList();
    if (!businessIds.isEmpty()) {
      businessEnquiryRepository.deleteAllByBusinessIdIn(businessIds);
      businessOrderRepository.deleteAllByBusinessIdIn(businessIds);
      businessItemRepository.deleteAllByBusinessIdIn(businessIds);
    }
    businessRepository.deleteAllByOwnerId(userId);
    eventRepository.deleteAllByOwnerId(userId);
    enquiryRepository.deleteAllByOwnerId(userId);
    workRepository.deleteAllByOwnerId(userId);
    entryRepository.deleteAllByOwnerId(userId);
    skillRepository.deleteAllByOwnerId(userId);
    socialLinkRepository.deleteAllByOwnerId(userId);
    portfolioRepository.deleteAllByOwnerId(userId);
    tokenRepository.deleteAllByUserId(userId);
    usernameAliasRepository.deleteAllByOwnerId(userId);
    announcementRecipientRepository.deleteAllByUserId(userId);
    feedbackRepository.deleteAllByOwnerId(userId);
    auditLogRepository.deleteAllByActorIdOrTargetUserId(userId, userId);
    userRepository.delete(target);
    auditLogRepository.save(
      AuditLog.builder()
        .actorId(actor.getId())
        .action("USER_PERMANENTLY_DELETED")
        .description(
          "A user account and all associated data were permanently deleted"
        )
        .build()
    );
  }

  @Transactional(readOnly = true)
  public AdminAnalyticsResponse analytics() {
    currentActor();
    LinkedHashMap<LocalDate, Long> growth = new LinkedHashMap<>();
    for (int i = 29; i >= 0; i--) growth.put(LocalDate.now().minusDays(i), 0L);
    userRepository
      .dailyRegistrations(LocalDate.now().minusDays(29).atStartOfDay())
      .forEach(row ->
        growth.computeIfPresent(row.getDay(), (day, count) -> row.getUsers())
      );

    return new AdminAnalyticsResponse(
      userRepository.countByDeletedFalse(),
      userRepository.countByDeletedFalseAndStatus(User.AccountStatus.ACTIVE),
      userRepository.countByDeletedFalseAndEmailVerifiedTrue(),
      portfolioRepository.countByStatus(Portfolio.PublicationStatus.PUBLISHED),
      managedFileRepository.totalStorageBytes(),
      eventRepository.countByEventType(PortfolioEvent.EventType.VIEW),
      eventRepository.countByEventType(PortfolioEvent.EventType.PROJECT_CLICK),
      enquiryRepository.count(),
      enquiryRepository.countByStatus(Enquiry.Status.NEW),
      growth
        .entrySet()
        .stream()
        .map(entry ->
          new AdminAnalyticsResponse.DailyGrowth(
            entry.getKey(),
            entry.getValue()
          )
        )
        .toList(),
      auditLogRepository.count() > 0 || userRepository.countByDeletedFalse() > 0
    );
  }

  private Pageable page(int page, int size, Sort sort) {
    return PageRequest.of(
      Math.max(0, page - 1),
      Math.min(50, Math.max(1, size)),
      sort
    );
  }

  private void requireUser(String userId) {
    currentActor();
    findUser(userId);
  }

  private User currentActor() {
    return userService.currentUser();
  }

  private User findUser(String userId) {
    return userRepository
      .findById(userId)
      .filter(user -> !user.isDeleted())
      .orElseThrow(() -> new ResourceNotFoundException("User not found"));
  }

  private void ensureRegularUser(User user) {
    if (
      user.getRole() == User.UserRole.SUPER_ADMIN
    ) throw new InvalidOperationException(
      "Super administrator accounts are protected"
    );
  }

  private void audit(
    User actor,
    User target,
    String action,
    String description
  ) {
    auditLogRepository.save(
      AuditLog.builder()
        .actorId(actor.getId())
        .targetUserId(target.getId())
        .action(action)
        .description(description)
        .build()
    );
  }

  private AdminActivityResponse mapActivity(AuditLog value) {
    return new AdminActivityResponse(
      value.getId(),
      value.getActorId(),
      value.getTargetUserId(),
      value.getAction(),
      value.getDescription(),
      value.getCreatedAt()
    );
  }

  private EnquiryResponse mapEnquiry(Enquiry value) {
    return new EnquiryResponse(
      value.getId(),
      value.getRecruiterName(),
      value.getRecruiterEmail(),
      value.getCompany(),
      value.getMessage(),
      value.getStatus(),
      value.getCreatedAt()
    );
  }

  private String cleanSearch(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String normalizeAction(String value) {
    String action = value == null
      ? ""
      : value
        .trim()
        .toUpperCase()
        .replaceAll("[^A-Z0-9]+", "_")
        .replaceAll("^_+|_+$", "");
    if (action.isBlank()) throw new InvalidOperationException(
      "Activity action is required"
    );
    return action;
  }
}
