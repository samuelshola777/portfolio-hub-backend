package com.portfolio_hub.feedback;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, String> {
  Page<Feedback> findByOwnerIdOrderByCreatedAtDesc(
    String ownerId,
    Pageable pageable
  );
  Page<Feedback> findAllByOrderByCreatedAtDesc(Pageable pageable);
  Page<Feedback> findByStatusOrderByCreatedAtDesc(
    Feedback.Status status,
    Pageable pageable
  );
  Optional<Feedback> findByIdAndOwnerId(String id, String ownerId);
  void deleteAllByOwnerId(String ownerId);
}
