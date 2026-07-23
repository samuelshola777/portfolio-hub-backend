package com.portfolio_hub.userauthmgt.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsernameAliasRepository
  extends JpaRepository<UsernameAlias, String> {
  Optional<UsernameAlias> findByUsernameIgnoreCase(String username);
  boolean existsByUsernameIgnoreCase(String username);
  void deleteAllByOwnerId(String ownerId);
}
