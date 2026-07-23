package com.portfolio_hub.userauthmgt.token;

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
  name = "user_tokens",
  indexes = {
    @Index(
      name = "idx_user_token_hash",
      columnList = "tokenHash",
      unique = true
    ),
    @Index(name = "idx_user_token_user_type", columnList = "userId,type"),
  }
)
public class UserToken extends BaseEntity {

  public enum TokenType {
    EMAIL_VERIFICATION,
    PASSWORD_RESET,
  }

  @Column(nullable = false, columnDefinition = "TEXT")
  private String userId;

  @Column(nullable = false, unique = true, columnDefinition = "TEXT")
  private String tokenHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "TEXT")
  private TokenType type;

  @Column(nullable = false)
  private LocalDateTime expiresAt;
}
