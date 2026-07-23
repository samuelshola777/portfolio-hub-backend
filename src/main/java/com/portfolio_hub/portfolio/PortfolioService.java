package com.portfolio_hub.portfolio;

import com.portfolio_hub.portfolio.request.PortfolioUpdateRequest;
import com.portfolio_hub.portfolio.response.PortfolioResponse;
import com.portfolio_hub.portfolio.response.PublicPortfolioResponse;
import com.portfolio_hub.profile.ProfileContentService;
import com.portfolio_hub.subscription.WorkspaceSubscriptionService;
import com.portfolio_hub.subscription.WorkspaceType;
import com.portfolio_hub.userauthmgt.user.User;
import com.portfolio_hub.userauthmgt.user.UserRepository;
import com.portfolio_hub.userauthmgt.user.UsernameAliasRepository;
import com.portfolio_hub.utils.exception.InvalidOperationException;
import com.portfolio_hub.utils.exception.ResourceNotFoundException;
import com.portfolio_hub.utils.exception.UnauthorizedException;
import com.portfolio_hub.work.Work;
import com.portfolio_hub.work.WorkRepository;
import com.portfolio_hub.work.WorkService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortfolioService {

  private final PortfolioRepository portfolioRepository;
  private final UserRepository userRepository;
  private final UsernameAliasRepository usernameAliasRepository;
  private final WorkRepository workRepository;
  private final ProfileContentService profileContentService;
  private final WorkspaceSubscriptionService subscriptionService;

  public Portfolio createForUser(User user) {
    Portfolio portfolio = portfolioRepository.save(
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
        // A new portfolio is intentionally public immediately. Email verification
        // continues to protect uploads, but it must not hide registration data.
        .status(Portfolio.PublicationStatus.PUBLISHED)
        .publishedAt(LocalDateTime.now())
        .build()
    );
    subscriptionService.provision(
      user.getId(),
      WorkspaceType.PORTFOLIO,
      portfolio.getId(),
      null
    );
    return portfolio;
  }

  public PortfolioResponse mine() {
    return map(findMine());
  }

  public PortfolioResponse updateMine(PortfolioUpdateRequest request) {
    Portfolio portfolio = findMine();
    if (request.headline() != null) portfolio.setHeadline(
      request.headline().trim()
    );
    if (request.introduction() != null) portfolio.setIntroduction(
      request.introduction().trim()
    );
    if (request.note() != null) portfolio.setNote(request.note().trim());
    if (request.availability() != null) portfolio.setAvailability(
      request.availability().trim()
    );
    if (request.avatarUrl() != null) portfolio.setAvatarUrl(
      cleanUrl(request.avatarUrl())
    );
    if (request.cvUrl() != null) portfolio.setCvUrl(cleanUrl(request.cvUrl()));
    if (request.introVideoUrl() != null) portfolio.setIntroVideoUrl(
      cleanUrl(request.introVideoUrl())
    );
    if (request.websiteUrl() != null) portfolio.setWebsiteUrl(
      cleanUrl(request.websiteUrl())
    );
    if (request.githubUsername() != null) portfolio.setGithubUsername(
      request.githubUsername().isBlank()
        ? null
        : request.githubUsername().trim()
    );
    if (request.theme() != null) portfolio.setTheme(request.theme());
    if (request.accent() != null) portfolio.setAccent(request.accent());
    if (request.background() != null) portfolio.setBackground(
      request.background()
    );
    if (request.font() != null) portfolio.setFont(request.font());
    if (request.motion() != null) portfolio.setMotion(request.motion());
    if (request.status() != null) {
      portfolio.setStatus(request.status());
      portfolio.setPublishedAt(
        request.status() == Portfolio.PublicationStatus.PUBLISHED
          ? LocalDateTime.now()
          : null
      );
    }
    return map(portfolioRepository.save(portfolio));
  }

  @Transactional
  public void updateUsername(String ownerId, String username) {
    Portfolio portfolio = portfolioRepository
      .findByOwnerId(ownerId)
      .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
    portfolio.setUsername(username);
    portfolioRepository.save(portfolio);
  }

  @Transactional(readOnly = true)
  public PublicPortfolioResponse getPublic(String username) {
    Portfolio portfolio = portfolioRepository
      .findByUsernameIgnoreCaseAndStatus(
        username,
        Portfolio.PublicationStatus.PUBLISHED
      )
      .orElseGet(() ->
        usernameAliasRepository
          .findByUsernameIgnoreCase(username)
          .flatMap(alias ->
            portfolioRepository.findByOwnerId(alias.getOwnerId())
          )
          .filter(
            value -> value.getStatus() == Portfolio.PublicationStatus.PUBLISHED
          )
          .orElseThrow(() ->
            new ResourceNotFoundException("Portfolio not found")
          )
      );
    User user = userRepository
      .findById(portfolio.getOwnerId())
      .filter(
        value ->
          !value.isDeleted() && value.getStatus() == User.AccountStatus.ACTIVE
      )
      .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
    var works = workRepository
      .findByOwnerIdAndDeletedFalseAndStatusOrderByFeaturedDescSortOrderAscCreatedAtDesc(
        user.getId(),
        Work.PublicationStatus.PUBLISHED
      )
      .stream()
      .map(WorkService::mapResponse)
      .toList();
    var profile = profileContentService.publicForOwner(user.getId());
    return new PublicPortfolioResponse(
      portfolio.getUsername(),
      user.getFullName(),
      portfolio.getHeadline(),
      portfolio.getIntroduction(),
      portfolio.getNote(),
      portfolio.getAvailability(),
      portfolio.getAvatarUrl(),
      portfolio.getCvUrl(),
      portfolio.getIntroVideoUrl(),
      portfolio.getWebsiteUrl(),
      portfolio.getGithubUsername(),
      portfolio.getTheme(),
      portfolio.getAccent(),
      portfolio.getBackground(),
      portfolio.getFont(),
      portfolio.getMotion(),
      works,
      profile.entries(),
      profile.skills(),
      profile.socialLinks()
    );
  }

  public Portfolio findMine() {
    String email = SecurityContextHolder.getContext()
      .getAuthentication()
      .getName();
    User user = userRepository
      .findByEmailAddressIgnoreCaseAndDeletedFalse(email)
      .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    if (
      user.getStatus() != User.AccountStatus.ACTIVE
    ) throw new InvalidOperationException("Account access is restricted");
    return portfolioRepository
      .findByOwnerId(user.getId())
      .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
  }

  public static PortfolioResponse map(Portfolio p) {
    return new PortfolioResponse(
      p.getId(),
      p.getUsername(),
      p.getHeadline(),
      p.getIntroduction(),
      p.getNote(),
      p.getAvailability(),
      p.getAvatarUrl(),
      p.getCvUrl(),
      p.getIntroVideoUrl(),
      p.getWebsiteUrl(),
      p.getGithubUsername(),
      p.getTheme(),
      p.getAccent(),
      p.getBackground(),
      p.getFont(),
      p.getMotion(),
      p.getStatus(),
      p.getPublishedAt()
    );
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
