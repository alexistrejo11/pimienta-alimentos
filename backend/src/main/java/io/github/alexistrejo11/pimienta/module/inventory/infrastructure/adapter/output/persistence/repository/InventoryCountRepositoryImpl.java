package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.InventoryCountSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryCountResponse;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryCountSession;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.InventoryCountRepository;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.InventoryCountResponseJpaEntity;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.InventoryCountSessionJpaEntity;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.repository.jpa.InventoryCountResponseJpaRepository;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.repository.jpa.InventoryCountSessionJpaRepository;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.specification.InventoryCountSpecifications;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryCountRepositoryImpl implements InventoryCountRepository {

  private final InventoryCountSessionJpaRepository sessions;
  private final InventoryCountResponseJpaRepository responses;

  public InventoryCountRepositoryImpl(
      InventoryCountSessionJpaRepository sessions, InventoryCountResponseJpaRepository responses) {
    this.sessions = sessions;
    this.responses = responses;
  }

  @Override
  public boolean existsActiveByLocationId(long locationId) {
    return sessions.existsByLocationIdAndStatusIn(
        locationId,
        List.of(InventoryCountSession.Status.DRAFT, InventoryCountSession.Status.SUBMITTED));
  }

  @Override
  public long countOpen(List<Long> headquarterIds) {
    var open = List.of(InventoryCountSession.Status.DRAFT, InventoryCountSession.Status.SUBMITTED);
    if (headquarterIds != null && headquarterIds.isEmpty()) {
      return 0;
    }
    if (headquarterIds == null) {
      return sessions.countByStatusIn(open);
    }
    return sessions.countOpenInHeadquarters(headquarterIds, open);
  }

  @Override
  public Optional<InventoryCountSession> findById(long id) {
    return sessions
        .findById(id)
        .map(
            entity -> {
              InventoryCountSession session = toDomain(entity);
              session.replaceResponsesForLoad(
                  responses.findBySessionIdOrderByIdAsc(id).stream()
                      .map(this::toDomain)
                      .toList());
              return session;
            });
  }

  @Override
  public Page<InventoryCountSession> search(
      InventoryCountSearchCriteria criteria, Pageable pageable) {
    return sessions
        .findAll(InventoryCountSpecifications.fromCriteria(criteria), pageable)
        .map(this::toDomain);
  }

  @Override
  public InventoryCountSession save(InventoryCountSession session) {
    InventoryCountSessionJpaEntity entity = new InventoryCountSessionJpaEntity();
    entity.setId(session.getId());
    entity.setLocationId(session.getLocationId());
    entity.setCountType(session.getCountType());
    entity.setStatus(session.getStatus());
    entity.setCreatedById(session.getCreatedById());
    entity.setSubmittedById(session.getSubmittedById());
    entity.setApprovedById(session.getApprovedById());
    entity.setCreatedAt(session.getCreatedAt());
    entity.setSubmittedAt(session.getSubmittedAt());
    entity.setApprovedAt(session.getApprovedAt());
    entity.setCancelledAt(session.getCancelledAt());
    entity = sessions.save(entity);
    session.setId(entity.getId());

    for (InventoryCountResponse response : session.getResponses()) {
      InventoryCountResponseJpaEntity row = new InventoryCountResponseJpaEntity();
      row.setId(response.getId());
      row.setSessionId(entity.getId());
      row.setItemId(response.getItemId());
      row.setExpectedQuantity(response.getExpectedQuantity());
      row.setCountedQuantity(response.getCountedQuantity());
      row.setVariance(response.getVariance());
      row.setCountedAt(response.getCountedAt());
      row = responses.save(row);
      response.setId(row.getId());
    }
    return session;
  }

  private InventoryCountSession toDomain(InventoryCountSessionJpaEntity entity) {
    InventoryCountSession session = new InventoryCountSession();
    session.setId(entity.getId());
    session.setLocationId(entity.getLocationId());
    session.setCountType(entity.getCountType());
    session.setStatus(entity.getStatus());
    session.setCreatedById(entity.getCreatedById());
    session.setSubmittedById(entity.getSubmittedById());
    session.setApprovedById(entity.getApprovedById());
    session.setCreatedAt(entity.getCreatedAt());
    session.setSubmittedAt(entity.getSubmittedAt());
    session.setApprovedAt(entity.getApprovedAt());
    session.setCancelledAt(entity.getCancelledAt());
    return session;
  }

  private InventoryCountResponse toDomain(InventoryCountResponseJpaEntity entity) {
    InventoryCountResponse response = new InventoryCountResponse();
    response.setId(entity.getId());
    response.setItemId(entity.getItemId());
    response.setExpectedQuantity(entity.getExpectedQuantity());
    response.setCountedQuantity(entity.getCountedQuantity());
    response.setVariance(entity.getVariance());
    response.setCountedAt(entity.getCountedAt());
    return response;
  }
}
