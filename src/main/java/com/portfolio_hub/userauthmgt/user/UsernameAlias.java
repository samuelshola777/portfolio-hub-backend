package com.portfolio_hub.userauthmgt.user;

import com.portfolio_hub.utils.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
  name = "username_aliases",
  indexes = {
    @Index(name = "idx_username_alias", columnList = "username", unique = true),
    @Index(name = "idx_username_alias_owner", columnList = "ownerId"),
  }
)
public class UsernameAlias extends BaseEntity {

  @Column(nullable = false, unique = true, columnDefinition = "TEXT")
  private String username;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String ownerId;
}
