package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.ItemSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.ItemRepository;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.ItemJpaEntity;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.mapper.ItemPersistenceMapper;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.specification.ItemSpecifications;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    return jpaRepository
        .findByIdAndDeletedAtIsNull(id)
        .map(entity -> withProductLinks(List.of(entity)).getFirst());
  }

  @Override
  public Page<Item> search(ItemSearchCriteria criteria, Pageable pageable) {
    Specification<ItemJpaEntity> spec = ItemSpecifications.fromCriteria(criteria);
    Page<ItemJpaEntity> page = jpaRepository.findAll(spec, pageable);
    return new PageImpl<>(withProductLinks(page.getContent()), pageable, page.getTotalElements());
  }

  @Override
  public Item save(Item item) {
    ItemJpaEntity saved = jpaRepository.save(ItemPersistenceMapper.toJpa(item));
    return withProductLinks(List.of(saved)).getFirst();
  }

  /**
   * Formula columns on a managed entity keep the value from when it entered the persistence
   * context, so the product link is read with a query that always hits the database.
   */
  private List<Item> withProductLinks(List<ItemJpaEntity> entities) {
    if (entities.isEmpty()) {
      return List.of();
    }
    List<Long> ids = entities.stream().map(ItemJpaEntity::getId).toList();
    Map<Long, ItemProductLinkProjection> links =
        jpaRepository.findProductLinks(ids).stream()
            .collect(
                Collectors.toMap(
                    ItemProductLinkProjection::getItemId, Function.identity(), (a, b) -> a));
    return entities.stream()
        .map(
            entity -> {
              Item item = ItemPersistenceMapper.toDomain(entity);
              ItemProductLinkProjection link = links.get(entity.getId());
              item.setProductId(link != null ? link.getProductId() : null);
              item.setSaleSku(link != null ? link.getSku() : null);
              return item;
            })
        .toList();
  }
}
