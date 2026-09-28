package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HeadquarterItemJpaRepository
    extends JpaRepository<HeadquarterItemJpaEntity, Long> {

  Optional<HeadquarterItemJpaEntity> findByHeadquarterIdAndProductIdAndDeletedAtIsNull(
      Long headquarterId, Long productId);

  Page<HeadquarterItemJpaEntity> findByHeadquarterIdAndDeletedAtIsNull(
      Long headquarterId, Pageable pageable);

  List<HeadquarterItemJpaEntity> findByHeadquarterIdAndDeletedAtIsNull(Long headquarterId);

  List<HeadquarterItemJpaEntity> findByProductIdAndDeletedAtIsNull(Long productId);

  boolean existsByProductIdAndStockPolicyAndDeletedAtIsNull(
      Long productId, io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy stockPolicy);

  List<HeadquarterItemJpaEntity> findByHeadquarterIdAndDeletedAtIsNotNullAndDeletedAtAfter(
      Long headquarterId, LocalDateTime deletedAt);
}
