package com.portfolio_hub.business;

import com.portfolio_hub.subscription.*;
import com.portfolio_hub.userauthmgt.user.*;
import com.portfolio_hub.utils.PaginatedData;
import com.portfolio_hub.utils.emailsenderservice.EmailSenderService;
import com.portfolio_hub.utils.exception.*;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessService {

  private final BusinessRepository businesses;
  private final BusinessItemRepository items;
  private final BusinessOrderRepository orders;
  private final BusinessEnquiryRepository enquiries;
  private final UserRepository users;
  private final EmailSenderService email;
  private final WorkspaceSubscriptionService subscriptionService;
  private final EntitlementService entitlementService;

  public record BusinessData(
    String id,
    String slug,
    String name,
    String tagline,
    String description,
    String industry,
    String category,
    Integer yearEstablished,
    String companySize,
    String registrationNumber,
    String logoUrl,
    String coverUrl,
    String email,
    String phone,
    String websiteUrl,
    String address,
    String socialLinksJson,
    String introVideoUrl,
    String templateKey,
    String accentColor,
    String lightBackground,
    String darkBackground,
    Business.ThemeMode defaultMode,
    Business.Status status,
    Business.OnboardingStage onboardingStage,
    int onboardingPercent
  ) {}

  public record ItemData(
    String id,
    BusinessItem.Type type,
    String title,
    String category,
    String summary,
    String description,
    String thumbnailUrl,
    String mediaJson,
    String configurationJson,
    BigDecimal price,
    BigDecimal discountPrice,
    Integer quantity,
    boolean featured,
    int sortOrder,
    BusinessItem.Status status
  ) {}

  public record OrderData(
    String id,
    String itemId,
    String itemName,
    String customerName,
    String customerEmail,
    String customerPhone,
    String address,
    int quantity,
    String variation,
    String instructions,
    BusinessOrder.Status status,
    java.time.LocalDateTime createdAt
  ) {}

  public record EnquiryData(
    String id,
    BusinessEnquiry.Type type,
    String name,
    String email,
    String company,
    String phone,
    String message,
    BusinessEnquiry.Status status,
    java.time.LocalDateTime createdAt
  ) {}

  public record OnboardingData(
    BusinessData business,
    List<String> missingRequirements,
    long contentItems,
    boolean readyToPublish
  ) {}

  @Transactional(readOnly = true)
  public List<BusinessData> mine() {
    return businesses
      .findByOwnerIdOrderByCreatedAtAsc(currentOwner().getId())
      .stream()
      .map(this::map)
      .toList();
  }

  @Transactional
  public BusinessData create(BusinessController.BusinessRequest r) {
    return createForOwner(currentOwner(), r);
  }

  @Transactional
  public BusinessData createForOwner(
    String ownerId,
    BusinessController.BusinessRequest request
  ) {
    User owner = users
      .findById(ownerId)
      .filter(value -> !value.isDeleted())
      .orElseThrow(() ->
        new ResourceNotFoundException("User account not found")
      );
    return createForOwner(owner, request);
  }

  private BusinessData createForOwner(
    User owner,
    BusinessController.BusinessRequest r
  ) {
    String slug = slug(r.slug());
    if (
      businesses.existsBySlugIgnoreCase(slug)
    ) throw new ResourceExistsException(
      "That permanent business URL is already in use"
    );
    Business b = Business.builder()
      .ownerId(owner.getId())
      .slug(slug)
      .name(required(r.name(), "Business name"))
      .tagline(clean(r.tagline()))
      .description(clean(r.description()))
      .industry(clean(r.industry()))
      .category(clean(r.category()))
      .yearEstablished(r.yearEstablished())
      .companySize(clean(r.companySize()))
      .registrationNumber(clean(r.registrationNumber()))
      .logoUrl(link(r.logoUrl()))
      .coverUrl(link(r.coverUrl()))
      .email(clean(r.email()))
      .phone(clean(r.phone()))
      .websiteUrl(link(r.websiteUrl()))
      .address(clean(r.address()))
      .socialLinksJson(clean(r.socialLinksJson()))
      .introVideoUrl(link(r.introVideoUrl()))
      .templateKey(or(r.templateKey(), randomTheme()))
      .accentColor(or(r.accentColor(), "#5ee6c4"))
      .lightBackground(or(r.lightBackground(), "#f7f7f2"))
      .darkBackground(or(r.darkBackground(), "#07111f"))
      .defaultMode(
        r.defaultMode() == null ? Business.ThemeMode.SYSTEM : r.defaultMode()
      )
      .status(Business.Status.DRAFT)
      .onboardingStage(Business.OnboardingStage.BASICS)
      .build();
    b = businesses.save(b);
    WorkspaceSubscription subscription = subscriptionService.provision(
      owner.getId(),
      WorkspaceType.BUSINESS,
      b.getId(),
      r.subscriptionPlanId()
    );
    if (
      subscription.getStatus() == WorkspaceSubscription.Status.ACTIVE &&
      r.status() != null
    ) {
      b.setStatus(r.status());
      if (r.status() == Business.Status.PUBLISHED) {
        b.setOnboardingStage(Business.OnboardingStage.COMPLETE);
      }
      b = businesses.save(b);
    }
    return map(b);
  }

  @Transactional
  public BusinessData update(
    String businessId,
    BusinessController.BusinessRequest r
  ) {
    Business b = owned(businessId);
    if (r.name() != null) b.setName(required(r.name(), "Business name"));
    if (r.tagline() != null) b.setTagline(clean(r.tagline()));
    if (r.description() != null) b.setDescription(clean(r.description()));
    if (r.industry() != null) b.setIndustry(clean(r.industry()));
    if (r.category() != null) b.setCategory(clean(r.category()));
    if (r.yearEstablished() != null) b.setYearEstablished(r.yearEstablished());
    if (r.companySize() != null) b.setCompanySize(clean(r.companySize()));
    if (r.registrationNumber() != null) b.setRegistrationNumber(
      clean(r.registrationNumber())
    );
    if (r.logoUrl() != null) b.setLogoUrl(link(r.logoUrl()));
    if (r.coverUrl() != null) b.setCoverUrl(link(r.coverUrl()));
    if (r.email() != null) b.setEmail(clean(r.email()));
    if (r.phone() != null) b.setPhone(clean(r.phone()));
    if (r.websiteUrl() != null) b.setWebsiteUrl(link(r.websiteUrl()));
    if (r.address() != null) b.setAddress(clean(r.address()));
    if (r.socialLinksJson() != null) b.setSocialLinksJson(
      clean(r.socialLinksJson())
    );
    if (r.introVideoUrl() != null) b.setIntroVideoUrl(link(r.introVideoUrl()));
    if (r.templateKey() != null) b.setTemplateKey(clean(r.templateKey()));
    if (r.accentColor() != null) b.setAccentColor(clean(r.accentColor()));
    if (r.lightBackground() != null) b.setLightBackground(
      clean(r.lightBackground())
    );
    if (r.darkBackground() != null) b.setDarkBackground(
      clean(r.darkBackground())
    );
    if (r.defaultMode() != null) b.setDefaultMode(r.defaultMode());
    if (r.onboardingStage() != null) advanceOnboarding(b, r.onboardingStage());
    if (r.status() != null && b.getStatus() != Business.Status.SUSPENDED) {
      if (r.status() == Business.Status.PUBLISHED) {
        subscriptionService.requireActive(WorkspaceType.BUSINESS, b.getId());
        requireReadyToPublish(b);
      }
      b.setStatus(r.status());
      if (r.status() == Business.Status.PUBLISHED) {
        b.setOnboardingStage(Business.OnboardingStage.COMPLETE);
      }
    }
    return map(businesses.save(b));
  }

  @Transactional(readOnly = true)
  public OnboardingData onboarding(String businessId) {
    Business business = owned(businessId);
    List<String> missing = missingRequirements(business);
    return new OnboardingData(
      map(business),
      missing,
      items.countByBusinessIdAndDeletedFalse(business.getId()),
      missing.isEmpty()
    );
  }

  @Transactional
  public BusinessData publish(String businessId) {
    Business business = owned(businessId);
    if (business.getStatus() == Business.Status.SUSPENDED) {
      throw new InvalidOperationException(
        "This business is currently suspended. Please contact support for help"
      );
    }
    subscriptionService.requireActive(WorkspaceType.BUSINESS, business.getId());
    requireReadyToPublish(business);
    business.setStatus(Business.Status.PUBLISHED);
    business.setOnboardingStage(Business.OnboardingStage.COMPLETE);
    return map(businesses.save(business));
  }

  @Transactional(readOnly = true)
  public BusinessData publicBusiness(String slug) {
    return map(
      businesses
        .findBySlugIgnoreCaseAndStatus(slug, Business.Status.PUBLISHED)
        .orElseThrow(() ->
          new ResourceNotFoundException("Business website not found")
        )
    );
  }

  @Transactional(readOnly = true)
  public PaginatedData<ItemData> listItems(
    String businessId,
    BusinessItem.Type type,
    int page,
    int size,
    boolean publicOnly
  ) {
    Business b = publicOnly
      ? businesses
        .findBySlugIgnoreCaseAndStatus(businessId, Business.Status.PUBLISHED)
        .orElseThrow(() ->
          new ResourceNotFoundException("Business website not found")
        )
      : owned(businessId);
    Pageable pageable = page(page, size, "sortOrder", Sort.Direction.ASC);
    Page<BusinessItem> result = publicOnly
      ? items.findByBusinessIdAndTypeAndStatusAndDeletedFalse(
        b.getId(),
        type,
        BusinessItem.Status.PUBLISHED,
        pageable
      )
      : items.findByBusinessIdAndTypeAndDeletedFalse(b.getId(), type, pageable);
    return PaginatedData.from(result, this::map);
  }

  @Transactional
  public ItemData createItem(
    String businessId,
    BusinessController.ItemRequest r
  ) {
    Business b = owned(businessId);
    if (r.type() == null) throw new InvalidInputException(
      "Content type is required"
    );
    requireItemCapacity(b.getId(), r.type());
    int order = (int) items.countByBusinessIdAndTypeAndDeletedFalse(
      b.getId(),
      r.type()
    );
    BusinessItem item = BusinessItem.builder()
      .businessId(b.getId())
      .type(r.type())
      .title(required(r.title(), "Title"))
      .category(clean(r.category()))
      .summary(clean(r.summary()))
      .description(clean(r.description()))
      .thumbnailUrl(link(r.thumbnailUrl()))
      .mediaJson(clean(r.mediaJson()))
      .configurationJson(clean(r.configurationJson()))
      .price(r.price())
      .discountPrice(r.discountPrice())
      .quantity(r.quantity())
      .featured(Boolean.TRUE.equals(r.featured()))
      .sortOrder(r.sortOrder() == null ? order : Math.max(0, r.sortOrder()))
      .status(r.status() == null ? BusinessItem.Status.PUBLISHED : r.status())
      .deleted(false)
      .build();
    return map(items.save(item));
  }

  @Transactional
  public ItemData updateItem(
    String businessId,
    String id,
    BusinessController.ItemRequest r
  ) {
    Business b = owned(businessId);
    BusinessItem i = items
      .findByIdAndBusinessIdAndDeletedFalse(id, b.getId())
      .orElseThrow(() ->
        new ResourceNotFoundException("Content item not found")
      );
    if (r.type() != null && r.type() != i.getType()) {
      requireItemCapacity(b.getId(), r.type());
    }
    if (r.type() != null) i.setType(r.type());
    if (r.title() != null) i.setTitle(required(r.title(), "Title"));
    if (r.category() != null) i.setCategory(clean(r.category()));
    if (r.summary() != null) i.setSummary(clean(r.summary()));
    if (r.description() != null) i.setDescription(clean(r.description()));
    if (r.thumbnailUrl() != null) i.setThumbnailUrl(link(r.thumbnailUrl()));
    if (r.mediaJson() != null) i.setMediaJson(clean(r.mediaJson()));
    if (r.configurationJson() != null) i.setConfigurationJson(
      clean(r.configurationJson())
    );
    if (r.price() != null) i.setPrice(r.price());
    if (r.discountPrice() != null) i.setDiscountPrice(r.discountPrice());
    if (r.quantity() != null) i.setQuantity(r.quantity());
    if (r.featured() != null) i.setFeatured(r.featured());
    if (r.sortOrder() != null) i.setSortOrder(Math.max(0, r.sortOrder()));
    if (r.status() != null) i.setStatus(r.status());
    return map(items.save(i));
  }

  @Transactional
  public void deleteItem(String businessId, String id) {
    Business b = owned(businessId);
    BusinessItem i = items
      .findByIdAndBusinessIdAndDeletedFalse(id, b.getId())
      .orElseThrow(() ->
        new ResourceNotFoundException("Content item not found")
      );
    i.setDeleted(true);
    items.save(i);
  }

  @Transactional
  public OrderData placeOrder(String slug, BusinessController.OrderRequest r) {
    Business b = businesses
      .findBySlugIgnoreCaseAndStatus(slug, Business.Status.PUBLISHED)
      .orElseThrow(() ->
        new ResourceNotFoundException("Business website not found")
      );
    entitlementService.requireFeature(
      WorkspaceType.BUSINESS,
      b.getId(),
      EntitlementCode.CART_ORDERS
    );
    BusinessOrder o = orders.save(
      BusinessOrder.builder()
        .businessId(b.getId())
        .itemId(clean(r.itemId()))
        .itemName(required(r.itemName(), "Product or service"))
        .customerName(required(r.customerName(), "Name"))
        .customerEmail(required(r.customerEmail(), "Email").toLowerCase())
        .customerPhone(clean(r.customerPhone()))
        .address(clean(r.address()))
        .quantity(Math.max(1, r.quantity()))
        .variation(clean(r.variation()))
        .instructions(clean(r.instructions()))
        .status(BusinessOrder.Status.NEW)
        .build()
    );
    users
      .findById(b.getOwnerId())
      .ifPresent(owner ->
        email.sendEmail(
          null,
          owner.getEmailAddress(),
          "New order for " + b.getName(),
          "A new order from " +
            o.getCustomerName() +
            " is waiting in your business dashboard."
        )
      );
    email.sendEmail(
      null,
      o.getCustomerEmail(),
      "We received your order",
      "Your order request for " +
        o.getItemName() +
        " was received by " +
        b.getName() +
        "."
    );
    return map(o);
  }

  @Transactional(readOnly = true)
  public PaginatedData<OrderData> orders(
    String businessId,
    int page,
    int size
  ) {
    Business b = owned(businessId);
    return PaginatedData.from(
      orders.findByBusinessId(
        b.getId(),
        page(page, size, "createdAt", Sort.Direction.DESC)
      ),
      this::map
    );
  }

  @Transactional
  public OrderData orderStatus(
    String businessId,
    String id,
    BusinessOrder.Status status
  ) {
    Business b = owned(businessId);
    BusinessOrder o = orders
      .findByIdAndBusinessId(id, b.getId())
      .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    o.setStatus(status);
    return map(orders.save(o));
  }

  @Transactional
  public EnquiryData enquire(String slug, BusinessController.EnquiryRequest r) {
    Business b = businesses
      .findBySlugIgnoreCaseAndStatus(slug, Business.Status.PUBLISHED)
      .orElseThrow(() ->
        new ResourceNotFoundException("Business website not found")
      );
    BusinessEnquiry e = enquiries.save(
      BusinessEnquiry.builder()
        .businessId(b.getId())
        .type(r.type() == null ? BusinessEnquiry.Type.GENERAL : r.type())
        .name(required(r.name(), "Name"))
        .email(required(r.email(), "Email").toLowerCase())
        .company(clean(r.company()))
        .phone(clean(r.phone()))
        .message(required(r.message(), "Message"))
        .status(BusinessEnquiry.Status.NEW)
        .build()
    );
    users
      .findById(b.getOwnerId())
      .ifPresent(owner ->
        email.sendEmail(
          null,
          owner.getEmailAddress(),
          "New business enquiry for " + b.getName(),
          "A new " +
            e.getType().name().toLowerCase() +
            " enquiry is waiting in your dashboard."
        )
      );
    return map(e);
  }

  @Transactional(readOnly = true)
  public PaginatedData<EnquiryData> enquiries(
    String businessId,
    int page,
    int size
  ) {
    Business b = owned(businessId);
    return PaginatedData.from(
      enquiries.findByBusinessId(
        b.getId(),
        page(page, size, "createdAt", Sort.Direction.DESC)
      ),
      this::map
    );
  }

  @Transactional
  public EnquiryData enquiryStatus(
    String businessId,
    String id,
    BusinessEnquiry.Status status
  ) {
    Business b = owned(businessId);
    BusinessEnquiry e = enquiries
      .findByIdAndBusinessId(id, b.getId())
      .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found"));
    e.setStatus(status);
    return map(enquiries.save(e));
  }

  private User currentOwner() {
    String name = SecurityContextHolder.getContext()
      .getAuthentication()
      .getName();
    User u = users
      .findByEmailAddressIgnoreCaseAndDeletedFalse(name)
      .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    if (
      u.getStatus() != User.AccountStatus.ACTIVE
    ) throw new InvalidOperationException("Account access is restricted");
    // Product ownership is intentionally independent of the account role.
    // One signed-in user may own portfolios and businesses at the same time.
    return u;
  }

  private void requireItemCapacity(String businessId, BusinessItem.Type type) {
    EntitlementCode code = switch (type) {
      case PAGE -> EntitlementCode.PAGES;
      case SECTION -> EntitlementCode.SECTIONS;
      case PRODUCT -> EntitlementCode.PRODUCTS;
      case MUSIC_TRACK -> EntitlementCode.MUSIC_TRACKS;
      default -> null;
    };
    if (code == null) return;
    long current = items.countByBusinessIdAndTypeAndDeletedFalse(
      businessId,
      type
    );
    entitlementService.requireCapacity(
      WorkspaceType.BUSINESS,
      businessId,
      code,
      current,
      1
    );
  }

  private Business owned(String id) {
    User u = currentOwner();
    if (u.getRole() == User.UserRole.SUPER_ADMIN) {
      return businesses
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
    }
    return businesses
      .findByIdAndOwnerId(id, u.getId())
      .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
  }

  private Pageable page(int p, int s, String sort, Sort.Direction d) {
    return PageRequest.of(
      Math.max(0, p - 1),
      Math.min(100, Math.max(1, s)),
      Sort.by(d, sort)
    );
  }

  private String clean(String v) {
    return v == null || v.isBlank() ? null : v.trim();
  }

  private String required(String v, String n) {
    String c = clean(v);
    if (c == null) throw new InvalidInputException(n + " is required");
    return c;
  }

  private String link(String v) {
    String c = clean(v);
    if (c == null) return null;
    if (
      c.regionMatches(true, 0, "https://", 0, 8) ||
      c.regionMatches(true, 0, "http://", 0, 7)
    ) return c;
    if (c.startsWith("//")) return "https:" + c;
    return "https://" + c;
  }

  private String or(String v, String f) {
    String c = clean(v);
    return c == null ? f : c;
  }

  private String randomTheme() {
    List<String> themes = List.of("MODERN", "EDITORIAL", "MINIMAL", "BOLD");
    return themes.get(
      java.util.concurrent.ThreadLocalRandom.current().nextInt(themes.size())
    );
  }

  private String slug(String v) {
    String s = required(v, "Business URL")
      .toLowerCase()
      .replaceAll("[^a-z0-9]+", "-")
      .replaceAll("^-|-$", "");
    if (s.length() < 3) throw new InvalidInputException(
      "Business URL must contain at least 3 letters or numbers"
    );
    return s;
  }

  private BusinessData map(Business b) {
    return new BusinessData(
      b.getId(),
      b.getSlug(),
      b.getName(),
      b.getTagline(),
      b.getDescription(),
      b.getIndustry(),
      b.getCategory(),
      b.getYearEstablished(),
      b.getCompanySize(),
      b.getRegistrationNumber(),
      b.getLogoUrl(),
      b.getCoverUrl(),
      b.getEmail(),
      b.getPhone(),
      b.getWebsiteUrl(),
      b.getAddress(),
      b.getSocialLinksJson(),
      b.getIntroVideoUrl(),
      b.getTemplateKey(),
      b.getAccentColor(),
      b.getLightBackground(),
      b.getDarkBackground(),
      b.getDefaultMode(),
      b.getStatus(),
      onboardingStage(b),
      onboardingPercent(b)
    );
  }

  private void requireReadyToPublish(Business business) {
    List<String> missing = missingRequirements(business);
    if (!missing.isEmpty()) {
      throw new InvalidOperationException(
        "Complete these details before publishing: " +
          String.join(", ", missing)
      );
    }
  }

  private List<String> missingRequirements(Business business) {
    List<String> missing = new ArrayList<>();
    if (clean(business.getName()) == null) missing.add("business name");
    if (clean(business.getIndustry()) == null) missing.add("industry");
    if (clean(business.getDescription()) == null) missing.add(
      "business description"
    );
    if (clean(business.getLogoUrl()) == null) missing.add("business logo");
    if (clean(business.getEmail()) == null) missing.add("contact email");
    if (clean(business.getPhone()) == null) missing.add("WhatsApp number");
    if (items.countByBusinessIdAndDeletedFalse(business.getId()) == 0) {
      missing.add("at least one website, product, service or media item");
    }
    return missing;
  }

  private void advanceOnboarding(
    Business business,
    Business.OnboardingStage requested
  ) {
    Business.OnboardingStage current = onboardingStage(business);
    if (requested.ordinal() >= current.ordinal()) {
      business.setOnboardingStage(requested);
    }
  }

  private Business.OnboardingStage onboardingStage(Business business) {
    return business.getOnboardingStage() == null
      ? Business.OnboardingStage.BASICS
      : business.getOnboardingStage();
  }

  private int onboardingPercent(Business business) {
    int completed = onboardingStage(business).ordinal() + 1;
    int total = Business.OnboardingStage.values().length;
    return Math.min(100, Math.round((completed * 100f) / total));
  }

  private ItemData map(BusinessItem i) {
    return new ItemData(
      i.getId(),
      i.getType(),
      i.getTitle(),
      i.getCategory(),
      i.getSummary(),
      i.getDescription(),
      i.getThumbnailUrl(),
      i.getMediaJson(),
      i.getConfigurationJson(),
      i.getPrice(),
      i.getDiscountPrice(),
      i.getQuantity(),
      i.isFeatured(),
      i.getSortOrder(),
      i.getStatus()
    );
  }

  private OrderData map(BusinessOrder o) {
    return new OrderData(
      o.getId(),
      o.getItemId(),
      o.getItemName(),
      o.getCustomerName(),
      o.getCustomerEmail(),
      o.getCustomerPhone(),
      o.getAddress(),
      o.getQuantity(),
      o.getVariation(),
      o.getInstructions(),
      o.getStatus(),
      o.getCreatedAt()
    );
  }

  private EnquiryData map(BusinessEnquiry e) {
    return new EnquiryData(
      e.getId(),
      e.getType(),
      e.getName(),
      e.getEmail(),
      e.getCompany(),
      e.getPhone(),
      e.getMessage(),
      e.getStatus(),
      e.getCreatedAt()
    );
  }
}
