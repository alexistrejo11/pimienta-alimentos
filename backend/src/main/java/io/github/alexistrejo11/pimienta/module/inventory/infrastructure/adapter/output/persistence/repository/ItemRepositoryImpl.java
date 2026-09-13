package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.ItemSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.ItemRepository;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.specification.ItemSpecifications;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.ItemJpaEntity;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.mapper.ItemPersistenceMapper;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class ItemRepositoryImpl implements ItemRepository {

  private final ItemJpaRepository jpaRepository;

  public ItemRepositoryImpl(ItemJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<Item> findById(long id) {
    return jpaRepository.findByIdAndDeletedAtIsNull(id).map(ItemPersistenceMapper::toDomain);
  }

  @Override
  public Optional<Item> findBySkuOrBarcode(String skuOrBarcode) {
    if (skuOrBarcode == null || skuOrBarcode.isBlank()) {
      return Optional.empty();
    }
    return jpaRepository
        .findActiveBySkuOrBarcode(skuOrBarcode.trim())
        .map(ItemPersistenceMapper::toDomain);
  }

  @Override
  public Page<Item> search(ItemSearchCriteria criteria, Pageable pageable) {
    Specification<ItemJpaEntity> spec = ItemSpecifications.fromCriteria(criteria);
    return jpaRepository.findAll(spec, pageable).map(ItemPersistenceMapper::toDomain);
  }

  @Override
  public Item save(Item item) {
    ItemJpaEntity entity = ItemPersistenceMapper.toJpa(item);
    ItemJpaEntity saved = jpaRepository.save(entity);
    return ItemPersistenceMapper.toDomain(saved);
  }

  @Override
  public String nextInternalSku() {
    try {
      return jpaRepository.nextInternalSku();
    } catch (DataAccessException ex) {
      // Keep local/test databases usable when the optional database function is absent.
      return "CAF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
  }

  @Override
  public List<Item> findPosCandidates(long headquarterId) {
    return jpaRepository.findPosCandidates(headquarterId).stream().map(ItemPersistenceMapper::toDomain).toList();
  }

  @Override
  public boolean existsBySkuIgnoreCaseExcludingId(String sku, Long excludeId) {
    if (sku == null || sku.isBlank()) {
      return false;
    }
    Long ex = excludeId != null && excludeId > 0 ? excludeId : 0L;
    return jpaRepository.existsBySkuIgnoreCaseAndIdNot(sku.trim(), ex);
  }

  @Override
  public boolean existsByBarcodeIgnoreCaseExcludingId(String barcode, Long excludeId) {
    if (barcode == null || barcode.isBlank()) {
      return false;
    }
    Long ex = excludeId != null && excludeId > 0 ? excludeId : 0L;
    return jpaRepository.existsByBarcodeIgnoreCaseAndIdNot(barcode.trim(), ex);
  }
}
