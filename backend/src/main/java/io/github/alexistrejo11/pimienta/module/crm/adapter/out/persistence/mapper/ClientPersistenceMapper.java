package io.github.alexistrejo11.pimienta.module.crm.adapter.out.persistence.mapper;

import io.github.alexistrejo11.pimienta.module.crm.adapter.out.persistence.model.ClientJpaEntity;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.Client;

public final class ClientPersistenceMapper {

  private ClientPersistenceMapper() {}

  public static Client toDomain(ClientJpaEntity e) {
    return Client.builder()
        .withId(e.getId())
        .withName(e.getName())
        .withCompanyName(e.getCompanyName())
        .withCreatedAt(e.getCreatedAt())
        .withUpdatedAt(e.getUpdatedAt())
        .withDeletedAt(e.getDeletedAt())
        .withVersion(e.getVersion())
        .reconstruct();
  }

  public static ClientJpaEntity toJpa(Client domain) {
    ClientJpaEntity e = new ClientJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      e.setId(domain.getId());
    }
    e.setName(domain.getName());
    e.setCompanyName(blankToNull(domain.getCompanyName()));
    e.setCreatedAt(domain.getCreatedAt());
    e.setUpdatedAt(domain.getUpdatedAt());
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion() != null ? domain.getVersion() : 0L);
    return e;
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
