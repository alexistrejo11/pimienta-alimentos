package io.github.alexistrejo11.pimienta.module.inventory.core.application.usecase;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.ItemSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.ItemBarcodeConflictException;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.ItemNotFoundException;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.ItemSkuConflictException;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.ItemManagementUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.ItemRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ItemManagementUseCasesImpl implements ItemManagementUseCases {

  private static final Logger log = LoggerFactory.getLogger(ItemManagementUseCasesImpl.class);

  private final ItemRepository itemRepository;

  public ItemManagementUseCasesImpl(ItemRepository itemRepository) {
    this.itemRepository = itemRepository;
  }

  @Override
  public Page<Item> search(ItemSearchCriteria criteria, Pageable pageable) {
    ItemSearchCriteria effective = criteria != null ? criteria : ItemSearchCriteria.empty();

    log.debug(
        "search items query start page={} size={} category={} status={} nameLen={} skuLen={}",
        pageable != null ? pageable.getPageNumber() : null,
        pageable != null ? pageable.getPageSize() : null,
        effective.category(),
        effective.status(),
        effective.name() != null ? effective.name().length() : 0,
        effective.sku() != null ? effective.sku().length() : 0);

    Page<Item> page = itemRepository.search(effective, pageable);

    log.debug(
        "search items query complete totalElements={} numberOfElements={}",
        page.getTotalElements(),
        page.getNumberOfElements());
    return page;
  }

  @Override
  public Item getById(Long id) {
    log.debug("get item by id query start itemId={}", id);

    Item item = itemRepository.findById(id).orElseThrow(() -> new ItemNotFoundException(id));

    log.debug("get item by id query complete itemId={}", item.getId());
    return item;
  }

  @Override
  public Item getBySkuOrBarcode(String skuOrBarcode) {
    int keyLen = skuOrBarcode != null ? skuOrBarcode.length() : 0;
    log.debug("get item by sku or barcode query start keyLen={}", keyLen);

    Item item = itemRepository
        .findBySkuOrBarcode(skuOrBarcode)
        .orElseThrow(() -> new ItemNotFoundException(skuOrBarcode));

    log.debug("get item by sku or barcode query complete itemId={}", item.getId());
    return item;
  }

  @Override
  public Item create(Item item) {
    if (item.getSku() == null || item.getSku().isBlank()) {
      item.setSku(itemRepository.nextInternalSku());
    }
    log.info(
        "create item start sku={} category={} status={} unit={}",
        item.getSku(),
        item.getCategory(),
        item.getStatus(),
        item.getUnit());

    if (itemRepository.existsBySkuIgnoreCaseExcludingId(item.getSku(), null)) {
      throw new ItemSkuConflictException(item.getSku());
    }
    assertBarcodeUnique(item.getBarcode(), null);

    Item saved = itemRepository.save(item);

    log.info("create item complete itemId={} sku={}", saved.getId(), saved.getSku());
    return saved;
  }

  @Override
  public Item update(Long id, Item merged) {
    log.info("update item start itemId={} sku={}", id, merged.getSku());

    Item existing = getById(id);

    if (itemRepository.existsBySkuIgnoreCaseExcludingId(merged.getSku(), id)) {
      throw new ItemSkuConflictException(merged.getSku());
    }
    assertBarcodeUnique(merged.getBarcode(), id);
    existing.setSku(merged.getSku());
    existing.setName(merged.getName());
    existing.setDescription(merged.getDescription());
    existing.setCategory(merged.getCategory());
    existing.setUnit(merged.getUnit());
    existing.setBrand(merged.getBrand());
    existing.setBarcode(merged.getBarcode());
    existing.setCostPrice(merged.getCostPrice());
    existing.setSalePrice(merged.getSalePrice());
    existing.setReorderPoint(merged.getReorderPoint());
    existing.setReorderQuantity(merged.getReorderQuantity());
    existing.setStatus(merged.getStatus());

    Item saved = itemRepository.save(existing);

    log.info("update item complete itemId={}", saved.getId());
    return saved;
  }

  @Override
  public Item discontinue(Long id) {
    log.info("discontinue item start itemId={}", id);

    Item item = getById(id);
    item.discontinue();

    Item saved = itemRepository.save(item);
    log.info("discontinue item complete itemId={}", saved.getId());
    return saved;
  }

  @Override
  public Item activate(Long id) {
    log.info("activate item start itemId={}", id);

    Item item = getById(id);
    item.activate();

    Item saved = itemRepository.save(item);
    log.info("activate item complete itemId={}", saved.getId());
    return saved;
  }

  @Override
  public void delete(Long id) {
    log.info("delete item start itemId={}", id);

    Item item = getById(id);
    item.delete();

    itemRepository.save(item);
    log.info("delete item complete itemId={}", id);
  }

  private void assertBarcodeUnique(String barcode, Long excludeId) {
    if (barcode == null || barcode.isBlank()) {
      return;
    }
    if (itemRepository.existsByBarcodeIgnoreCaseExcludingId(barcode, excludeId)) {
      throw new ItemBarcodeConflictException(barcode.trim());
    }
  }
}
