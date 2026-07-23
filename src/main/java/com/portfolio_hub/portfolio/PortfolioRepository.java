package com.portfolio_hub.portfolio;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioRepository extends JpaRepository<Portfolio, String> {
  Optional<Portfolio> findByOwnerId(String ownerId);
  Optional<Portfolio> findByUsernameIgnoreCaseAndStatus(
    String username,
    Portfolio.PublicationStatus status
  );
  long countByStatus(Portfolio.PublicationStatus status);
  void deleteAllByOwnerId(String ownerId);
}
