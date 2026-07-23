package com.portfolio_hub.business;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessEnquiryRepository
  extends JpaRepository<BusinessEnquiry, String> {
  Page<BusinessEnquiry> findByBusinessId(String businessId, Pageable pageable);
  Optional<BusinessEnquiry> findByIdAndBusinessId(String id, String businessId);

  @Query(
    "select e from BusinessEnquiry e where e.businessId in (select b.id from Business b where b.ownerId = :ownerId)"
  )
  Page<BusinessEnquiry> findAllForOwner(
    @Param("ownerId") String ownerId,
    Pageable pageable
  );

  @Query(
    "select count(e) from BusinessEnquiry e where e.businessId in (select b.id from Business b where b.ownerId = :ownerId)"
  )
  long countAllForOwner(@Param("ownerId") String ownerId);

  void deleteAllByBusinessIdIn(List<String> businessIds);
}
