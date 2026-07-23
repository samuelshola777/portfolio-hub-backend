package com.portfolio_hub.admin;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
  List<AuditLog> findTop100ByOrderByCreatedAtDesc();
  void deleteAllByActorIdOrTargetUserId(String actorId, String targetUserId);
  Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
  Page<AuditLog> findByActorIdOrTargetUserIdOrderByCreatedAtDesc(
    String actorId,
    String targetUserId,
    Pageable pageable
  );
}
