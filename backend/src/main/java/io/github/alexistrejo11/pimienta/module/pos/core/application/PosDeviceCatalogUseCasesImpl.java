package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.application.PosProductManagementUseCases;
import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.CreatePosProductCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.UpsertHeadquarterItemCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.PosSaleCategoryNotFoundException;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.input.HeadquarterPosCatalogUseCases;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.PosSaleCategoryRepository;
import io.github.alexistrejo11.pimienta.module.product.core.domain.Product;
import io.github.alexistrejo11.pimienta.module.product.core.domain.Product.Unit;
import io.github.alexistrejo11.pimienta.module.product.core.port.input.ProductManagementUseCases;
import io.github.alexistrejo11.pimienta.module.product.core.application.command.UpdateProductCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.DeviceCreatePosProductCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosDevice;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosOperator;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosDeviceNotFoundException;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosDeviceRevokedException;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosDeviceCatalogUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncBootstrapUseCases.ProductRow;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosDeviceRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosOperatorRepository;
import io.github.alexistrejo11.pimienta.shared.exception.BusinessValidationException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PosDeviceCatalogUseCasesImpl implements PosDeviceCatalogUseCases {

  private final PosDeviceRepository deviceRepository;
  private final PosOperatorRepository operatorRepository;
  private final PosSaleCategoryRepository saleCategories;
  private final PosProductManagementUseCases posProductManagementUseCases;
  private final PosSyncCatalogProjector projector;
  private final ProductManagementUseCases productManagementUseCases;
  private final HeadquarterPosCatalogUseCases headquarterPosCatalogUseCases;

  public PosDeviceCatalogUseCasesImpl(
      PosDeviceRepository deviceRepository,
      PosOperatorRepository operatorRepository,
      PosSaleCategoryRepository saleCategories,
      PosProductManagementUseCases posProductManagementUseCases,
      PosSyncCatalogProjector projector,
      ProductManagementUseCases productManagementUseCases,
      HeadquarterPosCatalogUseCases headquarterPosCatalogUseCases) {
    this.deviceRepository = deviceRepository;
    this.operatorRepository = operatorRepository;
    this.saleCategories = saleCategories;
    this.posProductManagementUseCases = posProductManagementUseCases;
    this.projector = projector;
    this.productManagementUseCases = productManagementUseCases;
    this.headquarterPosCatalogUseCases = headquarterPosCatalogUseCases;
  }

  @Override
  @Transactional
  public ProductRow renameProduct(UUID deviceId, long itemId, String name, String barcode) {
    long hqId = requireActiveDevice(deviceId).getHeadquarterId();
    HeadquarterItem catalog = headquarterPosCatalogUseCases.get(hqId, itemId);
    Product existing = productManagementUseCases.getById(itemId);
    productManagementUseCases.update(
        itemId,
        new UpdateProductCommand(
            name.strip(),
            existing.getDescription(),
            existing.getUnit(),
            catalogBarcode(barcode, existing.getSku()),
            existing.getStatus(),
            existing.isTrackStock()));
    return project(catalog);
  }

  @Override
  @Transactional
  public ProductRow updateProductOffer(
      UUID deviceId, long itemId, long salePriceCentavos, StockPolicy stockPolicy, String saleCategory) {
    long hqId = requireActiveDevice(deviceId).getHeadquarterId();
    headquarterPosCatalogUseCases.get(hqId, itemId);
    ensureControlledStock(itemId, stockPolicy);
    String category = resolveOfferCategory(hqId, saleCategory);

    HeadquarterItem saved =
        headquarterPosCatalogUseCases.upsert(
            hqId,
            itemId,
            new UpsertHeadquarterItemCommand(
                category, BigDecimal.valueOf(salePriceCentavos, 2), null, stockPolicy, null));
    return project(saved);
  }

  /** Blank keeps the stored category. A name must match an active sale category of the sede. */
  private String resolveOfferCategory(long headquarterId, String saleCategory) {
    if (saleCategory == null || saleCategory.isBlank()) {
      return null;
    }
    return saleCategories
        .findActiveByName(headquarterId, saleCategory.strip())
        .orElseThrow(() -> new PosSaleCategoryNotFoundException(saleCategory.strip()))
        .getName();
  }

  /**
   * A cashier turning stock control on needs a warehouse item. Staff catalog
   * edits still reject a controlled row that has no item.
   */
  private void ensureControlledStock(long productId, StockPolicy stockPolicy) {
    if (stockPolicy != StockPolicy.CONTROLLED) {
      return;
    }

    Product existing = productManagementUseCases.getById(productId);
    if (existing.getInventoryItemId() != null) {
      return;
    }

    productManagementUseCases.update(
        productId,
        new UpdateProductCommand(
            existing.getName(),
            existing.getDescription(),
            existing.getUnit(),
            existing.getBarcode(),
            existing.getStatus(),
            true));
  }

  @Override
  @Transactional
  public ProductRow createProduct(UUID deviceId, DeviceCreatePosProductCommand command) {
    long hqId = requireActiveDevice(deviceId).getHeadquarterId();
    String categoryName = command.saleCategory() == null ? "" : command.saleCategory().strip();
    var category =
        saleCategories
            .findActiveByName(hqId, categoryName)
            .orElseThrow(() -> new PosSaleCategoryNotFoundException(categoryName));
    assertOperatorAssigned(hqId, command.createdByOperatorId());
    BigDecimal salePrice = BigDecimal.valueOf(command.salePriceCentavos(), 2);
    var created =
        posProductManagementUseCases.create(
            hqId,
            new CreatePosProductCommand(
                command.name().strip(),
                null,
                command.barcode(),
                Unit.PIECE,
                salePrice,
                category.getId(),
                true,
                command.stockPolicy() != null ? command.stockPolicy() : StockPolicy.NOT_CONTROLLED,
                null));
    return projector
        .toProductRow(created.catalog())
        .orElseThrow(() -> new IllegalStateException("Created POS product could not be projected"));
  }

  private PosDevice requireActiveDevice(UUID deviceId) {
    PosDevice device =
        deviceRepository.findById(deviceId).orElseThrow(() -> new PosDeviceNotFoundException(deviceId));
    if (device.isRevoked()) {
      throw new PosDeviceRevokedException(deviceId);
    }
    return device;
  }

  private ProductRow project(HeadquarterItem catalog) {
    return projector
        .toProductRow(catalog)
        .orElseThrow(() -> new IllegalStateException("POS product could not be projected"));
  }

  /** Blank or equal to the SKU means "scan the SKU"; the SKU column itself is never rewritten. */
  private static String catalogBarcode(String barcode, String sku) {
    if (barcode == null || barcode.isBlank()) {
      return null;
    }
    String trimmed = barcode.strip();
    if (sku != null && trimmed.equalsIgnoreCase(sku)) {
      return null;
    }
    return trimmed;
  }

  private void assertOperatorAssigned(long headquarterId, Long operatorId) {
    if (operatorId == null) {
      return;
    }
    PosOperator operator = operatorRepository.findById(operatorId).orElse(null);
    if (operator == null || !operator.isActive() || !operator.getHeadquarterIds().contains(headquarterId)) {
      throw new BusinessValidationException(
          "El operador no está asignado a esta sede.",
          Map.of("operatorId", operatorId, "headquarterId", headquarterId),
          "device catalog create operator not assigned to HQ");
    }
  }
}
