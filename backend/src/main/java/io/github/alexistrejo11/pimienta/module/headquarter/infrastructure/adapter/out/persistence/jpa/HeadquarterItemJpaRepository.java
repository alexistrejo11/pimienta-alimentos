package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;

public interface HeadquarterItemJpaRepository
    extends JpaRepository<HeadquarterItemJpaEntity, Long> {

  Optional<HeadquarterItemJpaEntity> findByHeadquarterIdAndItemIdAndDeletedAtIsNull(
      Long headquarterId, Long itemId);

  Page<HeadquarterItemJpaEntity> findByHeadquarterIdAndDeletedAtIsNull(
      Long headquarterId, Pageable pageable);

  @Query("""
      select h from HeadquarterItemJpaEntity h, ItemJpaEntity i
      where h.headquarterId = :headquarterId and h.deletedAt is null and h.itemId = i.id
        and (:search is null or lower(i.name) like lower(concat('%', :search, '%'))
          or lower(i.sku) like lower(concat('%', :search, '%'))
          or lower(coalesce(i.barcode, '')) like lower(concat('%', :search, '%')))
        and (:saleCategory is null or lower(h.saleCategory) = lower(:saleCategory))
        and (:available is null or h.available = :available)
        and (:stockPolicy is null or h.stockPolicy = :stockPolicy)
      order by lower(i.name), h.id
      """)
  Page<HeadquarterItemJpaEntity> search(
      @Param("headquarterId") Long headquarterId, @Param("search") String search,
      @Param("saleCategory") String saleCategory, @Param("available") Boolean available,
      @Param("stockPolicy") StockPolicy stockPolicy, Pageable pageable);

  List<HeadquarterItemJpaEntity> findByHeadquarterIdAndDeletedAtIsNull(Long headquarterId);

  List<HeadquarterItemJpaEntity> findByItemIdAndDeletedAtIsNull(Long itemId);

  List<HeadquarterItemJpaEntity> findByHeadquarterIdAndDeletedAtIsNotNullAndDeletedAtAfter(
      Long headquarterId, LocalDateTime deletedAt);
}
