package com.portfolio_hub.admin;

import com.portfolio_hub.portfolio.PortfolioService;
import com.portfolio_hub.userauthmgt.user.User;
import com.portfolio_hub.userauthmgt.user.UserRepository;
import com.portfolio_hub.userauthmgt.user.UsernameAliasRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultAdminSeeder implements CommandLineRunner {

  private final UserRepository userRepository;
  private final UsernameAliasRepository usernameAliasRepository;
  private final PasswordEncoder passwordEncoder;
  private final PortfolioService portfolioService;

  @Value("${application.bootstrap.admin-email:}")
  private String adminEmail;

  @Value("${application.bootstrap.admin-username:portfolio-admin}")
  private String adminUsername;

  @Value("${application.bootstrap.admin-password:}")
  private String adminPassword;

  @Override
  public void run(String... args) {
    if (
      adminEmail == null ||
      adminEmail.isBlank() ||
      adminPassword == null ||
      adminPassword.isBlank()
    ) {
      return;
    }

    String email = adminEmail.trim().toLowerCase();
    if (
      !email.contains("@") ||
      userRepository.existsByEmailAddressIgnoreCase(email)
    ) {
      return;
    }

    String base = adminUsername == null
      ? ""
      : adminUsername
        .trim()
        .toLowerCase()
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("^-+|-+$", "");

    if (base.isBlank()) {
      base = "portfolio-admin";
    }

    String username = base.substring(0, Math.min(40, base.length()));
    while (
      userRepository.existsByUsernameIgnoreCase(username) ||
      usernameAliasRepository.existsByUsernameIgnoreCase(username)
    ) {
      username =
        base.substring(0, Math.min(32, base.length())) +
        "-" +
        UUID.randomUUID().toString().substring(0, 6);
    }

    User admin = userRepository.save(
      User.builder()
        .fullName("Super Administrator")
        .emailAddress(email)
        .username(username)
        .password(passwordEncoder.encode(adminPassword))
        .role(User.UserRole.SUPER_ADMIN)
        .status(User.AccountStatus.ACTIVE)
        .emailVerified(true)
        .twoFactorEnabled(false)
        .tokenVersion(0)
        .deleted(false)
        .build()
    );

    portfolioService.createForUser(admin);
  }
}
