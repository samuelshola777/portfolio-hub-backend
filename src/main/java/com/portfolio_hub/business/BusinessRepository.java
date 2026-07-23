package com.portfolio_hub.business;

import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, String> {
  List<Business> findByOwnerIdOrderByCreatedAtAsc(String ownerId);
  Optional<Business> findByIdAndOwnerId(String id, String ownerId);
  Optional<Business> findBySlugIgnoreCaseAndStatus(
    String slug,
    Business.Status status
  );
  boolean existsBySlugIgnoreCase(String slug);
  long countByOwnerId(String ownerId);
  Page<Business> findByOwnerId(String ownerId, Pageable pageable);
  void deleteAllByOwnerId(String ownerId);
}
