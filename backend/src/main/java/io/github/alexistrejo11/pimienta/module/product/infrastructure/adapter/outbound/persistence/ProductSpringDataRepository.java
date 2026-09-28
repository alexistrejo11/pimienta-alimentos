package io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.outbound.persistence.entity.ProductJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductSpringDataRepository
    extends JpaRepository<ProductJpaEntity, Long>, JpaSpecificationExecutor<ProductJpaEntity> {

  Optional<ProductJpaEntity> findByIdAndDeletedAtIsNull(Long id);

  Optional<ProductJpaEntity> findByInventoryItemIdAndDeletedAtIsNull(Long inventoryItemId);

  @Query(
      """
      select p from ProductJpaEntity p
      where p.deletedAt is null
        and p.status = io.github.alexistrejo11.pimienta.module.product.core.domain.Product.Status.ACTIVE
        and (lower(p.sku) = lower(:key) or (p.barcode is not null and lower(p.barcode) = lower(:key)))
      """)
  Optional<ProductJpaEntity> findActiveBySkuOrBarcode(@Param("key") String key);

  @Query(
      """
      select case when count(p) > 0 then true else false end from ProductJpaEntity p
      where p.barcode is not null
        and lower(p.barcode) = lower(:barcode)
        and p.deletedAt is null
        and p.id <> :excludeId
      """)
  boolean existsByBarcodeIgnoreCaseAndIdNot(
      @Param("barcode") String barcode, @Param("excludeId") Long excludeId);

  @Query(
      value =
          """
          select p.* from products p
          where p.deleted_at is null
            and p.status = 'ACTIVE'
            and not exists (
              select 1 from headquarter_items h
              where h.product_id = p.id
                and h.headquarter_id = :headquarterId
                and h.deleted_at is null
            )
          order by p.name
          """,
      nativeQuery = true)
  List<ProductJpaEntity> findPosCandidates(@Param("headquarterId") Long headquarterId);
}
