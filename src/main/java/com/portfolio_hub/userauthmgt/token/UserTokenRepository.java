package com.portfolio_hub.userauthmgt.token;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTokenRepository extends JpaRepository<UserToken, String> {
  Optional<UserToken> findByTokenHashAndTypeAndExpiresAtAfter(
    String hash,
    UserToken.TokenType type,
    LocalDateTime now
  );
  void deleteAllByUserIdAndType(String userId, UserToken.TokenType type);
  void deleteAllByUserId(String userId);
  Optional<UserToken> findTopByUserIdAndTypeOrderByCreatedAtDesc(
    String userId,
    UserToken.TokenType type
  );
}
