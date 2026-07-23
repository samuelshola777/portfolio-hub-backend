package com.portfolio_hub.business;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessItemRepository
  extends JpaRepository<BusinessItem, String> {
  Page<BusinessItem> findByBusinessIdAndTypeAndDeletedFalse(
    String businessId,
    BusinessItem.Type type,
    Pageable pageable
  );
  Page<BusinessItem> findByBusinessIdAndTypeAndStatusAndDeletedFalse(
    String businessId,
    BusinessItem.Type type,
    BusinessItem.Status status,
    Pageable pageable
  );
  Optional<BusinessItem> findByIdAndBusinessIdAndDeletedFalse(
    String id,
    String businessId
  );
  long countByBusinessIdAndTypeAndDeletedFalse(
    String businessId,
    BusinessItem.Type type
  );

  long countByBusinessIdAndDeletedFalse(String businessId);

  @Query(
    "select i from BusinessItem i where i.deleted = false and i.businessId in (select b.id from Business b where b.ownerId = :ownerId)"
  )
  Page<BusinessItem> findAllForOwner(
    @Param("ownerId") String ownerId,
    Pageable pageable
  );

  @Query(
    "select count(i) from BusinessItem i where i.deleted = false and i.businessId in (select b.id from Business b where b.ownerId = :ownerId)"
  )
  long countAllForOwner(@Param("ownerId") String ownerId);

  void deleteAllByBusinessIdIn(List<String> businessIds);
}
