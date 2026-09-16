package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterItemRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa.HeadquarterItemJpaEntity;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa.HeadquarterItemJpaRepository;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.ItemJpaEntity;
import io.github.alexistrejo11.pimienta.module.pos.core.application.PosChangeLogService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class HeadquarterItemRepositoryImpl implements HeadquarterItemRepository {

  private final HeadquarterItemJpaRepository jpaRepository;
  private final PosChangeLogService posChangeLogService;
  private final EntityManager entityManager;

  public HeadquarterItemRepositoryImpl(
      HeadquarterItemJpaRepository jpaRepository,
      PosChangeLogService posChangeLogService,
      EntityManager entityManager) {
    this.jpaRepository = jpaRepository;
    this.posChangeLogService = posChangeLogService;
    this.entityManager = entityManager;
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
  public Page<HeadquarterItem> search(
      long headquarterId,
      String search,
      String saleCategory,
      Boolean available,
      StockPolicy stockPolicy,
      Pageable pageable) {
    String searchTerm = blankToNull(search);
    String category = blankToNull(saleCategory);
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();

    CriteriaQuery<HeadquarterItemJpaEntity> cq = cb.createQuery(HeadquarterItemJpaEntity.class);
    Root<HeadquarterItemJpaEntity> catalog = cq.from(HeadquarterItemJpaEntity.class);
    Root<ItemJpaEntity> item = cq.from(ItemJpaEntity.class);
    cq.select(catalog)
        .where(predicates(cb, catalog, item, headquarterId, searchTerm, category, available, stockPolicy))
        .orderBy(cb.asc(cb.lower(item.get("name"))), cb.asc(catalog.get("id")));
    List<HeadquarterItemJpaEntity> rows =
        entityManager
            .createQuery(cq)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();

    CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
    Root<HeadquarterItemJpaEntity> countCatalog = countQuery.from(HeadquarterItemJpaEntity.class);
    Root<ItemJpaEntity> countItem = countQuery.from(ItemJpaEntity.class);
    countQuery
        .select(cb.count(countCatalog))
        .where(
            predicates(
                cb,
                countCatalog,
                countItem,
                headquarterId,
                searchTerm,
                category,
                available,
                stockPolicy));
    long total = entityManager.createQuery(countQuery).getSingleResult();

    List<HeadquarterItem> content =
        rows.stream().map(HeadquarterItemPersistenceMapper::toDomain).toList();
    return new PageImpl<>(content, pageable, total);
  }

  private static Predicate[] predicates(
      CriteriaBuilder cb,
      Root<HeadquarterItemJpaEntity> catalog,
      Root<ItemJpaEntity> item,
      long headquarterId,
      String search,
      String saleCategory,
      Boolean available,
      StockPolicy stockPolicy) {
    List<Predicate> parts = new ArrayList<>();
    parts.add(cb.equal(catalog.get("headquarterId"), headquarterId));
    parts.add(cb.isNull(catalog.get("deletedAt")));
    parts.add(cb.equal(catalog.get("itemId"), item.get("id")));
    if (search != null) {
      String term = "%" + search.toLowerCase() + "%";
      parts.add(
          cb.or(
              cb.like(cb.lower(item.get("name")), term),
              cb.like(cb.lower(item.get("sku")), term),
              cb.and(
                  cb.isNotNull(item.get("barcode")),
                  cb.like(cb.lower(item.get("barcode")), term))));
    }
    if (saleCategory != null) {
      parts.add(cb.equal(cb.lower(catalog.get("saleCategory")), saleCategory.toLowerCase()));
    }
    if (available != null) {
      parts.add(cb.equal(catalog.get("available"), available));
    }
    if (stockPolicy != null) {
      parts.add(cb.equal(catalog.get("stockPolicy"), stockPolicy));
    }
    return parts.toArray(Predicate[]::new);
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
