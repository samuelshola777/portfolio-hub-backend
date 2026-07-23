package com.portfolio_hub.profile;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileEntryRepository
  extends JpaRepository<ProfileEntry, String> {
  List<ProfileEntry> findByOwnerIdOrderByTypeAscSortOrderAscStartDateDesc(
    String ownerId
  );
  Optional<ProfileEntry> findByIdAndOwnerId(String id, String ownerId);
  long countByOwnerIdAndType(String ownerId, ProfileEntry.EntryType type);
  long countByOwnerId(String ownerId);
  Page<ProfileEntry> findByOwnerId(String ownerId, Pageable pageable);
  void deleteAllByOwnerId(String ownerId);
}
