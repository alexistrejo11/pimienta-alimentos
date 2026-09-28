package io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.product.core.application.query.ProductSearchCriteria;
import io.github.alexistrejo11.pimienta.module.product.core.domain.Product;
import io.github.alexistrejo11.pimienta.module.product.core.port.output.ProductRepository;
import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.outbound.persistence.entity.ProductJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepositoryImpl implements ProductRepository {

  /**
   * Used only when the database did not assign a SKU. PostgreSQL fills it in
   * trg_products_assign_sku; the H2 schema used by tests has no trigger.
   */
  private static final AtomicLong FALLBACK_SKU = new AtomicLong();

  private final ProductSpringDataRepository jpaRepository;

  @PersistenceContext private EntityManager entityManager;

  public ProductRepositoryImpl(ProductSpringDataRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<Product> findById(long id) {
    return jpaRepository.findByIdAndDeletedAtIsNull(id).map(ProductPersistenceMapper::toDomain);
  }

  @Override
  public Optional<Product> findByInventoryItemId(long inventoryItemId) {
    return jpaRepository
        .findByInventoryItemIdAndDeletedAtIsNull(inventoryItemId)
        .map(ProductPersistenceMapper::toDomain);
  }

  @Override
  public Optional<Product> findBySkuOrBarcode(String skuOrBarcode) {
    if (skuOrBarcode == null || skuOrBarcode.isBlank()) {
      return Optional.empty();
    }
    return jpaRepository
        .findActiveBySkuOrBarcode(skuOrBarcode.trim())
        .map(ProductPersistenceMapper::toDomain);
  }

  @Override
  public Page<Product> search(ProductSearchCriteria criteria, Pageable pageable) {
    Specification<ProductJpaEntity> spec = ProductSpecifications.fromCriteria(criteria);
    return jpaRepository.findAll(spec, pageable).map(ProductPersistenceMapper::toDomain);
  }

  @Override
  public List<Product> findPosCandidates(long headquarterId) {
    return jpaRepository.findPosCandidates(headquarterId).stream()
        .map(ProductPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public Product save(Product product) {
    boolean assignSku = product.getId() == null || product.getId() <= 0;
    ProductJpaEntity entity = ProductPersistenceMapper.toJpa(product);
    if (assignSku) {
      entity.setSku(null);
    }
    ProductJpaEntity saved = jpaRepository.save(entity);
    if (assignSku) {
      entityManager.flush();
      entityManager.refresh(saved);
      if (saved.getSku() == null || saved.getSku().isBlank()) {
        saved.setSku(fallbackSku());
        saved = jpaRepository.save(saved);
      }
    }
    return ProductPersistenceMapper.toDomain(saved);
  }

  private static String fallbackSku() {
    long next = FALLBACK_SKU.incrementAndGet();
    return "CAF-" + String.format("%06d", next);
  }

  @Override
  public boolean existsByBarcodeIgnoreCaseExcludingId(String barcode, Long excludeId) {
    if (barcode == null || barcode.isBlank()) {
      return false;
    }
    long excluded = excludeId != null && excludeId > 0 ? excludeId : 0L;
    return jpaRepository.existsByBarcodeIgnoreCaseAndIdNot(barcode.trim(), excluded);
  }
}
