package com.portfolio_hub.announcement;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRecipientRepository
  extends JpaRepository<AnnouncementRecipient, String> {
  Page<AnnouncementRecipient> findByUserIdOrderByCreatedAtDesc(
    String userId,
    Pageable pageable
  );
  Optional<AnnouncementRecipient> findByIdAndUserId(String id, String userId);
  void deleteAllByUserId(String userId);
}
