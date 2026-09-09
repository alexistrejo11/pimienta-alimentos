package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.ItemJpaEntity;

import java.util.Optional;
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

  @Query("""
      select case when count(e) > 0 then true else false end from ItemJpaEntity e
      where e.barcode is not null
        and lower(e.barcode) = lower(:barcode)
        and e.id <> :excludeId
      """)
  boolean existsByBarcodeIgnoreCaseAndIdNot(
      @Param("barcode") String barcode, @Param("excludeId") Long excludeId);
}
