package com.portfolio_hub.userauthmgt.user;

import org.springframework.data.jpa.domain.Specification;

public final class AdminUserSpecifications {

  private AdminUserSpecifications() {}

  public static Specification<User> matching(
    String search,
    User.AccountStatus status,
    Boolean verified,
    User.UserRole role,
    boolean includeSuperAdmins
  ) {
    return (root, query, builder) -> {
      var predicate = builder.isFalse(root.get("deleted"));

      if (!includeSuperAdmins) {
        predicate = builder.and(
          predicate,
          builder.notEqual(root.get("role"), User.UserRole.SUPER_ADMIN)
        );
      }
      if (search != null && !search.isBlank()) {
        String pattern = "%" + search.trim().toLowerCase() + "%";
        predicate = builder.and(
          predicate,
          builder.or(
            builder.like(builder.lower(root.get("fullName")), pattern),
            builder.like(builder.lower(root.get("emailAddress")), pattern),
            builder.like(builder.lower(root.get("username")), pattern),
            builder.like(builder.lower(root.get("whatsAppNumber")), pattern)
          )
        );
      }
      if (status != null) {
        predicate = builder.and(
          predicate,
          builder.equal(root.get("status"), status)
        );
      }
      if (verified != null) {
        predicate = builder.and(
          predicate,
          builder.equal(root.get("emailVerified"), verified)
        );
      }
      if (role != null) {
        predicate = builder.and(
          predicate,
          builder.equal(root.get("role"), role)
        );
      }
      return predicate;
    };
  }
}
