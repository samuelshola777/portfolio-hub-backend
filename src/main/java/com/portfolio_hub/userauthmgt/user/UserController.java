package com.portfolio_hub.userauthmgt.user;

import com.portfolio_hub.userauthmgt.user.request.*;
import com.portfolio_hub.userauthmgt.user.response.AuthResponse;
import com.portfolio_hub.userauthmgt.user.response.TwoFactorSetupResponse;
import com.portfolio_hub.userauthmgt.user.response.UserResponse;
import com.portfolio_hub.utils.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final GoogleAuthService googleAuthService;

  @PostMapping("/public/register")
  public ResponseEntity<ApiResponse<Map<String, Object>>> register(
          @Valid @RequestBody RegisterRequest request
  ) {
    return ResponseEntity.status(201).body(
            ApiResponse.success(
                    "Account created successfully",
                    userService.register(request)
            )
    );
  }

  @PostMapping("/public/login")
  public ResponseEntity<ApiResponse<AuthResponse>> login(
          @Valid @RequestBody LoginRequest request
  ) {
    return ResponseEntity.ok(
            ApiResponse.success("Login successful", userService.login(request))
    );
  }

  @PostMapping("/public/google")
  public ResponseEntity<ApiResponse<AuthResponse>> googleAuthentication(
          @Valid @RequestBody GoogleAuthRequest request
  ) {
    return ResponseEntity.ok(
            ApiResponse.success(
                    "Google authentication successful",
                    googleAuthService.authenticate(request)
            )
    );
  }

  @PostMapping("/public/refresh")
  public ResponseEntity<ApiResponse<AuthResponse>> refresh(
          @Valid @RequestBody RefreshTokenRequest request
  ) {
    return ResponseEntity.ok(
            ApiResponse.success(
                    "Session refreshed",
                    userService.refresh(request.refreshToken())
            )
    );
  }

  @GetMapping("/public/verify-email")
  public ResponseEntity<ApiResponse<Boolean>> verifyEmail(
          @RequestParam String token
  ) {
    return ResponseEntity.ok(
            ApiResponse.success(
                    "Email verified successfully",
                    userService.verifyEmail(token)
            )
    );
  }

  @PostMapping("/public/forgot-password")
  public ResponseEntity<ApiResponse<Void>> forgotPassword(
          @Valid @RequestBody ForgotPasswordRequest request
  ) {
    userService.forgotPassword(request.email());
    return ResponseEntity.ok(
            ApiResponse.success("If that account exists, a reset link has been sent")
    );
  }

  @PostMapping("/public/reset-password")
  public ResponseEntity<ApiResponse<Void>> resetPassword(
          @Valid @RequestBody ResetPasswordRequest request
  ) {
    userService.resetPassword(request.token(), request.password());
    return ResponseEntity.ok(
            ApiResponse.success("Password reset successfully")
    );
  }

  @GetMapping("/private/me")
  public ResponseEntity<ApiResponse<UserResponse>> me() {
    return ResponseEntity.ok(
            ApiResponse.success(
                    "Profile fetched successfully",
                    userService.currentProfile()
            )
    );
  }

  @PatchMapping("/private/me")
  public ResponseEntity<ApiResponse<UserResponse>> updateMe(
          @Valid @RequestBody CurrentUserUpdateRequest request
  ) {
    return ResponseEntity.ok(
            ApiResponse.success(
                    "Account information updated successfully",
                    userService.updateCurrentProfile(request)
            )
    );
  }

  @PostMapping("/private/change-password")
  public ResponseEntity<ApiResponse<Void>> changePassword(
          @Valid @RequestBody PasswordChangeRequest request
  ) {
    userService.changePassword(
            request.currentPassword(),
            request.newPassword()
    );
    return ResponseEntity.ok(
            ApiResponse.success("Password changed successfully")
    );
  }

  @PostMapping("/public/resend-verification")
  public ResponseEntity<ApiResponse<Boolean>> resendVerification(
          @RequestParam(name = "email") String email
  ) {
    return ResponseEntity.ok(
            ApiResponse.success(
                    "Verification link processed",
                    userService.resendVerification(email)
            )
    );
  }

  @PostMapping("/private/resend-verification")
  public ResponseEntity<ApiResponse<Boolean>> resendMyVerification() {
    return ResponseEntity.ok(
            ApiResponse.success(
                    "Verification link processed",
                    userService.resendVerificationForCurrentUser()
            )
    );
  }

  @PostMapping("/private/2fa/setup")
  public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> setup2fa() {
    return ResponseEntity.ok(
            ApiResponse.success(
                    "Two-factor setup created",
                    userService.beginTwoFactorSetup()
            )
    );
  }

  @PostMapping("/private/2fa/confirm")
  public ResponseEntity<ApiResponse<Map<String, List<String>>>> confirm2fa(
          @Valid @RequestBody TwoFactorCodeRequest request
  ) {
    return ResponseEntity.ok(
            ApiResponse.success(
                    "Two-factor authentication enabled",
                    Map.of("recoveryCodes", userService.confirmTwoFactor(request.code()))
            )
    );
  }

  @PostMapping("/private/2fa/disable")
  public ResponseEntity<ApiResponse<Void>> disable2fa(
          @Valid @RequestBody DisableTwoFactorRequest request
  ) {
    userService.disableTwoFactor(request.password());
    return ResponseEntity.ok(
            ApiResponse.success("Two-factor authentication disabled")
    );
  }
}