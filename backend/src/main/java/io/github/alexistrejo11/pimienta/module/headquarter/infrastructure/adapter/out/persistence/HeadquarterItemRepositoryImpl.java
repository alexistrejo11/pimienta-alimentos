package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterItemRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa.HeadquarterItemJpaEntity;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa.HeadquarterItemJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class HeadquarterItemRepositoryImpl implements HeadquarterItemRepository {

  private final HeadquarterItemJpaRepository jpaRepository;

  public HeadquarterItemRepositoryImpl(HeadquarterItemJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<HeadquarterItem> findByHeadquarterIdAndItemId(long headquarterId, long itemId) {
    return jpaRepository
        .findByHeadquarterIdAndItemIdAndDeletedAtIsNull(headquarterId, itemId)
        .map(HeadquarterItemPersistenceMapper::toDomain);
  }

  @Override
  public Page<HeadquarterItem> findByHeadquarterId(long headquarterId, Pageable pageable) {
    return jpaRepository
        .findByHeadquarterIdAndDeletedAtIsNull(headquarterId, pageable)
        .map(HeadquarterItemPersistenceMapper::toDomain);
  }

  @Override
  public List<HeadquarterItem> findAllByHeadquarterId(long headquarterId) {
    return jpaRepository.findByHeadquarterIdAndDeletedAtIsNull(headquarterId).stream()
        .map(HeadquarterItemPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public List<HeadquarterItem> findDeletedByHeadquarterIdAndDeletedAtAfter(
      long headquarterId, LocalDateTime since) {
    return jpaRepository
        .findByHeadquarterIdAndDeletedAtIsNotNullAndDeletedAtAfter(headquarterId, since)
        .stream()
        .map(HeadquarterItemPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public HeadquarterItem save(HeadquarterItem item) {
    HeadquarterItemJpaEntity saved =
        jpaRepository.save(HeadquarterItemPersistenceMapper.toEntity(item));
    return HeadquarterItemPersistenceMapper.toDomain(saved);
  }
}
