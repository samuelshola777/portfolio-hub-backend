package com.portfolio_hub.enquiry;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnquiryRepository extends JpaRepository<Enquiry, String> {
  List<Enquiry> findByOwnerIdOrderByCreatedAtDesc(String ownerId);
  Page<Enquiry> findByOwnerId(String ownerId, Pageable pageable);
  Optional<Enquiry> findByIdAndOwnerId(String id, String ownerId);
  long countByStatus(Enquiry.Status status);
  long countByOwnerId(String ownerId);
  void deleteAllByOwnerId(String ownerId);
}
