package com.portfolio_hub.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.portfolio_hub.portfolio.response.PublicPortfolioResponse;
import com.portfolio_hub.profile.ProfileContentService;
import com.portfolio_hub.profile.response.ProfileContentResponse;
import com.portfolio_hub.userauthmgt.user.User;
import com.portfolio_hub.userauthmgt.user.UserRepository;
import com.portfolio_hub.userauthmgt.user.UsernameAliasRepository;
import com.portfolio_hub.work.WorkRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

  @Mock
  private PortfolioRepository portfolioRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UsernameAliasRepository usernameAliasRepository;

  @Mock
  private WorkRepository workRepository;

  @Mock
  private ProfileContentService profileContentService;

  @InjectMocks
  private PortfolioService portfolioService;

  @Test
  void createsNewPortfolioAsPublic() {
    User user = unverifiedUser();
    when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(
      invocation -> invocation.getArgument(0)
    );

    Portfolio portfolio = portfolioService.createForUser(user);

    assertEquals(Portfolio.PublicationStatus.PUBLISHED, portfolio.getStatus());
    assertNotNull(portfolio.getPublishedAt());
    assertEquals(user.getId(), portfolio.getOwnerId());
    assertEquals(user.getUsername(), portfolio.getUsername());
  }

  @Test
  void returnsEmptyPublicPortfolioForUnverifiedUser() {
    User user = unverifiedUser();
    when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(
      invocation -> invocation.getArgument(0)
    );
    Portfolio portfolio = portfolioService.createForUser(user);
    ProfileContentResponse emptyProfile = new ProfileContentResponse(
      List.of(),
      List.of(),
      List.of()
    );

    when(
      portfolioRepository.findByUsernameIgnoreCaseAndStatus(
        user.getUsername(),
        Portfolio.PublicationStatus.PUBLISHED
      )
    ).thenReturn(Optional.of(portfolio));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(
      workRepository.findByOwnerIdAndDeletedFalseAndStatusOrderByFeaturedDescSortOrderAscCreatedAtDesc(
        user.getId(),
        com.portfolio_hub.work.Work.PublicationStatus.PUBLISHED
      )
    ).thenReturn(List.of());
    when(profileContentService.forOwner(user.getId())).thenReturn(emptyProfile);

    PublicPortfolioResponse response = portfolioService.getPublic(
      user.getUsername()
    );

    assertEquals(user.getFullName(), response.fullName());
    assertEquals(user.getUsername(), response.username());
    assertEquals(List.of(), response.works());
    assertEquals(List.of(), response.profileEntries());
    assertEquals(List.of(), response.skills());
    assertEquals(List.of(), response.socialLinks());
  }

  private User unverifiedUser() {
    User user = User.builder()
      .fullName("New User")
      .emailAddress("new@example.com")
      .username("new-user")
      .password("encoded-password")
      .role(User.UserRole.PROFESSIONAL)
      .status(User.AccountStatus.ACTIVE)
      .emailVerified(false)
      .build();
    user.setId("user-1");
    return user;
  }
}
