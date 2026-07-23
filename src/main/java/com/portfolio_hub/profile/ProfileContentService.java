package com.portfolio_hub.profile;

import com.portfolio_hub.profile.request.ProfileEntryRequest;
import com.portfolio_hub.profile.request.SkillRequest;
import com.portfolio_hub.profile.request.SocialLinkRequest;
import com.portfolio_hub.profile.response.ProfileContentResponse;
import com.portfolio_hub.profile.response.ProfileEntryResponse;
import com.portfolio_hub.profile.response.SkillResponse;
import com.portfolio_hub.profile.response.SocialLinkResponse;
import com.portfolio_hub.userauthmgt.user.User;
import com.portfolio_hub.userauthmgt.user.UserRepository;
import com.portfolio_hub.utils.exception.InvalidInputException;
import com.portfolio_hub.utils.exception.InvalidOperationException;
import com.portfolio_hub.utils.exception.ResourceExistsException;
import com.portfolio_hub.utils.exception.ResourceNotFoundException;
import com.portfolio_hub.utils.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileContentService {

  private final ProfileEntryRepository entryRepository;
  private final SkillRepository skillRepository;
  private final SocialLinkRepository socialLinkRepository;
  private final UserRepository userRepository;

  public ProfileContentResponse mine() {
    return forOwner(currentUser().getId());
  }

  public ProfileContentResponse forOwner(String ownerId) {
    return new ProfileContentResponse(
      entryRepository
        .findByOwnerIdOrderByTypeAscSortOrderAscStartDateDesc(ownerId)
        .stream()
        .map(ProfileContentService::map)
        .toList(),
      skillRepository
        .findByOwnerIdOrderByCategoryAscSortOrderAscNameAsc(ownerId)
        .stream()
        .map(ProfileContentService::map)
        .toList(),
      socialLinkRepository
        .findByOwnerIdOrderBySortOrderAscPlatformAsc(ownerId)
        .stream()
        .map(ProfileContentService::map)
        .toList()
    );
  }

  public ProfileContentResponse publicForOwner(String ownerId) {
    ProfileContentResponse content = forOwner(ownerId);
    return new ProfileContentResponse(
      content
        .entries()
        .stream()
        .filter(ProfileEntryResponse::published)
        .toList(),
      content.skills(),
      content.socialLinks()
    );
  }

  public ProfileEntryResponse createEntry(ProfileEntryRequest request) {
    User user = currentUser();
    int order = request.sortOrder() == null
      ? (int) entryRepository.countByOwnerIdAndType(
        user.getId(),
        request.type()
      )
      : Math.max(0, request.sortOrder());
    ProfileEntry entry = ProfileEntry.builder()
      .ownerId(user.getId())
      .type(request.type())
      .title(request.title().trim())
      .organization(clean(request.organization()))
      .subtitle(clean(request.subtitle()))
      .location(clean(request.location()))
      .startDate(request.startDate())
      .endDate(
        Boolean.TRUE.equals(request.current()) ? null : request.endDate()
      )
      .current(Boolean.TRUE.equals(request.current()))
      .description(clean(request.description()))
      .url(cleanUrl(request.url()))
      .thumbnailUrl(cleanUrl(request.thumbnailUrl()))
      .supportingDocumentUrl(cleanUrl(request.supportingDocumentUrl()))
      .published(request.published() == null || request.published())
      .sortOrder(order)
      .build();
    validateDates(entry);
    return map(entryRepository.save(entry));
  }

  public ProfileEntryResponse updateEntry(
    String id,
    ProfileEntryRequest request
  ) {
    User user = currentUser();
    ProfileEntry entry = entryRepository
      .findByIdAndOwnerId(id, user.getId())
      .orElseThrow(() ->
        new ResourceNotFoundException("Profile entry not found")
      );
    entry.setType(request.type());
    entry.setTitle(request.title().trim());
    entry.setOrganization(clean(request.organization()));
    entry.setSubtitle(clean(request.subtitle()));
    entry.setLocation(clean(request.location()));
    entry.setStartDate(request.startDate());
    entry.setCurrent(Boolean.TRUE.equals(request.current()));
    entry.setEndDate(entry.isCurrent() ? null : request.endDate());
    entry.setDescription(clean(request.description()));
    entry.setUrl(cleanUrl(request.url()));
    entry.setThumbnailUrl(cleanUrl(request.thumbnailUrl()));
    entry.setSupportingDocumentUrl(cleanUrl(request.supportingDocumentUrl()));
    entry.setPublished(request.published() == null || request.published());
    if (request.sortOrder() != null) entry.setSortOrder(
      Math.max(0, request.sortOrder())
    );
    validateDates(entry);
    return map(entryRepository.save(entry));
  }

  public void deleteEntry(String id) {
    User user = currentUser();
    entryRepository.delete(
      entryRepository
        .findByIdAndOwnerId(id, user.getId())
        .orElseThrow(() ->
          new ResourceNotFoundException("Profile entry not found")
        )
    );
  }

  public SkillResponse createSkill(SkillRequest request) {
    User user = currentUser();
    if (
      skillRepository.existsByOwnerIdAndNameIgnoreCase(
        user.getId(),
        request.name().trim()
      )
    ) throw new ResourceExistsException("That skill already exists");
    Skill skill = Skill.builder()
      .ownerId(user.getId())
      .name(request.name().trim())
      .category(request.category().trim())
      .proficiency(request.proficiency())
      .iconUrl(cleanUrl(request.iconUrl()))
      .featured(Boolean.TRUE.equals(request.featured()))
      .sortOrder(
        request.sortOrder() == null
          ? (int) skillRepository.countByOwnerId(user.getId())
          : Math.max(0, request.sortOrder())
      )
      .build();
    return map(skillRepository.save(skill));
  }

  public SkillResponse updateSkill(String id, SkillRequest request) {
    User user = currentUser();
    Skill skill = skillRepository
      .findByIdAndOwnerId(id, user.getId())
      .orElseThrow(() -> new ResourceNotFoundException("Skill not found"));
    if (
      skillRepository.existsByOwnerIdAndNameIgnoreCaseAndIdNot(
        user.getId(),
        request.name().trim(),
        id
      )
    ) throw new ResourceExistsException("That skill already exists");
    skill.setName(request.name().trim());
    skill.setCategory(request.category().trim());
    skill.setProficiency(request.proficiency());
    skill.setIconUrl(cleanUrl(request.iconUrl()));
    if (request.sortOrder() != null) skill.setSortOrder(
      Math.max(0, request.sortOrder())
    );
    skill.setFeatured(Boolean.TRUE.equals(request.featured()));
    return map(skillRepository.save(skill));
  }

  public void deleteSkill(String id) {
    User user = currentUser();
    skillRepository.delete(
      skillRepository
        .findByIdAndOwnerId(id, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Skill not found"))
    );
  }

  public SocialLinkResponse createSocial(SocialLinkRequest request) {
    User user = currentUser();
    if (
      socialLinkRepository.existsByOwnerIdAndPlatformIgnoreCase(
        user.getId(),
        request.platform().trim()
      )
    ) throw new ResourceExistsException(
      "That social platform is already listed"
    );
    SocialLink link = SocialLink.builder()
      .ownerId(user.getId())
      .platform(request.platform().trim())
      .url(requiredUrl(request.url()))
      .sortOrder(
        request.sortOrder() == null
          ? (int) socialLinkRepository.countByOwnerId(user.getId())
          : Math.max(0, request.sortOrder())
      )
      .build();
    return map(socialLinkRepository.save(link));
  }

  public SocialLinkResponse updateSocial(String id, SocialLinkRequest request) {
    User user = currentUser();
    SocialLink link = socialLinkRepository
      .findByIdAndOwnerId(id, user.getId())
      .orElseThrow(() ->
        new ResourceNotFoundException("Social link not found")
      );
    if (
      socialLinkRepository.existsByOwnerIdAndPlatformIgnoreCaseAndIdNot(
        user.getId(),
        request.platform().trim(),
        id
      )
    ) throw new ResourceExistsException(
      "That social platform is already listed"
    );
    link.setPlatform(request.platform().trim());
    link.setUrl(requiredUrl(request.url()));
    if (request.sortOrder() != null) link.setSortOrder(
      Math.max(0, request.sortOrder())
    );
    return map(socialLinkRepository.save(link));
  }

  public void deleteSocial(String id) {
    User user = currentUser();
    socialLinkRepository.delete(
      socialLinkRepository
        .findByIdAndOwnerId(id, user.getId())
        .orElseThrow(() ->
          new ResourceNotFoundException("Social link not found")
        )
    );
  }

  public static ProfileEntryResponse map(ProfileEntry e) {
    return new ProfileEntryResponse(
      e.getId(),
      e.getType(),
      e.getTitle(),
      e.getOrganization(),
      e.getSubtitle(),
      e.getLocation(),
      e.getStartDate(),
      e.getEndDate(),
      e.isCurrent(),
      e.getDescription(),
      e.getUrl(),
      e.getThumbnailUrl(),
      e.getSupportingDocumentUrl(),
      e.isPublished(),
      e.getSortOrder()
    );
  }

  public static SkillResponse map(Skill s) {
    return new SkillResponse(
      s.getId(),
      s.getName(),
      s.getCategory(),
      s.getProficiency(),
      s.getIconUrl(),
      s.isFeatured(),
      s.getSortOrder()
    );
  }

  public static SocialLinkResponse map(SocialLink s) {
    return new SocialLinkResponse(
      s.getId(),
      s.getPlatform(),
      s.getUrl(),
      s.getSortOrder()
    );
  }

  private User currentUser() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) throw new UnauthorizedException(
      "Authentication required"
    );
    User user = userRepository
      .findByEmailAddressIgnoreCaseAndDeletedFalse(authentication.getName())
      .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    if (
      user.getStatus() != User.AccountStatus.ACTIVE
    ) throw new InvalidOperationException("Account access is restricted");
    return user;
  }

  private void validateDates(ProfileEntry entry) {
    if (
      !entry.isCurrent() &&
      entry.getStartDate() != null &&
      entry.getEndDate() != null &&
      entry.getEndDate().isBefore(entry.getStartDate())
    ) throw new InvalidInputException("End date cannot be before start date");
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String requiredUrl(String value) {
    String result = cleanUrl(value);
    if (result == null) throw new InvalidInputException("A link is required");
    return result;
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
}
