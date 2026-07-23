package com.portfolio_hub.utils.fileupload;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ManagedFileRepository
  extends JpaRepository<ManagedFile, String> {
  Optional<ManagedFile> findByFileUrl(String fileUrl);

  @Query("select coalesce(sum(f.fileSizeBytes), 0) from ManagedFile f")
  long totalStorageBytes();

  long countByOwnerId(String ownerId);
  Page<ManagedFile> findByOwnerId(String ownerId, Pageable pageable);
  List<ManagedFile> findAllByOwnerId(String ownerId);
  List<ManagedFile> findAllByOwnerIdOrderByCreatedAtDesc(String ownerId);
  void deleteAllByOwnerId(String ownerId);
}
