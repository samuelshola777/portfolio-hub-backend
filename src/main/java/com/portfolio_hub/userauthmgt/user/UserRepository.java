package com.portfolio_hub.userauthmgt.user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository
        extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {
  Optional<User> findByEmailAddressIgnoreCaseAndDeletedFalse(
          String emailAddress
  );
  Optional<User> findByUsernameIgnoreCaseAndDeletedFalse(String username);
  Optional<User> findByGoogleSubjectAndDeletedFalse(String googleSubject);
  boolean existsByEmailAddressIgnoreCase(String emailAddress);
  boolean existsByUsernameIgnoreCase(String username);
  boolean existsByEmailAddressIgnoreCaseAndIdNot(
          String emailAddress,
          String id
  );
  boolean existsByUsernameIgnoreCaseAndIdNot(String username, String id);

  Page<User> findAllByDeletedFalse(Pageable pageable);

  @Query(
          """
          select u from User u where u.deleted = false and
          (lower(u.fullName) like lower(concat('%', :search, '%')) or
                   lower(u.emailAddress) like lower(concat('%', :search, '%')) or
                   lower(u.username) like lower(concat('%', :search, '%')) or
                   lower(u.whatsAppNumber) like lower(concat('%', :search, '%')))
          """
  )
  Page<User> search(@Param("search") String search, Pageable pageable);

  @Query(
          """
          select u from User u where u.deleted = false
            and (:includeSuperAdmins = true or u.role <> :superAdminRole)
            and (:search is null or lower(u.fullName) like lower(concat('%', :search, '%'))
                         or lower(u.emailAddress) like lower(concat('%', :search, '%'))
                         or lower(u.username) like lower(concat('%', :search, '%'))
                         or lower(u.whatsAppNumber) like lower(concat('%', :search, '%')))
            and (:status is null or u.status = :status)
            and (:verified is null or u.emailVerified = :verified)
            and (:role is null or u.role = :role)
          """
  )
  Page<User> adminSearch(
          @Param("search") String search,
          @Param("status") User.AccountStatus status,
          @Param("verified") Boolean verified,
          @Param("role") User.UserRole role,
          @Param("includeSuperAdmins") boolean includeSuperAdmins,
          @Param("superAdminRole") User.UserRole superAdminRole,
          Pageable pageable
  );

  @Query(
          """
          select u from User u where u.deleted = false and u.role <> :superAdminRole
            and (:search is null or lower(u.fullName) like lower(concat('%', :search, '%'))
                         or lower(u.emailAddress) like lower(concat('%', :search, '%'))
                         or lower(u.username) like lower(concat('%', :search, '%'))
                         or lower(u.whatsAppNumber) like lower(concat('%', :search, '%')))
            and (:status is null or u.status = :status)
            and (:verified is null or u.emailVerified = :verified)
            and (:role is null or u.role = :role)
          order by u.createdAt desc
          """
  )
  List<User> announcementRecipients(
          @Param("search") String search,
          @Param("status") User.AccountStatus status,
          @Param("verified") Boolean verified,
          @Param("role") User.UserRole role,
          @Param("superAdminRole") User.UserRole superAdminRole
  );

  long countByDeletedFalse();
  long countByDeletedFalseAndStatus(User.AccountStatus status);
  long countByDeletedFalseAndEmailVerifiedTrue();

  interface DailyUserCount {
    LocalDate getDay();
    long getUsers();
  }

  @Query(
          value = """
    select cast(created_at as date) as day, count(*) as users
    from users
    where deleted = false and created_at >= :after
    group by cast(created_at as date)
    order by day
    """,
          nativeQuery = true
  )
  List<DailyUserCount> dailyRegistrations(@Param("after") LocalDateTime after);
}