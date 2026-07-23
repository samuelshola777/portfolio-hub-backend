package com.portfolio_hub.userauthmgt.user;

import com.portfolio_hub.admin.AuditLog;
import com.portfolio_hub.admin.AuditLogRepository;
import com.portfolio_hub.portfolio.PortfolioService;
import com.portfolio_hub.userauthmgt.token.UserToken;
import com.portfolio_hub.userauthmgt.token.UserTokenRepository;
import com.portfolio_hub.userauthmgt.user.request.CurrentUserUpdateRequest;
import com.portfolio_hub.userauthmgt.user.request.LoginRequest;
import com.portfolio_hub.userauthmgt.user.request.RegisterRequest;
import com.portfolio_hub.userauthmgt.user.response.AuthResponse;
import com.portfolio_hub.userauthmgt.user.response.TwoFactorSetupResponse;
import com.portfolio_hub.userauthmgt.user.response.UserResponse;
import com.portfolio_hub.utils.appsecurity.JwtService;
import com.portfolio_hub.utils.emailsenderservice.EmailSenderService;
import com.portfolio_hub.utils.exception.InvalidInputException;
import com.portfolio_hub.utils.exception.InvalidOperationException;
import com.portfolio_hub.utils.exception.ResourceExistsException;
import com.portfolio_hub.utils.exception.ResourceNotFoundException;
import com.portfolio_hub.utils.exception.TooManyRequestsException;
import com.portfolio_hub.utils.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final UsernameAliasRepository usernameAliasRepository;
  private final UserTokenRepository tokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final PortfolioService portfolioService;
  private final EmailSenderService emailSenderService;
  private final TwoFactorService twoFactorService;
  private final ApplicationEventPublisher eventPublisher;
  private final AuditLogRepository auditLogRepository;
  private final SecureRandom secureRandom = new SecureRandom();

  @Value("${application.front-end-url}")
  private String frontEndUrl;

  @Transactional
  public Map<String, Object> register(RegisterRequest request) {
    log.info("1. Starting registration for email: {}", request.email());

    log.info("2. Validating email and username");
    String email = request.email().trim().toLowerCase();
    String username = request.username().trim().toLowerCase();

    if (userRepository.existsByEmailAddressIgnoreCase(email)) {
      log.warn("3. Email already exists: {}", email);
      throw new ResourceExistsException("That email is already in use");
    }
    if (userRepository.existsByUsernameIgnoreCase(username)) {
      log.warn("4. Username already exists: {}", username);
      throw new ResourceExistsException("That username is already in use");
    }
    if (usernameAliasRepository.existsByUsernameIgnoreCase(username)) {
      throw new ResourceExistsException(
        "That username belongs to an existing portfolio link"
      );
    }
    log.info("5. Email and username validation passed");

    log.info("6. Creating new user");
    User user = User.builder()
      .fullName(request.fullName().trim())
      .emailAddress(email)
      .username(username)
      .whatsAppNumber(WhatsAppNumber.normalize(request.whatsAppNumber()))
      .password(passwordEncoder.encode(request.password()))
      .role(normalizeRegistrationRole(request.accountType()))
      .status(User.AccountStatus.ACTIVE)
      .emailVerified(false)
      .twoFactorEnabled(false)
      .tokenVersion(0)
      .deleted(false)
      .build();
    user = userRepository.save(user);
    log.info("7. User created with ID: {}", user.getId());

    log.info("8. Creating portfolio for user");
    if (
      user.getRole() == User.UserRole.PROFESSIONAL
    ) portfolioService.createForUser(user);
    auditLogRepository.save(
      AuditLog.builder()
        .actorId(user.getId())
        .targetUserId(user.getId())
        .action("USER_REGISTERED")
        .description(user.getFullName() + " created an account")
        .build()
    );
    log.info("9. Portfolio created for user ID: {}", user.getId());

    log.info("10. Queueing verification email after transaction commit");
    queueVerification(user);
    log.info("11. Verification email queued successfully");

    log.info("12. Registration completed for email: {}", email);
    return Map.of("user", map(user), "verificationEmailSent", true);
  }

  @Transactional
  public AuthResponse login(LoginRequest request) {
    log.info("1. Starting login for email: {}", request.email());

    log.info("2. Finding user by email");
    User user = userRepository
      .findByEmailAddressIgnoreCaseAndDeletedFalse(
        request.email().trim().toLowerCase()
      )
      .orElseThrow(() -> {
        log.warn("3. User not found for email: {}", request.email());
        return new UnauthorizedException("Invalid email or password");
      });
    log.info("4. User found with ID: {}", user.getId());

    log.info("5. Validating password");
    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      log.warn("6. Invalid password for user: {}", user.getEmailAddress());
      throw new UnauthorizedException("Invalid email or password");
    }
    log.info("7. Password validated successfully");

    if (user.getStatus() == User.AccountStatus.BLOCKED) {
      log.warn("8. User account is blocked: {}", user.getEmailAddress());
      throw new UnauthorizedException("This account has been blocked");
    }

    if (user.isTwoFactorEnabled()) {
      log.info("9. User has 2FA enabled, verifying code");
      boolean valid = twoFactorService.verify(
        user.getTwoFactorSecret(),
        request.twoFactorCode()
      );
      if (!valid) {
        log.info("10. Checking recovery code");
        valid = twoFactorService.useRecoveryCode(user, request.twoFactorCode());
      }
      if (!valid) {
        log.warn("11. Invalid 2FA code for user: {}", user.getEmailAddress());
        throw new UnauthorizedException(
          request.twoFactorCode() == null
            ? "Two-factor code is required"
            : "Invalid authenticator or recovery code"
        );
      }
      log.info("12. 2FA verification successful");
    }

    log.info("13. Updating last login time");
    user.setLastLoginAt(LocalDateTime.now());
    userRepository.save(user);
    auditLogRepository.save(
      AuditLog.builder()
        .actorId(user.getId())
        .targetUserId(user.getId())
        .action("USER_LOGGED_IN")
        .description(user.getFullName() + " signed in")
        .build()
    );
    log.info("14. Last login updated for user: {}", user.getEmailAddress());

    log.info("15. Generating tokens");
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", user.getId());
    claims.put("role", user.getRole().name());
    claims.put("tokenVersion", tokenVersion(user));

    AuthResponse response = new AuthResponse(
      jwtService.generateToken(user.getEmailAddress(), claims),
      jwtService.generateRefreshToken(user.getEmailAddress(), claims),
      map(user)
    );
    log.info(
      "16. Login completed successfully for user: {}",
      user.getEmailAddress()
    );
    return response;
  }

  public AuthResponse refresh(String refreshToken) {
    log.info("1. Starting token refresh");

    try {
      log.info("2. Validating refresh token");
      if (!jwtService.isRefreshToken(refreshToken)) {
        log.warn("3. Invalid refresh token format");
        throw new UnauthorizedException("Invalid refresh token");
      }

      log.info("4. Extracting username from token");
      String email = jwtService.extractUsername(refreshToken);
      log.info("5. Email extracted: {}", email);

      log.info("6. Finding user by email");
      User user = userRepository
        .findByEmailAddressIgnoreCaseAndDeletedFalse(email)
        .filter(value -> value.getStatus() != User.AccountStatus.BLOCKED)
        .orElseThrow(() -> {
          log.warn("7. User not found or blocked: {}", email);
          return new UnauthorizedException("Invalid refresh token");
        });
      log.info("8. User found with ID: {}", user.getId());

      log.info("9. Validating token validity");
      if (!jwtService.isTokenValid(refreshToken, user.getEmailAddress())) {
        log.warn("10. Refresh token expired for user: {}", email);
        throw new UnauthorizedException("Refresh token expired");
      }
      if (jwtService.extractTokenVersion(refreshToken) != tokenVersion(user)) {
        throw new UnauthorizedException("This session is no longer valid");
      }
      log.info("11. Token validated successfully");

      log.info("12. Generating new tokens");
      Map<String, Object> claims = new HashMap<>();
      claims.put("userId", user.getId());
      claims.put("role", user.getRole().name());
      claims.put("tokenVersion", tokenVersion(user));

      AuthResponse response = new AuthResponse(
        jwtService.generateToken(user.getEmailAddress(), claims),
        jwtService.generateRefreshToken(user.getEmailAddress(), claims),
        map(user)
      );
      log.info(
        "13. Token refresh completed successfully for user: {}",
        user.getEmailAddress()
      );
      return response;
    } catch (UnauthorizedException exception) {
      log.error(
        "14. Unauthorized exception during refresh: {}",
        exception.getMessage()
      );
      throw exception;
    } catch (Exception exception) {
      log.error(
        "15. Unexpected error during refresh: {}",
        exception.getMessage(),
        exception
      );
      throw new UnauthorizedException("Invalid or expired refresh token");
    }
  }

  public User currentUser() {
    log.info("1. Fetching current user");
    String email = SecurityContextHolder.getContext()
      .getAuthentication()
      .getName();
    log.info("2. Authenticated user email: {}", email);

    User user = userRepository
      .findByEmailAddressIgnoreCaseAndDeletedFalse(email)
      .orElseThrow(() -> {
        log.warn("3. User not found for email: {}", email);
        return new UnauthorizedException("Authentication required");
      });
    log.info("4. User found with ID: {}", user.getId());

    if (user.getStatus() == User.AccountStatus.SUSPENDED) {
      log.warn("5. User account suspended: {}", email);
      throw new InvalidOperationException("Account suspended");
    }
    log.info("6. User is active and authenticated");
    return user;
  }

  public UserResponse currentProfile() {
    log.info("Fetching current user profile");
    UserResponse response = map(currentUser());
    log.info("Current profile fetched successfully");
    return response;
  }

  @Transactional
  public UserResponse updateCurrentProfile(CurrentUserUpdateRequest request) {
    User user = currentUser();
    user.setFullName(request.fullName().trim());
    user.setWhatsAppNumber(WhatsAppNumber.normalize(request.whatsAppNumber()));
    userRepository.save(user);
    return map(user);
  }

  @Transactional
  public void sendAccountSetupMessages(User user) {
    queueVerification(user);
    String raw = createToken(user, UserToken.TokenType.PASSWORD_RESET, 24 * 60);
    emailSenderService.sendPasswordResetEmail(
      user.getEmailAddress(),
      user.getFullName(),
      frontEndUrl + "/reset-password?token=" + raw
    );
  }

  private User.UserRole normalizeRegistrationRole(User.UserRole requested) {
    return requested == User.UserRole.BUSINESS_OWNER
      ? User.UserRole.BUSINESS_OWNER
      : User.UserRole.PROFESSIONAL;
  }

  public void updateUsername(User user, String requestedUsername) {
    String username = requestedUsername.trim().toLowerCase();
    if (!username.matches("^[a-z0-9][a-z0-9_-]{3,}[a-z0-9]$")) {
      throw new InvalidInputException(
        "Username must use at least 5 letters, numbers, hyphens or underscores"
      );
    }
    if (user.getUsername().equalsIgnoreCase(username)) {
      userRepository.save(user);
      return;
    }
    if (
      userRepository.existsByUsernameIgnoreCaseAndIdNot(username, user.getId())
    ) {
      throw new ResourceExistsException("That username is already in use");
    }

    UsernameAlias requestedAlias = usernameAliasRepository
      .findByUsernameIgnoreCase(username)
      .orElse(null);
    if (
      requestedAlias != null &&
      !requestedAlias.getOwnerId().equals(user.getId())
    ) {
      throw new ResourceExistsException(
        "That username belongs to an existing portfolio link"
      );
    }

    String previousUsername = user.getUsername().toLowerCase();
    if (!usernameAliasRepository.existsByUsernameIgnoreCase(previousUsername)) {
      usernameAliasRepository.save(
        UsernameAlias.builder()
          .username(previousUsername)
          .ownerId(user.getId())
          .build()
      );
    }

    user.setUsername(username);
    userRepository.save(user);
    portfolioService.updateUsername(user.getId(), username);
  }

  public void changePassword(String currentPassword, String newPassword) {
    log.info("1. Starting password change");
    User user = currentUser();
    log.info("2. User found: {}", user.getEmailAddress());

    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
      log.warn(
        "3. Incorrect current password for user: {}",
        user.getEmailAddress()
      );
      throw new UnauthorizedException("Current password is incorrect");
    }
    log.info("4. Password validation successful");

    log.info("5. Encoding and saving new password");
    user.setPassword(passwordEncoder.encode(newPassword));
    user.setTokenVersion(tokenVersion(user) + 1);
    userRepository.save(user);
    log.info(
      "6. Password changed successfully for user: {}",
      user.getEmailAddress()
    );
  }

  @Transactional
  public boolean resendVerification(String email) {
    log.info("1. Starting resend verification for email: {}", email);

    User user = userRepository
      .findByEmailAddressIgnoreCaseAndDeletedFalse(
        email == null ? "" : email.trim()
      )
      .orElse(null);
    // Public callers receive the same response whether the address exists or not.
    if (user == null) return true;
    log.info("3. User found with ID: {}", user.getId());

    if (user.isEmailVerified()) {
      log.info("4. Email already verified for user: {}", email);
      return true;
    }

    LocalDateTime cooldownStartedAt = LocalDateTime.now().minusMinutes(2);
    tokenRepository
      .findTopByUserIdAndTypeOrderByCreatedAtDesc(
        user.getId(),
        UserToken.TokenType.EMAIL_VERIFICATION
      )
      .filter(token -> token.getCreatedAt().isAfter(cooldownStartedAt))
      .ifPresent(token -> {
        long seconds = Math.max(
          1,
          java.time.Duration.between(
            LocalDateTime.now(),
            token.getCreatedAt().plusMinutes(2)
          ).toSeconds()
        );
        throw new TooManyRequestsException(
          "A verification email was already sent. Try again in " +
            seconds +
            " seconds."
        );
      });

    log.info("5. Queueing verification email");
    queueVerification(user);
    log.info("6. Verification email queued successfully");
    return true;
  }

  @Transactional
  public boolean resendVerificationForCurrentUser() {
    return resendVerification(currentUser().getEmailAddress());
  }

  @Transactional
  public boolean verifyEmail(String rawToken) {
    log.info("1. Starting email verification");

    log.info("2. Finding token");
    UserToken token = findToken(
      rawToken,
      UserToken.TokenType.EMAIL_VERIFICATION
    );
    log.info("3. Token found for user ID: {}", token.getUserId());

    User user = userRepository
      .findById(token.getUserId())
      .orElseThrow(() -> {
        log.warn("4. User not found with ID: {}", token.getUserId());
        return new ResourceNotFoundException("Account not found");
      });
    log.info("5. User found: {}", user.getEmailAddress());

    log.info("6. Setting email as verified");
    user.setEmailVerified(true);
    userRepository.save(user);
    log.info("7. Email verified for user: {}", user.getEmailAddress());

    log.info("8. Cleaning up verification tokens");
    tokenRepository.deleteAllByUserIdAndType(
      user.getId(),
      UserToken.TokenType.EMAIL_VERIFICATION
    );
    log.info("9. Email verification completed");
    return true;
  }

  public void forgotPassword(String email) {
    log.info("1. Starting forgot password for email: {}", email);
    String trimmedEmail = email.trim().toLowerCase();

    userRepository
      .findByEmailAddressIgnoreCaseAndDeletedFalse(trimmedEmail)
      .ifPresentOrElse(
        user -> {
          log.info("2. User found: {}", user.getEmailAddress());
          log.info("3. Creating password reset token");
          String raw = createToken(
            user,
            UserToken.TokenType.PASSWORD_RESET,
            60
          );
          log.info("4. Sending password reset email");
          emailSenderService.sendPasswordResetEmail(
            user.getEmailAddress(),
            user.getFullName(),
            frontEndUrl + "/reset-password?token=" + raw
          );
          log.info("5. Password reset email sent successfully");
        },
        () -> log.info("2. No user found for email: {}", trimmedEmail)
      );
    log.info("6. Forgot password process completed");
  }

  @Transactional
  public void resetPassword(String rawToken, String password) {
    log.info("1. Starting password reset");

    log.info("2. Finding reset token");
    UserToken token = findToken(rawToken, UserToken.TokenType.PASSWORD_RESET);
    log.info("3. Token found for user ID: {}", token.getUserId());

    User user = userRepository
      .findById(token.getUserId())
      .orElseThrow(() -> {
        log.warn("4. User not found with ID: {}", token.getUserId());
        return new ResourceNotFoundException("Account not found");
      });
    log.info("5. User found: {}", user.getEmailAddress());

    log.info("6. Encoding and setting new password");
    user.setPassword(passwordEncoder.encode(password));
    user.setTokenVersion(tokenVersion(user) + 1);
    userRepository.save(user);
    log.info(
      "7. Password reset completed for user: {}",
      user.getEmailAddress()
    );

    log.info("8. Cleaning up reset tokens");
    tokenRepository.deleteAllByUserIdAndType(
      user.getId(),
      UserToken.TokenType.PASSWORD_RESET
    );
    log.info("9. Password reset process completed");
  }

  public TwoFactorSetupResponse beginTwoFactorSetup() {
    log.info("1. Starting 2FA setup");
    User user = currentUser();
    log.info("2. User: {}", user.getEmailAddress());

    if (user.isTwoFactorEnabled()) {
      log.warn("3. 2FA already enabled for user: {}", user.getEmailAddress());
      throw new InvalidOperationException(
        "Two-factor authentication is already enabled"
      );
    }

    log.info("4. Creating 2FA secret");
    String secret = twoFactorService.createSecret();
    LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);
    user.setTwoFactorSecret(secret);
    user.setTwoFactorSetupExpiresAt(expiresAt);
    userRepository.save(user);
    log.info("5. 2FA secret saved for user: {}", user.getEmailAddress());

    TwoFactorSetupResponse response = new TwoFactorSetupResponse(
      secret,
      twoFactorService.createQrDataUrl(secret, user.getEmailAddress()),
      expiresAt
    );
    log.info("6. 2FA setup initiated for user: {}", user.getEmailAddress());
    return response;
  }

  public List<String> confirmTwoFactor(String code) {
    log.info("1. Confirming 2FA setup");
    User user = currentUser();
    log.info("2. User: {}", user.getEmailAddress());

    if (
      user.getTwoFactorSetupExpiresAt() == null ||
      user.getTwoFactorSetupExpiresAt().isBefore(LocalDateTime.now())
    ) {
      log.warn("3. 2FA setup expired for user: {}", user.getEmailAddress());
      throw new InvalidInputException("Two-factor setup expired. Start again");
    }

    log.info("4. Verifying authenticator code");
    if (!twoFactorService.verify(user.getTwoFactorSecret(), code)) {
      log.warn(
        "5. Invalid authenticator code for user: {}",
        user.getEmailAddress()
      );
      throw new InvalidInputException("That authenticator code is not valid");
    }
    log.info("6. Code verified successfully");

    log.info("7. Creating recovery codes");
    List<String> codes = twoFactorService.createRecoveryCodes();
    user.setTwoFactorEnabled(true);
    user.setTwoFactorSetupExpiresAt(null);
    user.setRecoveryCodeHashes(twoFactorService.hashRecoveryCodes(codes));
    userRepository.save(user);
    log.info("8. 2FA enabled for user: {}", user.getEmailAddress());
    return codes;
  }

  public void disableTwoFactor(String password) {
    log.info("1. Disabling 2FA");
    User user = currentUser();
    log.info("2. User: {}", user.getEmailAddress());

    if (!passwordEncoder.matches(password, user.getPassword())) {
      log.warn("3. Incorrect password for user: {}", user.getEmailAddress());
      throw new UnauthorizedException("Incorrect password");
    }
    log.info("4. Password verified");

    user.setTwoFactorEnabled(false);
    user.setTwoFactorSecret(null);
    user.setTwoFactorSetupExpiresAt(null);
    user.setRecoveryCodeHashes(null);
    userRepository.save(user);
    log.info("5. 2FA disabled for user: {}", user.getEmailAddress());
  }

  public UserResponse map(User user) {
    log.debug("Mapping user to response: {}", user.getEmailAddress());
    return new UserResponse(
      user.getId(),
      user.getFullName(),
      user.getEmailAddress(),
      user.getUsername(),
      user.getWhatsAppNumber(),
      user.getRole(),
      user.getStatus(),
      user.isEmailVerified(),
      user.isTwoFactorEnabled(),
      user.getCreatedAt(),
      user.getLastLoginAt()
    );
  }

  private void queueVerification(User user) {
    log.debug(
      "Creating verification token for user: {}",
      user.getEmailAddress()
    );
    String raw = createToken(
      user,
      UserToken.TokenType.EMAIL_VERIFICATION,
      24 * 60
    );
    eventPublisher.publishEvent(
      new VerificationEmailRequested(
        user.getEmailAddress(),
        user.getFullName(),
        frontEndUrl + "/verification-complete?token=" + raw
      )
    );
  }

  private String createToken(User user, UserToken.TokenType type, int minutes) {
    log.debug(
      "Creating token for user: {}, type: {}, minutes: {}",
      user.getEmailAddress(),
      type,
      minutes
    );
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

    tokenRepository.deleteAllByUserIdAndType(user.getId(), type);
    log.debug(
      "Deleted existing tokens for user: {}, type: {}",
      user.getEmailAddress(),
      type
    );

    tokenRepository.save(
      UserToken.builder()
        .userId(user.getId())
        .tokenHash(hash(raw))
        .type(type)
        .expiresAt(LocalDateTime.now().plusMinutes(minutes))
        .build()
    );
    log.debug(
      "Token created successfully for user: {}",
      user.getEmailAddress()
    );
    return raw;
  }

  private UserToken findToken(String raw, UserToken.TokenType type) {
    log.debug("Finding token for type: {}", type);
    return tokenRepository
      .findByTokenHashAndTypeAndExpiresAtAfter(
        hash(raw),
        type,
        LocalDateTime.now()
      )
      .orElseThrow(() -> {
        log.warn("Token not found or expired for type: {}", type);
        return new InvalidInputException("This link is invalid or expired");
      });
  }

  private String hash(String value) {
    try {
      return HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(
          value.getBytes(StandardCharsets.UTF_8)
        )
      );
    } catch (Exception exception) {
      log.error("Error hashing value: {}", exception.getMessage(), exception);
      throw new IllegalStateException(exception);
    }
  }

  private int tokenVersion(User user) {
    return user.getTokenVersion() == null ? 0 : user.getTokenVersion();
  }
}
