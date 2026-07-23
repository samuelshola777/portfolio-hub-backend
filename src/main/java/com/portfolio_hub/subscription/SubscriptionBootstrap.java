package com.portfolio_hub.subscription;

import com.portfolio_hub.business.BusinessRepository;
import com.portfolio_hub.portfolio.PortfolioRepository;
import com.portfolio_hub.subscription.SubscriptionDtos.EntitlementRequest;
import com.portfolio_hub.subscription.SubscriptionDtos.PlanRequest;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SubscriptionBootstrap implements ApplicationRunner {

  private final SubscriptionPlanRepository plans;
  private final SubscriptionPlanService planService;
  private final WorkspaceSubscriptionService subscriptionService;
  private final PortfolioRepository portfolios;
  private final BusinessRepository businesses;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    createFreePortfolioPlan();
    createFreeBusinessPlan();
    createBusinessPlans();
    portfolios
      .findAll()
      .forEach(portfolio ->
        subscriptionService.provisionLegacyFree(
          portfolio.getOwnerId(),
          WorkspaceType.PORTFOLIO,
          portfolio.getId()
        )
      );
    businesses
      .findAll()
      .forEach(business ->
        subscriptionService.provisionLegacyFree(
          business.getOwnerId(),
          WorkspaceType.BUSINESS,
          business.getId()
        )
      );
  }

  private void createFreePortfolioPlan() {
    if (plans.findByCodeIgnoreCase("FREE_PORTFOLIO").isPresent()) return;
    planService.create(
      new PlanRequest(
        "FREE_PORTFOLIO",
        "Free Portfolio",
        "A permanent free plan for one personal portfolio.",
        WorkspaceType.PORTFOLIO,
        BigDecimal.ZERO,
        "NGN",
        true,
        true,
        true,
        0,
        List.of(
          entitlement(EntitlementCode.PAGES, "1"),
          entitlement(EntitlementCode.SECTIONS, "10"),
          entitlement(EntitlementCode.STORAGE_MB, "100"),
          entitlement(EntitlementCode.VIDEO_BACKGROUNDS, "false"),
          entitlement(EntitlementCode.CUSTOM_DOMAIN, "false"),
          entitlement(EntitlementCode.ADVANCED_ANIMATIONS, "false"),
          entitlement(EntitlementCode.REMOVE_BRANDING, "false")
        )
      )
    );
  }

  private void createFreeBusinessPlan() {
    saveSeededPlan(
      new PlanRequest(
        "FREE_BUSINESS",
        "Free Business",
        "Best for testing an idea or launching a simple business presence. Keep one business website free permanently with 1 page, 8 sections, 5 products, 3 music tracks, 1 team member, 200 MB storage, cart and WhatsApp ordering, 1 branded email template and up to 100 emails each month. Business Hub branding remains visible; custom domains, video backgrounds and advanced animations are not included.",
        WorkspaceType.BUSINESS,
        BigDecimal.ZERO,
        "NGN",
        true,
        true,
        true,
        0,
        List.of(
          entitlement(EntitlementCode.PAGES, "1"),
          entitlement(EntitlementCode.SECTIONS, "8"),
          entitlement(EntitlementCode.PRODUCTS, "5"),
          entitlement(EntitlementCode.MUSIC_TRACKS, "3"),
          entitlement(EntitlementCode.TEAM_MEMBERS, "1"),
          entitlement(EntitlementCode.STORAGE_MB, "200"),
          entitlement(EntitlementCode.EMAIL_TEMPLATES, "1"),
          entitlement(EntitlementCode.MONTHLY_EMAILS, "100"),
          entitlement(EntitlementCode.VIDEO_BACKGROUNDS, "false"),
          entitlement(EntitlementCode.CUSTOM_DOMAIN, "false"),
          entitlement(EntitlementCode.ADVANCED_ANIMATIONS, "false"),
          entitlement(EntitlementCode.REMOVE_BRANDING, "false"),
          entitlement(EntitlementCode.CART_ORDERS, "true"),
          entitlement(EntitlementCode.WHATSAPP_ORDERS, "true")
        )
      )
    );
  }

  private void createBusinessPlans() {
    createBusinessPlan(
      "BUSINESS_STARTER_10K",
      "Starter",
      "Best for freelancers, new stores and small businesses building a professional online presence. Includes 10 pages, 30 sections, 30 products, 10 music tracks, 3 team members and 1 GB storage. Accept cart and WhatsApp orders, create 2 branded email templates and send up to 1,000 emails monthly. Business Hub branding remains visible; custom domains, video backgrounds and advanced animations are not included.",
      "10000",
      10,
      30,
      30,
      10,
      3,
      1000,
      2,
      1000,
      false,
      false,
      false,
      10
    );
    createBusinessPlan(
      "BUSINESS_GROWTH_20K",
      "Growth",
      "Best for growing brands that need more products, content and customer communication. Includes 20 pages, 75 sections, 100 products, 30 music tracks, 8 team members and 3 GB storage. Use cart and WhatsApp ordering, video backgrounds, advanced animations and a custom domain. Create 5 branded email templates and send up to 5,000 emails monthly. Business Hub branding remains visible.",
      "20000",
      20,
      75,
      100,
      30,
      8,
      3000,
      5,
      5000,
      true,
      true,
      false,
      20
    );
    createBusinessPlan(
      "BUSINESS_PRO_30K",
      "Professional",
      "Best for established stores, agencies and creators with active customers. Includes 40 pages, 150 sections, 300 products, 100 music tracks, 20 team members and 7.5 GB storage. Use cart and WhatsApp ordering, video backgrounds, advanced animations, a custom domain and removal of Business Hub branding. Create 10 branded email templates and send up to 15,000 emails monthly.",
      "30000",
      40,
      150,
      300,
      100,
      20,
      7500,
      10,
      15000,
      true,
      true,
      true,
      30
    );
    createBusinessPlan(
      "BUSINESS_SCALE_40K",
      "Scale",
      "Best for larger teams and high-volume businesses managing broad catalogs and audiences. Includes 75 pages, 300 sections, 1,000 products, 300 music tracks, 50 team members and 15 GB storage. Includes cart and WhatsApp ordering, video backgrounds, advanced animations, a custom domain and no Business Hub branding. Create 25 branded email templates and send up to 50,000 emails monthly.",
      "40000",
      75,
      300,
      1000,
      300,
      50,
      15000,
      25,
      50000,
      true,
      true,
      true,
      40
    );
    createBusinessPlan(
      "BUSINESS_PREMIUM_50K",
      "Premium",
      "Best for businesses that want the complete Business Hub experience without core content limits. Includes unlimited pages, sections, products, music tracks, team members and email templates, plus 30 GB storage and up to 100,000 emails monthly. Includes cart and WhatsApp ordering, video backgrounds, advanced animations, a custom domain and complete removal of Business Hub branding.",
      "50000",
      -1,
      -1,
      -1,
      -1,
      -1,
      30000,
      -1,
      100000,
      true,
      true,
      true,
      50
    );
  }

  private void createBusinessPlan(
    String code,
    String name,
    String description,
    String price,
    int pages,
    int sections,
    int products,
    int musicTracks,
    int teamMembers,
    int storageMb,
    int emailTemplates,
    int monthlyEmails,
    boolean videoBackgrounds,
    boolean customDomain,
    boolean removeBranding,
    int sortOrder
  ) {
    saveSeededPlan(
      new PlanRequest(
        code,
        name,
        description,
        WorkspaceType.BUSINESS,
        new BigDecimal(price),
        "NGN",
        false,
        true,
        true,
        sortOrder,
        List.of(
          entitlement(EntitlementCode.PAGES, Integer.toString(pages)),
          entitlement(EntitlementCode.SECTIONS, Integer.toString(sections)),
          entitlement(EntitlementCode.PRODUCTS, Integer.toString(products)),
          entitlement(
            EntitlementCode.MUSIC_TRACKS,
            Integer.toString(musicTracks)
          ),
          entitlement(
            EntitlementCode.TEAM_MEMBERS,
            Integer.toString(teamMembers)
          ),
          entitlement(EntitlementCode.STORAGE_MB, Integer.toString(storageMb)),
          entitlement(
            EntitlementCode.EMAIL_TEMPLATES,
            Integer.toString(emailTemplates)
          ),
          entitlement(
            EntitlementCode.MONTHLY_EMAILS,
            Integer.toString(monthlyEmails)
          ),
          entitlement(
            EntitlementCode.VIDEO_BACKGROUNDS,
            Boolean.toString(videoBackgrounds)
          ),
          entitlement(
            EntitlementCode.CUSTOM_DOMAIN,
            Boolean.toString(customDomain)
          ),
          entitlement(
            EntitlementCode.ADVANCED_ANIMATIONS,
            Boolean.toString(videoBackgrounds)
          ),
          entitlement(
            EntitlementCode.REMOVE_BRANDING,
            Boolean.toString(removeBranding)
          ),
          entitlement(EntitlementCode.CART_ORDERS, "true"),
          entitlement(EntitlementCode.WHATSAPP_ORDERS, "true")
        )
      )
    );
  }

  private void saveSeededPlan(PlanRequest request) {
    plans
      .findByCodeIgnoreCase(request.code())
      .ifPresentOrElse(
        existing -> planService.update(existing.getId(), request),
        () -> planService.create(request)
      );
  }

  private EntitlementRequest entitlement(EntitlementCode code, String value) {
    return new EntitlementRequest(code, value);
  }
}
