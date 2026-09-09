package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosSyncIncidentJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PosSyncIncidentSpringDataRepository
    extends JpaRepository<PosSyncIncidentJpaEntity, UUID> {

  Optional<PosSyncIncidentJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

  @Query(
      """
      SELECT i FROM PosSyncIncidentJpaEntity i
      WHERE i.deletedAt IS NULL
        AND (:headquarterId IS NULL OR i.headquarterId = :headquarterId)
        AND (
          :openOnly IS NULL
          OR (:openOnly = TRUE AND i.acceptedAt IS NULL)
          OR (:openOnly = FALSE AND i.acceptedAt IS NOT NULL)
        )
      ORDER BY i.createdAt DESC
      """)
  Page<PosSyncIncidentJpaEntity> findFiltered(
      @Param("headquarterId") Long headquarterId,
      @Param("openOnly") Boolean openOnly,
      Pageable pageable);
}
