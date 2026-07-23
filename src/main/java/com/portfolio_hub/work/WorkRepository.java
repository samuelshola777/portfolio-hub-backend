package com.portfolio_hub.work;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkRepository extends JpaRepository<Work, String> {
  List<Work> findByOwnerIdAndDeletedFalseOrderBySortOrderAscCreatedAtDesc(
    String ownerId
  );
  Page<Work> findByOwnerIdAndDeletedFalse(String ownerId, Pageable pageable);
  List<
    Work
  > findByOwnerIdAndDeletedFalseAndStatusOrderByFeaturedDescSortOrderAscCreatedAtDesc(
    String ownerId,
    Work.PublicationStatus status
  );
  Optional<Work> findByIdAndOwnerIdAndDeletedFalse(String id, String ownerId);
  Optional<Work> findByOwnerIdAndSlugIgnoreCase(String ownerId, String slug);
  boolean existsByOwnerIdAndSlugIgnoreCaseAndDeletedFalse(
    String ownerId,
    String slug
  );
  long countByOwnerIdAndDeletedFalse(String ownerId);
  void deleteAllByOwnerId(String ownerId);
}
