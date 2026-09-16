package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterItemRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa.HeadquarterItemJpaEntity;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa.HeadquarterItemJpaRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.application.PosChangeLogService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;

@Repository
public class HeadquarterItemRepositoryImpl implements HeadquarterItemRepository {

  private final HeadquarterItemJpaRepository jpaRepository;
  private final PosChangeLogService posChangeLogService;

  public HeadquarterItemRepositoryImpl(
      HeadquarterItemJpaRepository jpaRepository, PosChangeLogService posChangeLogService) {
    this.jpaRepository = jpaRepository;
    this.posChangeLogService = posChangeLogService;
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
  public Page<HeadquarterItem> search(long headquarterId, String search, String saleCategory,
      Boolean available, StockPolicy stockPolicy, Pageable pageable) {
    return jpaRepository.search(headquarterId, blankToNull(search), blankToNull(saleCategory),
        available, stockPolicy, pageable).map(HeadquarterItemPersistenceMapper::toDomain);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  @Override
  public List<HeadquarterItem> findAllByHeadquarterId(long headquarterId) {
    return jpaRepository.findByHeadquarterIdAndDeletedAtIsNull(headquarterId).stream()
        .map(HeadquarterItemPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public List<HeadquarterItem> findAllByItemId(long itemId) {
    return jpaRepository.findByItemIdAndDeletedAtIsNull(itemId).stream()
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
    HeadquarterItem result = HeadquarterItemPersistenceMapper.toDomain(saved);
    posChangeLogService.appendCatalogItem(result);
    return result;
  }
}
