package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosChangeLogJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PosChangeLogSpringDataRepository extends JpaRepository<PosChangeLogJpaEntity, Long> {

  Optional<PosChangeLogJpaEntity> findFirstByHeadquarterIdOrderBySequenceAsc(Long headquarterId);

  Optional<PosChangeLogJpaEntity> findFirstByHeadquarterIdOrderBySequenceDesc(Long headquarterId);

  @Query("""
      select e from PosChangeLogJpaEntity e
      where e.headquarterId = :headquarterId
        and e.sequence > :sequence
        and e.sequence <= :upperBound
      order by e.sequence asc
      """)
  List<PosChangeLogJpaEntity> findAfterSequence(
      @Param("headquarterId") Long headquarterId,
      @Param("sequence") Long sequence,
      @Param("upperBound") Long upperBound,
      Pageable pageable);
}
