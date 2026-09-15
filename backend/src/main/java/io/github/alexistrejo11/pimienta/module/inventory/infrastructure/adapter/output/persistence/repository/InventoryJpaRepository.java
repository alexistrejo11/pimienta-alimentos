package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.repository;

import java.util.List;
import java.util.Optional;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.InventoryJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryJpaRepository
    extends JpaRepository<InventoryJpaEntity, Long>, JpaSpecificationExecutor<InventoryJpaEntity> {

  Optional<InventoryJpaEntity> findByIdAndDeletedAtIsNull(Long id);

  Optional<InventoryJpaEntity> findByItemIdAndLocationIdAndDeletedAtIsNull(Long itemId, Long locationId);

  long countByLocationIdAndDeletedAtIsNull(Long locationId);

  List<InventoryJpaEntity> findByItemIdAndDeletedAtIsNullOrderByIdAsc(Long itemId);

  List<InventoryJpaEntity> findByLocationIdAndDeletedAtIsNullOrderByIdAsc(Long locationId);

  @Query("""
      select i from InventoryJpaEntity i join ItemJpaEntity it on i.itemId = it.id
      where i.deletedAt is null and it.deletedAt is null
        and i.availableQuantity > 0 and i.availableQuantity <= it.reorderPoint
      """)
  Page<InventoryJpaEntity> findLowStock(Pageable pageable);

  Page<InventoryJpaEntity> findByDeletedAtIsNullAndAvailableQuantity(int availableQuantity, Pageable pageable);

  @Query(value = """
      SELECT s.item_id AS itemId, i.sku AS sku, i.name AS name, i.category AS category,
             l.headquarter_id AS headquarterId, COALESCE(h.name, 'Almacén central') AS headquarterName,
             SUM(s.available_quantity) AS availableQuantity, SUM(s.reserved_quantity) AS reservedQuantity,
             SUM(s.in_transit_quantity) AS inTransitQuantity,
             SUM(s.available_quantity + s.reserved_quantity) AS totalQuantity,
             i.cost_price AS unitCost,
             i.cost_price * SUM(s.available_quantity + s.reserved_quantity) AS totalValue,
             CASE WHEN SUM(s.available_quantity) <= 0 THEN 'OUT_OF_STOCK'
                  WHEN SUM(s.available_quantity) <= i.reorder_point THEN 'LOW_STOCK' ELSE 'NORMAL' END AS status
      FROM inventory_stock s JOIN inventory_items i ON i.id = s.item_id
           JOIN storage_locations l ON l.id = s.location_id
           LEFT JOIN headquarters h ON h.id = l.headquarter_id
      WHERE s.deleted_at IS NULL AND i.deleted_at IS NULL AND l.deleted_at IS NULL
        AND (:search IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(i.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(i.barcode) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:category IS NULL OR i.category = :category)
        AND (:minCost IS NULL OR i.cost_price >= :minCost)
        AND (:maxCost IS NULL OR i.cost_price <= :maxCost)
        AND (:headquarters IS NULL OR l.headquarter_id IN (:headquarters))
      GROUP BY s.item_id, i.sku, i.name, i.category, i.cost_price, i.reorder_point, l.headquarter_id, h.name
      HAVING (:status IS NULL OR CASE WHEN SUM(s.available_quantity) <= 0 THEN 'OUT_OF_STOCK' WHEN SUM(s.available_quantity) <= i.reorder_point THEN 'LOW_STOCK' ELSE 'NORMAL' END = :status)
      ORDER BY i.name, l.headquarter_id
      """, countQuery = """
      SELECT COUNT(*) FROM (SELECT s.item_id, l.headquarter_id
      FROM inventory_stock s JOIN inventory_items i ON i.id=s.item_id JOIN storage_locations l ON l.id=s.location_id
      WHERE s.deleted_at IS NULL AND i.deleted_at IS NULL AND l.deleted_at IS NULL
        AND (:search IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(i.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(i.barcode) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:category IS NULL OR i.category = :category) AND (:minCost IS NULL OR i.cost_price >= :minCost) AND (:maxCost IS NULL OR i.cost_price <= :maxCost) AND (:headquarters IS NULL OR l.headquarter_id IN (:headquarters))
      GROUP BY s.item_id, l.headquarter_id, i.reorder_point
      HAVING (:status IS NULL OR CASE WHEN SUM(s.available_quantity) <= 0 THEN 'OUT_OF_STOCK' WHEN SUM(s.available_quantity) <= i.reorder_point THEN 'LOW_STOCK' ELSE 'NORMAL' END = :status)) x
      """, nativeQuery = true)
  Page<InventoryGlobalSummaryProjection> searchGlobalSummary(@Param("search") String search, @Param("category") String category, @Param("status") String status, @Param("minCost") java.math.BigDecimal minCost, @Param("maxCost") java.math.BigDecimal maxCost, @Param("headquarters") List<Long> headquarters, Pageable pageable);
}
