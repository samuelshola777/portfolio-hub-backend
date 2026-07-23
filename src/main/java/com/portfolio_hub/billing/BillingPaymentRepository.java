package com.portfolio_hub.billing;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingPaymentRepository
  extends JpaRepository<BillingPayment, String> {
  Optional<BillingPayment> findByReference(String reference);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
    "select payment from BillingPayment payment where payment.reference = :reference"
  )
  Optional<BillingPayment> findByReferenceForUpdate(
    @Param("reference") String reference
  );

  Page<BillingPayment> findByOwnerIdOrderByCreatedAtDesc(
    String ownerId,
    Pageable pageable
  );
  Page<BillingPayment> findByStatusOrderByCreatedAtAsc(
    PaymentStatus status,
    Pageable pageable
  );
  Page<BillingPayment> findAllByOrderByCreatedAtDesc(Pageable pageable);
  java.util.List<BillingPayment> findByStatusAndExpiresAtBefore(
    PaymentStatus status,
    LocalDateTime expiresAt
  );
}
