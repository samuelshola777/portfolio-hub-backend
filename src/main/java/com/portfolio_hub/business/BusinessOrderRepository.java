package com.portfolio_hub.business;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessOrderRepository
  extends JpaRepository<BusinessOrder, String> {
  Page<BusinessOrder> findByBusinessId(String businessId, Pageable pageable);
  Optional<BusinessOrder> findByIdAndBusinessId(String id, String businessId);

  @Query(
    "select o from BusinessOrder o where o.businessId in (select b.id from Business b where b.ownerId = :ownerId)"
  )
  Page<BusinessOrder> findAllForOwner(
    @Param("ownerId") String ownerId,
    Pageable pageable
  );

  @Query(
    "select count(o) from BusinessOrder o where o.businessId in (select b.id from Business b where b.ownerId = :ownerId)"
  )
  long countAllForOwner(@Param("ownerId") String ownerId);

  void deleteAllByBusinessIdIn(List<String> businessIds);
}
