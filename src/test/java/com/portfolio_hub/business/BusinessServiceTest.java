package com.portfolio_hub.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.portfolio_hub.subscription.EntitlementService;
import com.portfolio_hub.subscription.WorkspaceSubscriptionService;
import com.portfolio_hub.subscription.WorkspaceType;
import com.portfolio_hub.userauthmgt.user.User;
import com.portfolio_hub.userauthmgt.user.UserRepository;
import com.portfolio_hub.utils.emailsenderservice.EmailSenderService;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class BusinessServiceTest {

  @Mock
  private BusinessRepository businesses;

  @Mock
  private BusinessItemRepository items;

  @Mock
  private BusinessOrderRepository orders;

  @Mock
  private BusinessEnquiryRepository enquiries;

  @Mock
  private UserRepository users;

  @Mock
  private EmailSenderService email;

  @Mock
  private WorkspaceSubscriptionService subscriptionService;

  @Mock
  private EntitlementService entitlementService;

  @InjectMocks
  private BusinessService service;

  private User owner;

  @BeforeEach
  void authenticateOwner() {
    owner = User.builder()
      .fullName("Business Owner")
      .emailAddress("owner@example.com")
      .username("owner")
      .password("encoded")
      .role(User.UserRole.BUSINESS_OWNER)
      .status(User.AccountStatus.ACTIVE)
      .build();
    owner.setId("owner-1");
    SecurityContextHolder.getContext().setAuthentication(
      new UsernamePasswordAuthenticationToken(owner.getEmailAddress(), null)
    );
    when(
      users.findByEmailAddressIgnoreCaseAndDeletedFalse(owner.getEmailAddress())
    ).thenReturn(Optional.of(owner));
  }

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void explainsWhatIsMissingBeforePublishing() {
    Business business = draftBusiness();
    when(businesses.findByIdAndOwnerId("business-1", owner.getId())).thenReturn(
      Optional.of(business)
    );
    when(items.countByBusinessIdAndDeletedFalse("business-1")).thenReturn(0L);

    BusinessService.OnboardingData result = service.onboarding("business-1");

    assertFalse(result.readyToPublish());
    assertTrue(result.missingRequirements().contains("business description"));
    assertTrue(result.missingRequirements().contains("business logo"));
    assertTrue(result.missingRequirements().contains("WhatsApp number"));
  }

  @Test
  void publishesACompleteBusinessAndFinishesOnboarding() {
    Business business = draftBusiness();
    business.setDescription("We help customers grow.");
    business.setLogoUrl("https://cdn.example.com/logo.png");
    business.setPhone("+2348012345678");
    when(businesses.findByIdAndOwnerId("business-1", owner.getId())).thenReturn(
      Optional.of(business)
    );
    when(items.countByBusinessIdAndDeletedFalse("business-1")).thenReturn(1L);
    when(businesses.save(any(Business.class))).thenAnswer(invocation ->
      invocation.getArgument(0)
    );

    BusinessService.BusinessData result = service.publish("business-1");

    verify(subscriptionService).requireActive(
      WorkspaceType.BUSINESS,
      "business-1"
    );
    assertEquals(Business.Status.PUBLISHED, result.status());
    assertEquals(Business.OnboardingStage.COMPLETE, result.onboardingStage());
  }

  private Business draftBusiness() {
    Business business = Business.builder()
      .ownerId(owner.getId())
      .slug("sample-business")
      .name("Sample Business")
      .industry("Retail")
      .email("hello@example.com")
      .templateKey("MODERN")
      .accentColor("#168a73")
      .lightBackground("#f7f7f2")
      .darkBackground("#07111f")
      .defaultMode(Business.ThemeMode.SYSTEM)
      .status(Business.Status.DRAFT)
      .onboardingStage(Business.OnboardingStage.BASICS)
      .build();
    business.setId("business-1");
    return business;
  }
}
