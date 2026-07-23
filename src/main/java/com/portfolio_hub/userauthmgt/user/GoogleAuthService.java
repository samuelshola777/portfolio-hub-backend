package com.portfolio_hub.userauthmgt.user;

import com.portfolio_hub.admin.AuditLog;
import com.portfolio_hub.admin.AuditLogRepository;
import com.portfolio_hub.userauthmgt.user.request.GoogleAuthRequest;
import com.portfolio_hub.userauthmgt.user.response.AuthResponse;
import com.portfolio_hub.utils.appsecurity.JwtService;
import com.portfolio_hub.utils.exception.UnauthorizedException;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthService {

    private final GoogleIdentityService googleIdentityService;
    private final UserRepository userRepository;
    private final UsernameAliasRepository usernameAliasRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;
    private final AuditLogRepository auditLogRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthResponse authenticate(GoogleAuthRequest request) {
        GoogleIdentityService.GoogleProfile profile =
                googleIdentityService.verify(request.credential());

        User user = userRepository
                .findByGoogleSubjectAndDeletedFalse(profile.subject())
                .orElseGet(() -> findOrCreateByEmail(profile));

        if (user.getStatus() == User.AccountStatus.BLOCKED) {
            throw new UnauthorizedException("This account has been blocked");
        }

        if (user.isTwoFactorEnabled()) {
            throw new UnauthorizedException(
                    "Use email and password to complete two-factor authentication"
            );
        }

        if (
                user.getGoogleSubject() != null &&
                        !user.getGoogleSubject().equals(profile.subject())
        ) {
            throw new UnauthorizedException(
                    "This email is already connected to another Google account"
            );
        }

        user.setGoogleSubject(profile.subject());
        user.setEmailVerified(true);
        user.setLastLoginAt(LocalDateTime.now());
        user = userRepository.save(user);

        auditLogRepository.save(
                AuditLog.builder()
                        .actorId(user.getId())
                        .targetUserId(user.getId())
                        .action("USER_GOOGLE_AUTHENTICATED")
                        .description(user.getFullName() + " signed in with Google")
                        .build()
        );

        return createAuthResponse(user);
    }

    private User findOrCreateByEmail(
            GoogleIdentityService.GoogleProfile profile
    ) {
        return userRepository
                .findByEmailAddressIgnoreCaseAndDeletedFalse(profile.email())
                .map(existingUser -> linkExistingUser(existingUser, profile))
                .orElseGet(() -> createGoogleUser(profile));
    }

    private User linkExistingUser(
            User user,
            GoogleIdentityService.GoogleProfile profile
    ) {
        if (
                user.getGoogleSubject() != null &&
                        !user.getGoogleSubject().equals(profile.subject())
        ) {
            throw new UnauthorizedException(
                    "This email is already connected to another Google account"
            );
        }

        user.setGoogleSubject(profile.subject());
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private User createGoogleUser(
            GoogleIdentityService.GoogleProfile profile
    ) {
        User user = User.builder()
                .fullName(profile.fullName())
                .emailAddress(profile.email())
                .username(generateUniqueUsername(profile.email()))
                .whatsAppNumber(null)
                .password(passwordEncoder.encode(generateRandomPassword()))
                .googleSubject(profile.subject())
                .role(User.UserRole.BUSINESS_OWNER)
                .status(User.AccountStatus.ACTIVE)
                .emailVerified(true)
                .twoFactorEnabled(false)
                .tokenVersion(0)
                .deleted(false)
                .build();

        user = userRepository.save(user);

        auditLogRepository.save(
                AuditLog.builder()
                        .actorId(user.getId())
                        .targetUserId(user.getId())
                        .action("USER_REGISTERED_WITH_GOOGLE")
                        .description(user.getFullName() + " created an account with Google")
                        .build()
        );

        log.info("Created Google business account for user ID: {}", user.getId());
        return user;
    }

    private AuthResponse createAuthResponse(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("tokenVersion", tokenVersion(user));

        return new AuthResponse(
                jwtService.generateToken(user.getEmailAddress(), claims),
                jwtService.generateRefreshToken(user.getEmailAddress(), claims),
                userService.map(user)
        );
    }

    private String generateUniqueUsername(String email) {
        String localPart = email.substring(0, email.indexOf('@'));
        String base = Normalizer.normalize(localPart, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (base.length() < 5) {
            base = (base + "-user").replaceAll("^-+|-+$", "");
        }

        if (base.length() > 36) {
            base = base.substring(0, 36).replaceAll("-+$", "");
        }

        String candidate = base;
        int suffix = 1;

        while (usernameExists(candidate)) {
            candidate = base + "-" + suffix++;
        }

        return candidate;
    }

    private boolean usernameExists(String username) {
        return (
                userRepository.existsByUsernameIgnoreCase(username) ||
                        usernameAliasRepository.existsByUsernameIgnoreCase(username)
        );
    }

    private String generateRandomPassword() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private int tokenVersion(User user) {
        return user.getTokenVersion() == null ? 0 : user.getTokenVersion();
    }
}
