package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.mapper;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryTransaction;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.InventoryTransactionJpaEntity;

import org.springframework.stereotype.Component;

@Component
public class TransactionPersistenceMapper {

  public InventoryTransactionJpaEntity toJpa(InventoryTransaction domain) {
    InventoryTransactionJpaEntity e = new InventoryTransactionJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      e.setId(domain.getId());
    }
    e.setTransactionNumber(domain.getTransactionNumber());
    e.setType(domain.getType());
    e.setStatus(domain.getStatus());
    e.setExternalReference(domain.getExternalReference());
    e.setNotes(domain.getNotes());
    e.setExitReason(domain.getExitReason());
    e.setInitiatedById(domain.getInitiatedById());
    e.setApprovedById(domain.getApprovedById());
    e.setApprovedAt(domain.getApprovedAt());
    e.setCompletedAt(domain.getCompletedAt());
    e.setCreatedAt(domain.getCreatedAt());
    e.setUpdatedAt(domain.getUpdatedAt());
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion() != null ? domain.getVersion() : 0L);
    return e;
  }

  public InventoryTransaction toDomain(InventoryTransactionJpaEntity e) {
    InventoryTransaction tx = new InventoryTransaction();
    tx.setId(e.getId());
    tx.setTransactionNumber(e.getTransactionNumber());
    tx.setType(e.getType());
    tx.setStatus(e.getStatus());
    tx.setExternalReference(e.getExternalReference());
    tx.setNotes(e.getNotes());
    tx.setExitReason(e.getExitReason());
    tx.setInitiatedById(e.getInitiatedById());
    tx.setApprovedById(e.getApprovedById());
    tx.setApprovedAt(e.getApprovedAt());
    tx.setCompletedAt(e.getCompletedAt());
    tx.setCreatedAt(e.getCreatedAt());
    tx.setUpdatedAt(e.getUpdatedAt());
    tx.setDeletedAt(e.getDeletedAt());
    tx.setVersion(e.getVersion());
    return tx;
  }
}
