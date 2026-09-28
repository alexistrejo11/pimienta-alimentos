package io.github.alexistrejo11.pimienta.module.product.core.application;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemUnit;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.ItemHasStockException;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.InventoryRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.ItemRepository;
import io.github.alexistrejo11.pimienta.module.product.core.application.command.CreateProductCommand;
import io.github.alexistrejo11.pimienta.module.product.core.application.command.UpdateProductCommand;
import io.github.alexistrejo11.pimienta.module.product.core.application.query.ProductSearchCriteria;
import io.github.alexistrejo11.pimienta.module.product.core.domain.Product;
import io.github.alexistrejo11.pimienta.module.product.core.domain.Product.Status;
import io.github.alexistrejo11.pimienta.module.product.core.domain.exception.ProductBarcodeConflictException;
import io.github.alexistrejo11.pimienta.module.product.core.domain.exception.ProductNotFoundException;
import io.github.alexistrejo11.pimienta.module.product.core.domain.exception.ProductStockRequiredException;
import io.github.alexistrejo11.pimienta.module.product.core.port.input.ProductManagementUseCases;
import io.github.alexistrejo11.pimienta.module.product.core.port.output.ProductCatalogPolicy;
import io.github.alexistrejo11.pimienta.module.product.core.port.output.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductManagementUseCasesImpl implements ProductManagementUseCases {

  private final ProductRepository productRepository;
  private final ItemRepository itemRepository;
  private final InventoryRepository inventoryRepository;
  private final ProductCatalogPolicy catalogPolicy;

  public ProductManagementUseCasesImpl(
      ProductRepository productRepository,
      ItemRepository itemRepository,
      InventoryRepository inventoryRepository,
      ProductCatalogPolicy catalogPolicy) {
    this.productRepository = productRepository;
    this.itemRepository = itemRepository;
    this.inventoryRepository = inventoryRepository;
    this.catalogPolicy = catalogPolicy;
  }

  @Override
  public Page<Product> search(ProductSearchCriteria criteria, Pageable pageable) {
    ProductSearchCriteria effective = criteria != null ? criteria : ProductSearchCriteria.empty();
    return productRepository.search(effective, pageable);
  }

  @Override
  public Product getById(long id) {
    return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
  }

  @Override
  public Product getBySkuOrBarcode(String skuOrBarcode) {
    return productRepository
        .findBySkuOrBarcode(skuOrBarcode)
        .orElseThrow(() -> new ProductNotFoundException(skuOrBarcode));
  }

  @Override
  @Transactional
  public Product create(CreateProductCommand command) {
    String barcode = blankToNull(command.barcode());
    assertBarcodeUnique(barcode, null);

    Long inventoryItemId = null;
    if (command.trackStock()) {
      Item stockItem = createStockItem(command.name(), command.description(), command.unit());
      inventoryItemId = stockItem.getId();
    }

    Product product =
        Product.builder()
            .withName(command.name())
            .withDescription(command.description())
            .withUnit(command.unit())
            .withBarcode(barcode)
            .withStatus(Status.ACTIVE)
            .withInventoryItemId(inventoryItemId)
            .register();
    return productRepository.save(product);
  }

  @Override
  @Transactional
  public Product update(long id, UpdateProductCommand command) {
    Product existing = getById(id);
    String barcode = blankToNull(command.barcode());
    assertBarcodeUnique(barcode, id);

    Long inventoryItemId = existing.getInventoryItemId();
    if (command.trackStock() && inventoryItemId == null) {
      Item stockItem = createStockItem(command.name(), command.description(), command.unit());
      inventoryItemId = stockItem.getId();
    }
    if (!command.trackStock() && inventoryItemId != null) {
      if (catalogPolicy.isControlledAtAnyHeadquarter(id)) {
        throw new ProductStockRequiredException(id);
      }
      archiveStockItem(inventoryItemId);
      inventoryItemId = null;
    }

    existing.setName(command.name());
    existing.setDescription(command.description() != null ? command.description() : "");
    existing.setUnit(command.unit());
    existing.setBarcode(barcode);
    existing.setStatus(command.status() != null ? command.status() : existing.getStatus());
    existing.setInventoryItemId(inventoryItemId);
    existing.touch();

    if (inventoryItemId != null) {
      syncLinkedItem(inventoryItemId, existing);
    }
    return productRepository.save(existing);
  }

  @Override
  public List<Product> findPosCandidates(long headquarterId) {
    return productRepository.findPosCandidates(headquarterId);
  }

  private Item createStockItem(String name, String description, Product.Unit unit) {
    Item item =
        Item.create(
            name,
            description != null ? description : "",
            BigDecimal.ZERO,
            ItemCategory.FINISHED_GOOD,
            ItemUnit.valueOf(unit.name()),
            0,
            0);
    return itemRepository.save(item);
  }

  /** An unlinked item would otherwise surface as an editable warehouse item. */
  private void archiveStockItem(long inventoryItemId) {
    boolean hasStock =
        inventoryRepository.findByItemId(inventoryItemId).stream()
            .anyMatch(row -> row.getTotalQuantity() != 0 || row.getInTransitQuantity() != 0);
    if (hasStock) {
      throw new ItemHasStockException(inventoryItemId);
    }
    itemRepository
        .findById(inventoryItemId)
        .ifPresent(
            item -> {
              item.delete();
              itemRepository.save(item);
            });
  }

  private void syncLinkedItem(long inventoryItemId, Product product) {
    Item item =
        itemRepository
            .findById(inventoryItemId)
            .orElseThrow(() -> new ProductStockRequiredException(product.getId()));
    item.setName(product.getName());
    item.setDescription(product.getDescription());
    item.setUnit(ItemUnit.valueOf(product.getUnit().name()));
    itemRepository.save(item);
  }

  private void assertBarcodeUnique(String barcode, Long excludeId) {
    if (barcode == null) {
      return;
    }
    if (productRepository.existsByBarcodeIgnoreCaseExcludingId(barcode, excludeId)) {
      throw new ProductBarcodeConflictException(barcode);
    }
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.strip();
  }
}
