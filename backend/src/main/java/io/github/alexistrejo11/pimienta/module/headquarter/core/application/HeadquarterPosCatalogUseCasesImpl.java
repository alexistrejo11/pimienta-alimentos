package io.github.alexistrejo11.pimienta.module.headquarter.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.UpsertHeadquarterItemCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.HeadquarterItemNotFoundException;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.HeadquarterNotFoundException;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.input.HeadquarterPosCatalogUseCases;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterItemRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncTombstone;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosSyncTombstoneRepository;
import io.github.alexistrejo11.pimienta.module.product.core.domain.Product;
import io.github.alexistrejo11.pimienta.module.product.core.domain.exception.ProductNotFoundException;
import io.github.alexistrejo11.pimienta.module.product.core.domain.exception.ProductStockRequiredException;
import io.github.alexistrejo11.pimienta.module.product.core.port.output.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HeadquarterPosCatalogUseCasesImpl implements HeadquarterPosCatalogUseCases {

  private final HeadquarterRepository headquarterRepository;
  private final HeadquarterItemRepository headquarterItemRepository;
  private final ProductRepository productRepository;
  private final PosSyncTombstoneRepository tombstoneRepository;

  public HeadquarterPosCatalogUseCasesImpl(
      HeadquarterRepository headquarterRepository,
      HeadquarterItemRepository headquarterItemRepository,
      ProductRepository productRepository,
      PosSyncTombstoneRepository tombstoneRepository) {
    this.headquarterRepository = headquarterRepository;
    this.headquarterItemRepository = headquarterItemRepository;
    this.productRepository = productRepository;
    this.tombstoneRepository = tombstoneRepository;
  }

  @Override
  public Page<HeadquarterItem> list(long headquarterId, Pageable pageable) {
    assertHeadquarterExists(headquarterId);
    return headquarterItemRepository.findByHeadquarterId(headquarterId, pageable);
  }

  @Override
  public Page<HeadquarterItem> search(
      long headquarterId,
      String search,
      String saleCategory,
      Boolean available,
      StockPolicy stockPolicy,
      Pageable pageable) {
    assertHeadquarterExists(headquarterId);
    return headquarterItemRepository.search(
        headquarterId, search, saleCategory, available, stockPolicy, pageable);
  }

  @Override
  public HeadquarterItem get(long headquarterId, long productId) {
    assertHeadquarterExists(headquarterId);
    return headquarterItemRepository
        .findByHeadquarterIdAndProductId(headquarterId, productId)
        .orElseThrow(() -> new HeadquarterItemNotFoundException(headquarterId, productId));
  }

  @Override
  @Transactional
  public HeadquarterItem upsert(
      long headquarterId, long productId, UpsertHeadquarterItemCommand command) {
    assertHeadquarterExists(headquarterId);
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

    StockPolicy policy =
        command.stockPolicy() != null ? command.stockPolicy() : StockPolicy.CONTROLLED;
    assertControlledHasStock(product, policy);

    return headquarterItemRepository
        .findByHeadquarterIdAndProductId(headquarterId, productId)
        .map(
            existing -> {
              StockPolicy resolved =
                  command.stockPolicy() != null ? command.stockPolicy() : existing.getStockPolicy();
              assertControlledHasStock(product, resolved);
              return headquarterItemRepository.save(
                  HeadquarterItem.builder()
                      .withSaleCategory(
                          resolveCategory(command.saleCategory(), existing.getSaleCategory()))
                      .withSalePrice(resolvePrice(command.salePrice(), existing.getSalePrice()))
                      .withAvailable(
                          command.available() != null ? command.available() : existing.isAvailable())
                      .withStockPolicy(resolved)
                      .withNegativeStockLimit(
                          command.negativeStockLimit() != null
                              ? command.negativeStockLimit()
                              : existing.getNegativeStockLimit())
                      .revise(existing));
            })
        .orElseGet(
            () ->
                headquarterItemRepository.save(
                    HeadquarterItem.builder()
                        .withHeadquarterId(headquarterId)
                        .withProductId(productId)
                        .withSaleCategory(resolveCategory(command.saleCategory(), "GENERAL"))
                        .withSalePrice(resolvePrice(command.salePrice(), BigDecimal.ZERO))
                        .withAvailable(command.available() == null || command.available())
                        .withStockPolicy(policy)
                        .withNegativeStockLimit(command.negativeStockLimit())
                        .register()));
  }

  @Override
  public List<Product> candidates(long headquarterId) {
    assertHeadquarterExists(headquarterId);
    return productRepository.findPosCandidates(headquarterId);
  }

  @Override
  @Transactional
  public HeadquarterItem softDelete(long headquarterId, long productId) {
    assertHeadquarterExists(headquarterId);
    HeadquarterItem item = get(headquarterId, productId);
    item.softDelete();
    HeadquarterItem saved = headquarterItemRepository.save(item);
    tombstoneRepository.save(PosSyncTombstone.product(headquarterId, productId));
    return saved;
  }

  private void assertHeadquarterExists(long headquarterId) {
    headquarterRepository
        .findById(headquarterId)
        .orElseThrow(() -> new HeadquarterNotFoundException(headquarterId));
  }

  private static void assertControlledHasStock(Product product, StockPolicy policy) {
    if (policy == StockPolicy.CONTROLLED && product.getInventoryItemId() == null) {
      throw new ProductStockRequiredException(product.getId());
    }
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
