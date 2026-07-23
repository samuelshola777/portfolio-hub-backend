package com.portfolio_hub.profile;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, String> {
  List<Skill> findByOwnerIdOrderByCategoryAscSortOrderAscNameAsc(
    String ownerId
  );
  Optional<Skill> findByIdAndOwnerId(String id, String ownerId);
  boolean existsByOwnerIdAndNameIgnoreCase(String ownerId, String name);
  boolean existsByOwnerIdAndNameIgnoreCaseAndIdNot(
    String ownerId,
    String name,
    String id
  );
  long countByOwnerId(String ownerId);
  Page<Skill> findByOwnerId(String ownerId, Pageable pageable);
  void deleteAllByOwnerId(String ownerId);
}
