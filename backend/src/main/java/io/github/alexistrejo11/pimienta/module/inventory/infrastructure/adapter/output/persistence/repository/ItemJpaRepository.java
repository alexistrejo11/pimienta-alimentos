package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.ItemJpaEntity;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemJpaRepository
    extends JpaRepository<ItemJpaEntity, Long>, JpaSpecificationExecutor<ItemJpaEntity> {

  Optional<ItemJpaEntity> findByIdAndDeletedAtIsNull(Long id);

  @Query("""
      select e from ItemJpaEntity e
      where e.deletedAt is null
        and (lower(e.sku) = lower(:key) or (e.barcode is not null and lower(e.barcode) = lower(:key)))
      """)
  Optional<ItemJpaEntity> findActiveBySkuOrBarcode(@Param("key") String key);

  boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

  @Query(value = "select next_internal_item_sku()", nativeQuery = true)
  String nextInternalSku();

  @Query(value = "select i.* from inventory_items i where i.deleted_at is null and i.status = 'ACTIVE' and i.catalog_role = 'POS_SELLABLE' and not exists (select 1 from headquarter_items h where h.item_id = i.id and h.headquarter_id = :headquarterId and h.deleted_at is null) order by i.name", nativeQuery = true)
  List<ItemJpaEntity> findPosCandidates(@Param("headquarterId") Long headquarterId);

  @Query("""
      select case when count(e) > 0 then true else false end from ItemJpaEntity e
      where e.barcode is not null
        and lower(e.barcode) = lower(:barcode)
        and e.id <> :excludeId
      """)
  boolean existsByBarcodeIgnoreCaseAndIdNot(
      @Param("barcode") String barcode, @Param("excludeId") Long excludeId);
}
