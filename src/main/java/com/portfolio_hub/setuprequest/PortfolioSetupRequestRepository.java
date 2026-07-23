package com.portfolio_hub.setuprequest;

import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioSetupRequestRepository
  extends JpaRepository<PortfolioSetupRequest, String> {
  Page<PortfolioSetupRequest> findByStatusOrderByCreatedAtDesc(
    PortfolioSetupRequest.Status status,
    Pageable pageable
  );
  Page<PortfolioSetupRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

  Optional<
    PortfolioSetupRequest
  > findFirstByWorkspaceIdAndStatusInOrderByCreatedAtDesc(
    String workspaceId,
    Collection<PortfolioSetupRequest.Status> statuses
  );
}
