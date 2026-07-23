package com.portfolio_hub.userauthmgt.user;

import com.portfolio_hub.utils.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(
                        name = "idx_users_email",
                        columnList = "emailAddress",
                        unique = true
                ),
                @Index(name = "idx_users_username", columnList = "username", unique = true),
                @Index(
                        name = "idx_users_google_subject",
                        columnList = "googleSubject",
                        unique = true
                ),
                @Index(name = "idx_users_status_role", columnList = "status,role"),
        }
)
public class User extends BaseEntity {


  public enum UserRole {
    USER,
    PROFESSIONAL,
    BUSINESS_OWNER,
    SUPER_ADMIN,
  }

  public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    BLOCKED,
  }

  @Column(nullable = false, columnDefinition = "TEXT")
  private String fullName;

  @Column(nullable = false, unique = true, columnDefinition = "TEXT")
  private String emailAddress;

  @Column(nullable = false, unique = true, columnDefinition = "TEXT")
  private String username;

  @Column(columnDefinition = "TEXT")
  private String whatsAppNumber;

  @Column(nullable = false)
  private String password;

  @Column(unique = true, columnDefinition = "TEXT")
  private String googleSubject;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private UserRole role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private AccountStatus status;

  private boolean emailVerified;
  private boolean twoFactorEnabled;

  @Column(columnDefinition = "TEXT")
  private String twoFactorSecret;

  private LocalDateTime twoFactorSetupExpiresAt;

  @Column(columnDefinition = "TEXT")
  private String recoveryCodeHashes;

  private LocalDateTime lastLoginAt;
  private Integer tokenVersion;
  private boolean deleted;
}

