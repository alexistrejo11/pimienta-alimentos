package io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum Role {
  /** Registered account with no elevated permissions. */
  USER(EnumSet.noneOf(Permission.class)),
  SUPPORT(EnumSet.of(Permission.USERS_READ)),
  MANAGER(
      EnumSet.of(
          Permission.PROFILE_READ,
          Permission.PROFILE_WRITE,
          Permission.USERS_READ,
           Permission.USERS_WRITE)),
  SALES(EnumSet.noneOf(Permission.class)),
  POS_OPERATOR(EnumSet.noneOf(Permission.class)),
  EMPLOYEE(EnumSet.noneOf(Permission.class)),
  ADMIN(EnumSet.allOf(Permission.class));

  private final EnumSet<Permission> permissions;

  Role(EnumSet<Permission> permissions) {
    this.permissions = permissions.clone();
  }

  public Set<Permission> getPermissions() {
    return Collections.unmodifiableSet(permissions);
  }
}
