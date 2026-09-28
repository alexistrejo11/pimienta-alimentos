package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.ItemJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemJpaRepository
    extends JpaRepository<ItemJpaEntity, Long>, JpaSpecificationExecutor<ItemJpaEntity> {

  Optional<ItemJpaEntity> findByIdAndDeletedAtIsNull(Long id);

  @Query(
      value =
          """
          select p.inventory_item_id as itemId, p.id as productId, p.sku as sku
          from products p
          where p.inventory_item_id in (:itemIds) and p.deleted_at is null
          """,
      nativeQuery = true)
  List<ItemProductLinkProjection> findProductLinks(@Param("itemIds") Collection<Long> itemIds);
}
