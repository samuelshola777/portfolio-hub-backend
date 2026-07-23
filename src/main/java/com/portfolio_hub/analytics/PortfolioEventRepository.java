package com.portfolio_hub.analytics;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioEventRepository
  extends JpaRepository<PortfolioEvent, String> {
  List<PortfolioEvent> findByOwnerIdAndCreatedAtAfterOrderByCreatedAtAsc(
    String ownerId,
    LocalDateTime after
  );
  long countByEventType(PortfolioEvent.EventType type);
  List<PortfolioEvent> findTop100ByOrderByCreatedAtDesc();
  long countByOwnerId(String ownerId);
  Page<PortfolioEvent> findByOwnerId(String ownerId, Pageable pageable);
  void deleteAllByOwnerId(String ownerId);
}
