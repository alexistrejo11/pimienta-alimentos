package io.github.alexistrejo11.pimienta.module.headquarter.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.UpsertHeadquarterItemCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.HeadquarterItemNotFoundException;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.HeadquarterNotFoundException;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.input.HeadquarterPosCatalogUseCases;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterItemRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.ItemNotFoundException;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.ItemRepository;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncTombstone;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosSyncTombstoneRepository;

@Service
public class HeadquarterPosCatalogUseCasesImpl implements HeadquarterPosCatalogUseCases {

  private final HeadquarterRepository headquarterRepository;
  private final HeadquarterItemRepository headquarterItemRepository;
  private final ItemRepository itemRepository;
  private final PosSyncTombstoneRepository tombstoneRepository;

  public HeadquarterPosCatalogUseCasesImpl(
      HeadquarterRepository headquarterRepository,
      HeadquarterItemRepository headquarterItemRepository,
      ItemRepository itemRepository,
      PosSyncTombstoneRepository tombstoneRepository) {
    this.headquarterRepository = headquarterRepository;
    this.headquarterItemRepository = headquarterItemRepository;
    this.itemRepository = itemRepository;
    this.tombstoneRepository = tombstoneRepository;
  }

  @Override
  public Page<HeadquarterItem> list(long headquarterId, Pageable pageable) {
    assertHeadquarterExists(headquarterId);
    return headquarterItemRepository.findByHeadquarterId(headquarterId, pageable);
  }

  @Override
  public HeadquarterItem get(long headquarterId, long itemId) {
    assertHeadquarterExists(headquarterId);
    return headquarterItemRepository
        .findByHeadquarterIdAndItemId(headquarterId, itemId)
        .orElseThrow(() -> new HeadquarterItemNotFoundException(headquarterId, itemId));
  }

  @Override
  @Transactional
  public HeadquarterItem upsert(
      long headquarterId, long itemId, UpsertHeadquarterItemCommand command) {
    assertHeadquarterExists(headquarterId);
    itemRepository.findById(itemId).orElseThrow(() -> new ItemNotFoundException(itemId));

    return headquarterItemRepository
        .findByHeadquarterIdAndItemId(headquarterId, itemId)
        .map(
            existing ->
                headquarterItemRepository.save(
                    HeadquarterItem.builder()
                        .withSaleCategory(
                            resolveCategory(command.saleCategory(), existing.getSaleCategory()))
                        .withSalePrice(resolvePrice(command.salePrice(), existing.getSalePrice()))
                        .withAvailable(
                            command.available() != null
                                ? command.available()
                                : existing.isAvailable())
                        .withStockPolicy(
                            command.stockPolicy() != null
                                ? command.stockPolicy()
                                : existing.getStockPolicy())
                        .withNegativeStockLimit(
                            command.negativeStockLimit() != null
                                ? command.negativeStockLimit()
                                : existing.getNegativeStockLimit())
                        .revise(existing)))
        .orElseGet(
            () ->
                headquarterItemRepository.save(
                    HeadquarterItem.builder()
                        .withHeadquarterId(headquarterId)
                        .withItemId(itemId)
                        .withSaleCategory(resolveCategory(command.saleCategory(), "GENERAL"))
                        .withSalePrice(resolvePrice(command.salePrice(), BigDecimal.ZERO))
                        .withAvailable(command.available() == null || command.available())
                        .withStockPolicy(
                            command.stockPolicy() != null
                                ? command.stockPolicy()
                                : StockPolicy.CONTROLLED)
                        .withNegativeStockLimit(command.negativeStockLimit())
                        .register()));
  }

  @Override
  public List<Item> candidates(long headquarterId) {
    assertHeadquarterExists(headquarterId);
    return itemRepository.findPosCandidates(headquarterId);
  }

  @Override
  @Transactional
  public HeadquarterItem softDelete(long headquarterId, long itemId) {
    assertHeadquarterExists(headquarterId);
    HeadquarterItem item = get(headquarterId, itemId);
    item.softDelete();
    HeadquarterItem saved = headquarterItemRepository.save(item);
    tombstoneRepository.save(PosSyncTombstone.product(headquarterId, itemId));
    return saved;
  }

  private void assertHeadquarterExists(long headquarterId) {
    headquarterRepository
        .findById(headquarterId)
        .orElseThrow(() -> new HeadquarterNotFoundException(headquarterId));
  }

  private static String resolveCategory(String incoming, String fallback) {
    if (incoming == null || incoming.isBlank()) {
      return fallback;
    }
    return incoming.strip();
  }

  private static BigDecimal resolvePrice(BigDecimal incoming, BigDecimal fallback) {
    return incoming != null ? incoming : fallback;
  }
}
