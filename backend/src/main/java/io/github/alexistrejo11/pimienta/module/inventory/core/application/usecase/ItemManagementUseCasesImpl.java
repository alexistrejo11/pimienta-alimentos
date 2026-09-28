package io.github.alexistrejo11.pimienta.module.inventory.core.application.usecase;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.UpdateItemIdentityCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.UpdateItemStockSettingsCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.ItemSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.ItemLinkedToProductException;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.ItemNotFoundException;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.ItemManagementUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.ItemRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemManagementUseCasesImpl implements ItemManagementUseCases {

  private final ItemRepository itemRepository;

  public ItemManagementUseCasesImpl(ItemRepository itemRepository) {
    this.itemRepository = itemRepository;
  }

  @Override
  public Page<Item> search(ItemSearchCriteria criteria, Pageable pageable) {
    ItemSearchCriteria effective = criteria != null ? criteria : ItemSearchCriteria.empty();
    return itemRepository.search(effective, pageable);
  }

  @Override
  public Item getById(Long id) {
    return itemRepository.findById(id).orElseThrow(() -> new ItemNotFoundException(id));
  }

  @Override
  @Transactional
  public Item create(Item item) {
    return itemRepository.save(item);
  }

  @Override
  @Transactional
  public Item updateIdentity(Long id, UpdateItemIdentityCommand command) {
    Item existing = getById(id);
    assertWarehouseItem(existing);
    existing.setName(command.name());
    existing.setDescription(command.description() != null ? command.description() : "");
    existing.setCategory(command.category());
    existing.setUnit(command.unit());
    existing.setBrand(command.brand());
    existing.setUpdatedAt(LocalDateTime.now());
    return itemRepository.save(existing);
  }

  @Override
  @Transactional
  public Item updateStockSettings(Long id, UpdateItemStockSettingsCommand command) {
    Item existing = getById(id);
    existing.setCostPrice(command.costPrice() != null ? command.costPrice() : BigDecimal.ZERO);
    existing.setReorderPoint(command.reorderPoint());
    existing.setReorderQuantity(command.reorderQuantity());
    if (command.status() != null) {
      existing.setStatus(command.status());
    }
    existing.setUpdatedAt(LocalDateTime.now());
    return itemRepository.save(existing);
  }

  @Override
  @Transactional
  public Item discontinue(Long id) {
    Item item = getById(id);
    item.discontinue();
    return itemRepository.save(item);
  }

  @Override
  @Transactional
  public Item activate(Long id) {
    Item item = getById(id);
    item.activate();
    return itemRepository.save(item);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    Item item = getById(id);
    assertWarehouseItem(item);
    item.delete();
    itemRepository.save(item);
  }

  private static void assertWarehouseItem(Item item) {
    if (item.isLinkedToProduct()) {
      throw new ItemLinkedToProductException(item.getId(), item.getProductId());
    }
  }
}
