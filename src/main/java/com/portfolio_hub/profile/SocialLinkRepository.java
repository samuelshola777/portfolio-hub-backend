package com.portfolio_hub.profile;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialLinkRepository
  extends JpaRepository<SocialLink, String> {
  List<SocialLink> findByOwnerIdOrderBySortOrderAscPlatformAsc(String ownerId);
  Optional<SocialLink> findByIdAndOwnerId(String id, String ownerId);
  boolean existsByOwnerIdAndPlatformIgnoreCase(String ownerId, String platform);
  boolean existsByOwnerIdAndPlatformIgnoreCaseAndIdNot(
    String ownerId,
    String platform,
    String id
  );
  long countByOwnerId(String ownerId);
  Page<SocialLink> findByOwnerId(String ownerId, Pageable pageable);
  void deleteAllByOwnerId(String ownerId);
}
