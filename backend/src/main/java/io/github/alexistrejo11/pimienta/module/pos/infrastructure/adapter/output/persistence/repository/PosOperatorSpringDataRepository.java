package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosOperatorJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PosOperatorSpringDataRepository extends JpaRepository<PosOperatorJpaEntity, Long> {

  Optional<PosOperatorJpaEntity> findByIdAndDeletedAtIsNull(Long id);

  Page<PosOperatorJpaEntity> findByDeletedAtIsNull(Pageable pageable);

  @Query(
      """
      SELECT o FROM PosOperatorJpaEntity o
      JOIN o.headquarterIds h
      WHERE h = :headquarterId AND o.deletedAt IS NULL
      """)
  List<PosOperatorJpaEntity> findByHeadquarterIdAndDeletedAtIsNull(
      @Param("headquarterId") Long headquarterId);

  @Query(
      """
      SELECT o FROM PosOperatorJpaEntity o
      JOIN o.headquarterIds h
      WHERE h = :headquarterId
        AND o.deletedAt IS NOT NULL
        AND o.updatedAt > :since
      """)
  List<PosOperatorJpaEntity> findDeletedByHeadquarterIdAndUpdatedAtAfter(
      @Param("headquarterId") Long headquarterId, @Param("since") java.time.LocalDateTime since);
}
