package io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.module.account.user.core.domain.entities.User;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.entities.UserReconstructParams;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Role;

import java.util.ArrayList;
import java.util.LinkedHashSet;

final class UserPersistenceMapper {

  private UserPersistenceMapper() {
  }

  static UserJpaEntity toJpa(User domain) {
    var e = new UserJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      e.setId(domain.getId());
    }
    e.setEmail(domain.getEmail());
    e.setPasswordHash(domain.getPasswordHash());
    e.setFirstName(domain.getFirstName());
    e.setLastName(domain.getLastName());
    e.setGender(domain.getGender());
    e.setPhone(domain.getPhone());
    e.setDateOfBirth(domain.getDateOfBirth());
    e.setAccountStatus(domain.getAccountStatus());
    e.setBannedReason(domain.getBannedReason());
    e.setBannedAt(domain.getBannedAt());
    e.setRoles(new LinkedHashSet<>(domain.getRoles()));
    e.setAssignedHeadquarterIds(new LinkedHashSet<>(domain.getAssignedHeadquarterIds()));
    e.setCreatedAt(domain.getCreatedAt());
    e.setUpdatedAt(domain.getUpdatedAt());
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion() != null ? domain.getVersion() : 0L);
    return e;
  }

  static User toDomain(UserJpaEntity e) {
    var roles = new ArrayList<Role>();
    if (e.getRoles() != null) {
      roles.addAll(e.getRoles());
    }
    var hqIds = new ArrayList<Long>();
    if (e.getAssignedHeadquarterIds() != null) {
      hqIds.addAll(e.getAssignedHeadquarterIds());
    }
    UserReconstructParams params =
        UserReconstructParams.builder()
        .id(e.getId())
        .email(e.getEmail())
        .passwordHash(e.getPasswordHash())
        .firstName(e.getFirstName())
        .lastName(e.getLastName())
        .gender(e.getGender())
        .phone(e.getPhone())
        .dateOfBirth(e.getDateOfBirth())
        .accountStatus(e.getAccountStatus())
        .bannedReason(e.getBannedReason())
        .bannedAt(e.getBannedAt())
        .roles(roles)
        .assignedHeadquarterIds(hqIds)
        .createdAt(e.getCreatedAt())
        .updatedAt(e.getUpdatedAt())
        .deletedAt(e.getDeletedAt())
        .version(e.getVersion())
        .build();
    return User.reconstruct(params);
  }
}
