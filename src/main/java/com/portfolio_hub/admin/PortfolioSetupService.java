package com.portfolio_hub.admin;

import com.portfolio_hub.admin.request.AdminAccountCreateRequest;
import com.portfolio_hub.admin.request.PortfolioImportMode;
import com.portfolio_hub.admin.response.PortfolioSetupPreview;
import com.portfolio_hub.admin.response.PortfolioSetupResult;
import com.portfolio_hub.portfolio.Portfolio;
import com.portfolio_hub.portfolio.PortfolioRepository;
import com.portfolio_hub.profile.ProfileEntry;
import com.portfolio_hub.profile.ProfileEntryRepository;
import com.portfolio_hub.profile.Skill;
import com.portfolio_hub.profile.SkillRepository;
import com.portfolio_hub.profile.SocialLink;
import com.portfolio_hub.profile.SocialLinkRepository;
import com.portfolio_hub.subscription.WorkspaceSubscriptionService;
import com.portfolio_hub.subscription.WorkspaceType;
import com.portfolio_hub.userauthmgt.user.User;
import com.portfolio_hub.userauthmgt.user.UserRepository;
import com.portfolio_hub.userauthmgt.user.UserService;
import com.portfolio_hub.userauthmgt.user.UsernameAliasRepository;
import com.portfolio_hub.userauthmgt.user.WhatsAppNumber;
import com.portfolio_hub.userauthmgt.user.response.UserResponse;
import com.portfolio_hub.utils.exception.InvalidInputException;
import com.portfolio_hub.utils.exception.ResourceExistsException;
import com.portfolio_hub.utils.exception.ResourceNotFoundException;
import com.portfolio_hub.work.Work;
import com.portfolio_hub.work.WorkRepository;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PortfolioSetupService {

  private static final long MAX_WORKBOOK_SIZE = 5 * 1024 * 1024;
  private static final int HEADER_ROW = 1;

  private final UserRepository userRepository;
  private final UsernameAliasRepository usernameAliasRepository;
  private final PortfolioRepository portfolioRepository;
  private final SkillRepository skillRepository;
  private final ProfileEntryRepository entryRepository;
  private final WorkRepository workRepository;
  private final SocialLinkRepository socialLinkRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserService userService;
  private final AuditLogRepository auditLogRepository;
  private final WorkspaceSubscriptionService subscriptionService;
  private final SecureRandom secureRandom = new SecureRandom();

  @Transactional
  public UserResponse createAccount(AdminAccountCreateRequest request) {
    User actor = requireAdministrator();
    User user = createUser(
      request.fullName(),
      request.email(),
      request.whatsAppNumber(),
      request.username(),
      request.accountType()
    );
    audit(
      actor,
      user,
      "ADMIN_CREATED_ACCOUNT",
      "Administrator created an account for " + user.getEmailAddress()
    );
    userService.sendAccountSetupMessages(user);
    return userService.map(user);
  }

  @Transactional(readOnly = true)
  public PortfolioSetupPreview preview(MultipartFile file, String userId) {
    requireAdministrator();
    ParsedWorkbook parsed = parse(file);
    Optional<User> existing = resolveExistingUser(
      userId,
      parsed.account().email()
    );
    return previewOf(parsed, existing.orElse(null));
  }

  @Transactional
  public PortfolioSetupResult apply(
    MultipartFile file,
    String userId,
    PortfolioImportMode mode
  ) {
    User actor = requireAdministrator();
    ParsedWorkbook parsed = parse(file);
    Optional<User> existing = resolveExistingUser(
      userId,
      parsed.account().email()
    );
    PortfolioSetupPreview preview = previewOf(parsed, existing.orElse(null));
    if (!preview.valid()) {
      throw new InvalidInputException(
        "Please correct the workbook errors shown in the preview before importing"
      );
    }

    boolean accountCreated = existing.isEmpty();
    User user = existing.orElseGet(() ->
      createUser(
        parsed.account().fullName(),
        parsed.account().email(),
        parsed.account().whatsAppNumber(),
        parsed.account().username(),
        parsed.account().accountType()
      )
    );

    if (!accountCreated) {
      updateAccount(user, parsed.account(), mode);
    }
    Portfolio portfolio = ensurePortfolio(user);
    updatePortfolio(portfolio, parsed.profile(), mode);

    int skillsSaved = importSkills(user, parsed.skills(), mode);
    int entriesSaved = importEntries(user, parsed.entries(), mode);
    int projectsSaved = importProjects(
      user,
      portfolio,
      parsed.projects(),
      mode
    );
    int socialLinksSaved = importSocialLinks(user, parsed.socialLinks(), mode);

    audit(
      actor,
      user,
      "PORTFOLIO_SETUP_IMPORTED",
      "Portfolio setup workbook imported for " + user.getEmailAddress()
    );
    if (accountCreated) {
      userService.sendAccountSetupMessages(user);
    }
    return new PortfolioSetupResult(
      userService.map(user),
      accountCreated,
      skillsSaved,
      entriesSaved,
      projectsSaved,
      socialLinksSaved
    );
  }

  private User requireAdministrator() {
    User actor = userService.currentUser();
    if (actor.getRole() != User.UserRole.SUPER_ADMIN) {
      throw new InvalidInputException(
        "You do not have permission to manage portfolio setup"
      );
    }
    return actor;
  }

  private User createUser(
    String fullName,
    String emailValue,
    String whatsAppNumber,
    String usernameValue,
    User.UserRole requestedRole
  ) {
    String email = emailValue.trim().toLowerCase(Locale.ROOT);
    String username = usernameValue.trim().toLowerCase(Locale.ROOT);
    if (userRepository.existsByEmailAddressIgnoreCase(email)) {
      throw new ResourceExistsException(
        "An account already uses this email address"
      );
    }
    if (
      userRepository.existsByUsernameIgnoreCase(username) ||
      usernameAliasRepository.existsByUsernameIgnoreCase(username)
    ) {
      throw new ResourceExistsException(
        "That username is unavailable. Please choose another one"
      );
    }
    if (!username.matches("^[a-z0-9][a-z0-9_-]{3,}[a-z0-9]$")) {
      throw new InvalidInputException(
        "Use at least 5 letters, numbers, hyphens or underscores for the username"
      );
    }

    User.UserRole role = requestedRole == User.UserRole.BUSINESS_OWNER
      ? User.UserRole.BUSINESS_OWNER
      : User.UserRole.PROFESSIONAL;
    byte[] passwordBytes = new byte[36];
    secureRandom.nextBytes(passwordBytes);
    User user = User.builder()
      .fullName(fullName.trim())
      .emailAddress(email)
      .username(username)
      .whatsAppNumber(WhatsAppNumber.normalize(whatsAppNumber))
      .password(
        passwordEncoder.encode(
          Base64.getUrlEncoder().withoutPadding().encodeToString(passwordBytes)
        )
      )
      .role(role)
      .status(User.AccountStatus.ACTIVE)
      .emailVerified(false)
      .twoFactorEnabled(false)
      .tokenVersion(0)
      .deleted(false)
      .build();
    userRepository.save(user);
    if (role == User.UserRole.PROFESSIONAL) {
      ensurePortfolio(user);
    }
    return user;
  }

  private void updateAccount(
    User user,
    AccountRow account,
    PortfolioImportMode mode
  ) {
    if (mode != PortfolioImportMode.FILL_EMPTY || blank(user.getFullName())) {
      user.setFullName(account.fullName().trim());
    }
    if (
      mode != PortfolioImportMode.FILL_EMPTY || blank(user.getWhatsAppNumber())
    ) {
      user.setWhatsAppNumber(
        WhatsAppNumber.normalize(account.whatsAppNumber())
      );
    }
    userRepository.save(user);
  }

  private Portfolio ensurePortfolio(User user) {
    Portfolio portfolio = portfolioRepository
      .findByOwnerId(user.getId())
      .orElseGet(() ->
        portfolioRepository.save(
          Portfolio.builder()
            .ownerId(user.getId())
            .username(user.getUsername())
            .headline("")
            .introduction("")
            .note("")
            .availability("")
            .theme(Portfolio.Theme.ORBIT)
            .font(Portfolio.FontStyle.GEIST)
            .motion(Portfolio.Motion.FULL)
            .status(Portfolio.PublicationStatus.PUBLISHED)
            .publishedAt(LocalDateTime.now())
            .build()
        )
      );
    subscriptionService.provision(
      user.getId(),
      WorkspaceType.PORTFOLIO,
      portfolio.getId(),
      null
    );
    return portfolio;
  }

  private void updatePortfolio(
    Portfolio portfolio,
    ProfileRow profile,
    PortfolioImportMode mode
  ) {
    portfolio.setHeadline(
      mergeText(portfolio.getHeadline(), profile.headline(), mode)
    );
    portfolio.setIntroduction(
      mergeText(portfolio.getIntroduction(), profile.introduction(), mode)
    );
    portfolio.setNote(mergeText(portfolio.getNote(), profile.note(), mode));
    portfolio.setAvailability(
      mergeText(portfolio.getAvailability(), profile.availability(), mode)
    );
    portfolio.setWebsiteUrl(
      mergeUrl(portfolio.getWebsiteUrl(), profile.websiteUrl(), mode)
    );
    portfolio.setGithubUsername(
      mergeText(portfolio.getGithubUsername(), profile.githubUsername(), mode)
    );
    portfolio.setAvatarUrl(
      mergeUrl(portfolio.getAvatarUrl(), profile.avatarUrl(), mode)
    );
    portfolio.setCvUrl(mergeUrl(portfolio.getCvUrl(), profile.cvUrl(), mode));
    portfolio.setIntroVideoUrl(
      mergeUrl(portfolio.getIntroVideoUrl(), profile.introVideoUrl(), mode)
    );
    portfolio.setStatus(Portfolio.PublicationStatus.PUBLISHED);
    if (portfolio.getPublishedAt() == null) {
      portfolio.setPublishedAt(LocalDateTime.now());
    }
    portfolioRepository.save(portfolio);
  }

  private int importSkills(
    User user,
    List<SkillRow> rows,
    PortfolioImportMode mode
  ) {
    if (mode == PortfolioImportMode.REPLACE_SECTIONS) {
      skillRepository.deleteAllByOwnerId(user.getId());
    }
    Map<String, Skill> existing = skillRepository
      .findByOwnerIdOrderByCategoryAscSortOrderAscNameAsc(user.getId())
      .stream()
      .collect(
        Collectors.toMap(
          value -> key(value.getName()),
          Function.identity(),
          (left, right) -> left
        )
      );
    int saved = 0;
    for (SkillRow row : rows) {
      Skill skill = existing.get(key(row.name()));
      if (skill != null && mode == PortfolioImportMode.FILL_EMPTY) {
        continue;
      }
      if (skill == null) {
        skill = Skill.builder().ownerId(user.getId()).build();
      }
      skill.setName(row.name());
      skill.setCategory(row.category());
      skill.setProficiency(row.proficiency());
      skill.setIconUrl(cleanUrl(row.iconUrl()));
      skill.setFeatured(row.featured());
      skill.setSortOrder(row.sortOrder());
      skillRepository.save(skill);
      saved++;
    }
    return saved;
  }

  private int importEntries(
    User user,
    List<EntryRow> rows,
    PortfolioImportMode mode
  ) {
    if (mode == PortfolioImportMode.REPLACE_SECTIONS) {
      entryRepository.deleteAllByOwnerId(user.getId());
    }
    Map<String, ProfileEntry> existing = entryRepository
      .findByOwnerIdOrderByTypeAscSortOrderAscStartDateDesc(user.getId())
      .stream()
      .collect(
        Collectors.toMap(this::entryKey, Function.identity(), (left, right) ->
          left
        )
      );
    int saved = 0;
    for (EntryRow row : rows) {
      ProfileEntry entry = existing.get(entryKey(row));
      if (entry != null && mode == PortfolioImportMode.FILL_EMPTY) {
        continue;
      }
      if (entry == null) {
        entry = ProfileEntry.builder().ownerId(user.getId()).build();
      }
      entry.setType(row.type());
      entry.setTitle(row.title());
      entry.setOrganization(clean(row.organization()));
      entry.setSubtitle(clean(row.subtitle()));
      entry.setLocation(clean(row.location()));
      entry.setStartDate(row.startDate());
      entry.setCurrent(row.current());
      entry.setEndDate(row.current() ? null : row.endDate());
      entry.setDescription(clean(row.description()));
      entry.setUrl(cleanUrl(row.url()));
      entry.setThumbnailUrl(cleanUrl(row.thumbnailUrl()));
      entry.setSupportingDocumentUrl(cleanUrl(row.supportingDocumentUrl()));
      entry.setPublished(row.published());
      entry.setSortOrder(row.sortOrder());
      entryRepository.save(entry);
      saved++;
    }
    return saved;
  }

  private int importProjects(
    User user,
    Portfolio portfolio,
    List<ProjectRow> rows,
    PortfolioImportMode mode
  ) {
    if (mode == PortfolioImportMode.REPLACE_SECTIONS) {
      workRepository.deleteAllByOwnerId(user.getId());
    }
    Map<String, Work> existing = workRepository
      .findByOwnerIdAndDeletedFalseOrderBySortOrderAscCreatedAtDesc(
        user.getId()
      )
      .stream()
      .collect(
        Collectors.toMap(
          value -> key(value.getSlug()),
          Function.identity(),
          (left, right) -> left
        )
      );
    int saved = 0;
    for (ProjectRow row : rows) {
      Work work = existing.get(key(row.slug()));
      if (work != null && mode == PortfolioImportMode.FILL_EMPTY) {
        continue;
      }
      if (work == null) {
        work = Work.builder()
          .ownerId(user.getId())
          .portfolioId(portfolio.getId())
          .deleted(false)
          .build();
      }
      work.setTitle(row.title());
      work.setSlug(row.slug().toLowerCase(Locale.ROOT));
      work.setSummary(row.summary());
      work.setDescription(clean(row.description()));
      work.setChallengeText(clean(row.challenge()));
      work.setProcessText(clean(row.process()));
      work.setResultsText(clean(row.results()));
      work.setCategory(row.category());
      work.setRoleName(clean(row.role()));
      work.setStartedAt(row.startedAt());
      work.setOngoing(row.ongoing());
      work.setCompletedAt(row.ongoing() ? null : row.completedAt());
      work.setProjectUrl(cleanUrl(row.projectUrl()));
      work.setSourceUrl(cleanUrl(row.sourceUrl()));
      work.setThumbnailUrl(cleanUrl(row.thumbnailUrl()));
      work.setGalleryUrls(new ArrayList<>(row.galleryUrls()));
      work.setTechnologyStack(new ArrayList<>(row.technologyStack()));
      work.setFeatured(row.featured());
      work.setSortOrder(row.sortOrder());
      work.setStatus(
        row.published()
          ? Work.PublicationStatus.PUBLISHED
          : Work.PublicationStatus.DRAFT
      );
      workRepository.save(work);
      saved++;
    }
    return saved;
  }

  private int importSocialLinks(
    User user,
    List<SocialRow> rows,
    PortfolioImportMode mode
  ) {
    if (mode == PortfolioImportMode.REPLACE_SECTIONS) {
      socialLinkRepository.deleteAllByOwnerId(user.getId());
    }
    Map<String, SocialLink> existing = socialLinkRepository
      .findByOwnerIdOrderBySortOrderAscPlatformAsc(user.getId())
      .stream()
      .collect(
        Collectors.toMap(
          value -> key(value.getPlatform()),
          Function.identity(),
          (left, right) -> left
        )
      );
    int saved = 0;
    for (SocialRow row : rows) {
      SocialLink link = existing.get(key(row.platform()));
      if (link != null && mode == PortfolioImportMode.FILL_EMPTY) {
        continue;
      }
      if (link == null) {
        link = SocialLink.builder().ownerId(user.getId()).build();
      }
      link.setPlatform(row.platform());
      link.setUrl(cleanUrl(row.url()));
      link.setSortOrder(row.sortOrder());
      socialLinkRepository.save(link);
      saved++;
    }
    return saved;
  }

  private Optional<User> resolveExistingUser(String userId, String email) {
    if (userId != null && !userId.isBlank()) {
      User user = userRepository
        .findById(userId)
        .filter(value -> !value.isDeleted())
        .orElseThrow(() ->
          new ResourceNotFoundException(
            "We could not find the selected user account"
          )
        );
      if (!user.getEmailAddress().equalsIgnoreCase(email)) {
        throw new InvalidInputException(
          "The workbook email does not match the selected user account"
        );
      }
      return Optional.of(user);
    }
    return userRepository.findByEmailAddressIgnoreCaseAndDeletedFalse(email);
  }

  private PortfolioSetupPreview previewOf(
    ParsedWorkbook parsed,
    User existing
  ) {
    List<String> warnings = new ArrayList<>(parsed.warnings());
    List<String> errors = new ArrayList<>(parsed.errors());
    if (
      existing != null &&
      !existing.getUsername().equalsIgnoreCase(parsed.account().username())
    ) {
      warnings.add(
        "The existing username will remain unchanged so the public portfolio link is not broken"
      );
    }
    return new PortfolioSetupPreview(
      errors.isEmpty(),
      existing != null,
      existing == null ? null : existing.getId(),
      parsed.account().fullName(),
      parsed.account().email(),
      parsed.account().whatsAppNumber(),
      parsed.account().username(),
      parsed.skills().size(),
      parsed.entries().size(),
      parsed.projects().size(),
      parsed.socialLinks().size(),
      List.copyOf(warnings),
      List.copyOf(errors)
    );
  }

  private ParsedWorkbook parse(MultipartFile file) {
    validateFile(file);
    List<String> warnings = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
      AccountRow account = parseAccount(workbook, errors);
      ProfileRow profile = parseProfile(workbook, warnings);
      List<SkillRow> skills = parseSkills(workbook, errors);
      List<EntryRow> entries = parseEntries(workbook, errors);
      List<ProjectRow> projects = parseProjects(workbook, errors);
      List<SocialRow> links = parseSocialLinks(workbook, errors);
      return new ParsedWorkbook(
        account,
        profile,
        skills,
        entries,
        projects,
        links,
        warnings,
        errors
      );
    } catch (IOException | RuntimeException exception) {
      if (exception instanceof InvalidInputException invalidInputException) {
        throw invalidInputException;
      }
      throw new InvalidInputException(
        "We could not read this workbook. Download a fresh template and try again"
      );
    } catch (Exception exception) {
      throw new InvalidInputException(
        "We could not read this workbook. Download a fresh template and try again"
      );
    }
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new InvalidInputException(
        "Choose the completed Excel workbook first"
      );
    }
    if (file.getSize() > MAX_WORKBOOK_SIZE) {
      throw new InvalidInputException(
        "The Excel workbook must be smaller than 5 MB"
      );
    }
    String filename = Objects.requireNonNullElse(
      file.getOriginalFilename(),
      ""
    ).toLowerCase(Locale.ROOT);
    if (!filename.endsWith(".xlsx")) {
      throw new InvalidInputException(
        "Upload the completed .xlsx Portfolio Hub template"
      );
    }
  }

  private AccountRow parseAccount(Workbook workbook, List<String> errors) {
    List<Map<String, String>> rows = rows(workbook, "Account", errors);
    Map<String, String> row = rows.isEmpty() ? Map.of() : rows.getFirst();
    String fullName = required(
      row,
      "fullName",
      "Account: enter the user's full name",
      errors
    );
    String email = required(
      row,
      "email",
      "Account: enter the user's email address",
      errors
    ).toLowerCase(Locale.ROOT);
    String phone = required(
      row,
      "whatsAppNumber",
      "Account: enter the user's WhatsApp number",
      errors
    );
    String username = required(
      row,
      "username",
      "Account: choose a username",
      errors
    ).toLowerCase(Locale.ROOT);
    if (!email.isBlank() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
      errors.add("Account: enter a valid email address");
    }
    if (
      !username.isBlank() &&
      !username.matches("^[a-z0-9][a-z0-9_-]{3,}[a-z0-9]$")
    ) {
      errors.add(
        "Account: use at least 5 letters, numbers, hyphens or underscores for the username"
      );
    }
    if (!phone.isBlank()) {
      try {
        phone = WhatsAppNumber.normalize(phone);
      } catch (InvalidInputException exception) {
        errors.add("Account: " + exception.getMessage());
      }
    }
    User.UserRole role = enumValue(
      User.UserRole.class,
      row.getOrDefault("accountType", "PROFESSIONAL"),
      User.UserRole.PROFESSIONAL,
      "Account: accountType must be PROFESSIONAL or BUSINESS_OWNER",
      errors
    );
    if (role != User.UserRole.BUSINESS_OWNER) {
      role = User.UserRole.PROFESSIONAL;
    }
    return new AccountRow(fullName, email, phone, username, role);
  }

  private ProfileRow parseProfile(Workbook workbook, List<String> warnings) {
    List<Map<String, String>> rows = rows(workbook, "Profile", warnings);
    Map<String, String> row = rows.isEmpty() ? Map.of() : rows.getFirst();
    return new ProfileRow(
      row.get("headline"),
      row.get("introduction"),
      row.get("note"),
      row.get("availability"),
      row.get("websiteUrl"),
      row.get("githubUsername"),
      row.get("avatarUrl"),
      row.get("cvUrl"),
      row.get("introVideoUrl")
    );
  }

  private List<SkillRow> parseSkills(Workbook workbook, List<String> errors) {
    List<SkillRow> result = new ArrayList<>();
    List<Map<String, String>> rows = rows(workbook, "Skills", errors);
    for (int index = 0; index < rows.size(); index++) {
      Map<String, String> row = rows.get(index);
      String prefix = "Skills row " + (index + 3) + ": ";
      String name = required(
        row,
        "name",
        prefix + "enter a skill name",
        errors
      );
      String category = required(
        row,
        "category",
        prefix + "enter a category",
        errors
      );
      Skill.Proficiency proficiency = enumValue(
        Skill.Proficiency.class,
        row.get("proficiency"),
        Skill.Proficiency.INTERMEDIATE,
        prefix + "choose BEGINNER, INTERMEDIATE, ADVANCED or EXPERT",
        errors
      );
      result.add(
        new SkillRow(
          name,
          category,
          proficiency,
          row.get("iconUrl"),
          bool(row.get("featured"), false),
          integer(row.get("sortOrder"), index)
        )
      );
    }
    return result;
  }

  private List<EntryRow> parseEntries(Workbook workbook, List<String> errors) {
    List<EntryRow> result = new ArrayList<>();
    List<Map<String, String>> rows = rows(workbook, "Background", errors);
    for (int index = 0; index < rows.size(); index++) {
      Map<String, String> row = rows.get(index);
      String prefix = "Background row " + (index + 3) + ": ";
      ProfileEntry.EntryType type = enumValue(
        ProfileEntry.EntryType.class,
        row.get("type"),
        null,
        prefix + "choose a valid background type",
        errors
      );
      String title = required(row, "title", prefix + "enter a title", errors);
      LocalDate start = date(
        row.get("startDate"),
        prefix + "startDate",
        errors
      );
      boolean current = bool(row.get("current"), false);
      LocalDate end = date(row.get("endDate"), prefix + "endDate", errors);
      if (!current && start != null && end != null && end.isBefore(start)) {
        errors.add(prefix + "endDate cannot be earlier than startDate");
      }
      result.add(
        new EntryRow(
          type,
          title,
          row.get("organization"),
          row.get("subtitle"),
          row.get("location"),
          start,
          end,
          current,
          row.get("description"),
          row.get("url"),
          row.get("thumbnailUrl"),
          row.get("supportingDocumentUrl"),
          bool(row.get("published"), true),
          integer(row.get("sortOrder"), index)
        )
      );
    }
    return result;
  }

  private List<ProjectRow> parseProjects(
    Workbook workbook,
    List<String> errors
  ) {
    List<ProjectRow> result = new ArrayList<>();
    List<Map<String, String>> rows = rows(workbook, "Projects", errors);
    for (int index = 0; index < rows.size(); index++) {
      Map<String, String> row = rows.get(index);
      String prefix = "Projects row " + (index + 3) + ": ";
      String title = required(row, "title", prefix + "enter a title", errors);
      String slug = required(
        row,
        "slug",
        prefix + "enter a project URL name",
        errors
      ).toLowerCase(Locale.ROOT);
      if (!slug.isBlank() && !slug.matches("^[a-z0-9]+(?:-[a-z0-9]+)*$")) {
        errors.add(
          prefix + "slug must use lowercase letters, numbers and single hyphens"
        );
      }
      String summary = required(
        row,
        "summary",
        prefix + "enter a summary",
        errors
      );
      String category = required(
        row,
        "category",
        prefix + "enter a category",
        errors
      );
      boolean ongoing = bool(row.get("ongoing"), false);
      LocalDate completedAt = date(
        row.get("completedAt"),
        prefix + "completedAt",
        errors
      );
      if (!ongoing && completedAt == null) {
        errors.add(prefix + "completedAt is required unless ongoing is TRUE");
      }
      result.add(
        new ProjectRow(
          title,
          slug,
          summary,
          row.get("description"),
          row.get("challenge"),
          row.get("process"),
          row.get("results"),
          category,
          row.get("role"),
          date(row.get("startedAt"), prefix + "startedAt", errors),
          completedAt,
          ongoing,
          row.get("projectUrl"),
          row.get("sourceUrl"),
          row.get("thumbnailUrl"),
          split(row.get("galleryUrls")),
          split(row.get("technologyStack")),
          bool(row.get("featured"), false),
          bool(row.get("published"), true),
          integer(row.get("sortOrder"), index)
        )
      );
    }
    return result;
  }

  private List<SocialRow> parseSocialLinks(
    Workbook workbook,
    List<String> errors
  ) {
    List<SocialRow> result = new ArrayList<>();
    List<Map<String, String>> rows = rows(workbook, "Social Links", errors);
    for (int index = 0; index < rows.size(); index++) {
      Map<String, String> row = rows.get(index);
      String prefix = "Social Links row " + (index + 3) + ": ";
      result.add(
        new SocialRow(
          required(row, "platform", prefix + "enter a platform", errors),
          required(row, "url", prefix + "enter a profile link", errors),
          integer(row.get("sortOrder"), index)
        )
      );
    }
    return result;
  }

  private List<Map<String, String>> rows(
    Workbook workbook,
    String sheetName,
    List<String> messages
  ) {
    Sheet sheet = workbook.getSheet(sheetName);
    if (sheet == null) {
      messages.add("The " + sheetName + " sheet is missing");
      return List.of();
    }
    Row header = sheet.getRow(HEADER_ROW);
    if (header == null) {
      messages.add(
        "The " + sheetName + " sheet does not contain column headings"
      );
      return List.of();
    }
    Map<Integer, String> columns = new LinkedHashMap<>();
    for (Cell cell : header) {
      String value = cellText(cell).replace("*", "").trim();
      if (!value.isBlank()) {
        columns.put(cell.getColumnIndex(), value);
      }
    }
    List<Map<String, String>> result = new ArrayList<>();
    for (
      int rowIndex = HEADER_ROW + 1;
      rowIndex <= sheet.getLastRowNum();
      rowIndex++
    ) {
      Row row = sheet.getRow(rowIndex);
      if (row == null) {
        continue;
      }
      Map<String, String> values = new LinkedHashMap<>();
      boolean populated = false;
      for (Map.Entry<Integer, String> column : columns.entrySet()) {
        String value = cellText(row.getCell(column.getKey()));
        values.put(column.getValue(), value);
        populated |= !value.isBlank();
      }
      if (populated) {
        result.add(values);
      }
    }
    return result;
  }

  private String cellText(Cell cell) {
    if (cell == null) {
      return "";
    }
    if (
      cell.getCellType() == CellType.NUMERIC &&
      DateUtil.isCellDateFormatted(cell)
    ) {
      return cell
        .getDateCellValue()
        .toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString();
    }
    return new DataFormatter(Locale.ROOT).formatCellValue(cell).trim();
  }

  private String required(
    Map<String, String> row,
    String field,
    String message,
    List<String> errors
  ) {
    String value = clean(row.get(field));
    if (value == null) {
      errors.add(message);
      return "";
    }
    return value;
  }

  private <E extends Enum<E>> E enumValue(
    Class<E> type,
    String value,
    E fallback,
    String message,
    List<String> errors
  ) {
    if (value == null || value.isBlank()) {
      if (fallback != null) {
        return fallback;
      }
      errors.add(message);
      return null;
    }
    try {
      return Enum.valueOf(
        type,
        value.trim().toUpperCase(Locale.ROOT).replace(' ', '_')
      );
    } catch (IllegalArgumentException exception) {
      errors.add(message);
      return fallback;
    }
  }

  private LocalDate date(String value, String field, List<String> errors) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value.trim());
    } catch (RuntimeException exception) {
      errors.add(field + " must use YYYY-MM-DD");
      return null;
    }
  }

  private boolean bool(String value, boolean fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    return (
      value.equalsIgnoreCase("true") ||
      value.equalsIgnoreCase("yes") ||
      value.equals("1")
    );
  }

  private int integer(String value, int fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    try {
      return Math.max(0, Integer.parseInt(value.replace(".0", "").trim()));
    } catch (NumberFormatException exception) {
      return fallback;
    }
  }

  private List<String> split(String value) {
    if (value == null || value.isBlank()) {
      return new ArrayList<>();
    }
    return java.util.Arrays.stream(value.split("[|,\\n]"))
      .map(String::trim)
      .filter(part -> !part.isBlank())
      .distinct()
      .collect(Collectors.toCollection(ArrayList::new));
  }

  private String mergeText(
    String current,
    String incoming,
    PortfolioImportMode mode
  ) {
    if (incoming == null || incoming.isBlank()) {
      return mode == PortfolioImportMode.REPLACE_SECTIONS ? null : current;
    }
    return mode == PortfolioImportMode.FILL_EMPTY && !blank(current)
      ? current
      : incoming.trim();
  }

  private String mergeUrl(
    String current,
    String incoming,
    PortfolioImportMode mode
  ) {
    String merged = mergeText(current, incoming, mode);
    return cleanUrl(merged);
  }

  private String cleanUrl(String value) {
    String cleaned = clean(value);
    if (cleaned == null) {
      return null;
    }
    if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
      return cleaned;
    }
    return "https://" + cleaned;
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private String key(String value) {
    return Objects.requireNonNullElse(value, "")
      .trim()
      .toLowerCase(Locale.ROOT);
  }

  private String entryKey(ProfileEntry entry) {
    return key(
      entry.getType() +
        "|" +
        entry.getTitle() +
        "|" +
        Objects.requireNonNullElse(entry.getOrganization(), "")
    );
  }

  private String entryKey(EntryRow entry) {
    return key(
      entry.type() +
        "|" +
        entry.title() +
        "|" +
        Objects.requireNonNullElse(entry.organization(), "")
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

  private record AccountRow(
    String fullName,
    String email,
    String whatsAppNumber,
    String username,
    User.UserRole accountType
  ) {}

  private record ProfileRow(
    String headline,
    String introduction,
    String note,
    String availability,
    String websiteUrl,
    String githubUsername,
    String avatarUrl,
    String cvUrl,
    String introVideoUrl
  ) {}

  private record SkillRow(
    String name,
    String category,
    Skill.Proficiency proficiency,
    String iconUrl,
    boolean featured,
    int sortOrder
  ) {}

  private record EntryRow(
    ProfileEntry.EntryType type,
    String title,
    String organization,
    String subtitle,
    String location,
    LocalDate startDate,
    LocalDate endDate,
    boolean current,
    String description,
    String url,
    String thumbnailUrl,
    String supportingDocumentUrl,
    boolean published,
    int sortOrder
  ) {}

  private record ProjectRow(
    String title,
    String slug,
    String summary,
    String description,
    String challenge,
    String process,
    String results,
    String category,
    String role,
    LocalDate startedAt,
    LocalDate completedAt,
    boolean ongoing,
    String projectUrl,
    String sourceUrl,
    String thumbnailUrl,
    List<String> galleryUrls,
    List<String> technologyStack,
    boolean featured,
    boolean published,
    int sortOrder
  ) {}

  private record SocialRow(String platform, String url, int sortOrder) {}

  private record ParsedWorkbook(
    AccountRow account,
    ProfileRow profile,
    List<SkillRow> skills,
    List<EntryRow> entries,
    List<ProjectRow> projects,
    List<SocialRow> socialLinks,
    List<String> warnings,
    List<String> errors
  ) {}
}
