package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.application.PosProductManagementUseCases;
import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.CreatePosProductCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.PosSaleCategoryNotFoundException;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.PosSaleCategoryRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemUnit;
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

  public PosDeviceCatalogUseCasesImpl(
      PosDeviceRepository deviceRepository,
      PosOperatorRepository operatorRepository,
      PosSaleCategoryRepository saleCategories,
      PosProductManagementUseCases posProductManagementUseCases,
      PosSyncCatalogProjector projector) {
    this.deviceRepository = deviceRepository;
    this.operatorRepository = operatorRepository;
    this.saleCategories = saleCategories;
    this.posProductManagementUseCases = posProductManagementUseCases;
    this.projector = projector;
  }

  @Override
  @Transactional
  public ProductRow createProduct(UUID deviceId, DeviceCreatePosProductCommand command) {
    PosDevice device =
        deviceRepository.findById(deviceId).orElseThrow(() -> new PosDeviceNotFoundException(deviceId));
    if (device.isRevoked()) {
      throw new PosDeviceRevokedException(deviceId);
    }
    long hqId = device.getHeadquarterId();
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
                BigDecimal.ZERO,
                salePrice,
                ItemCategory.FINISHED_GOOD,
                ItemUnit.PIECE,
                null,
                command.barcode(),
                0,
                0,
                category.getId(),
                true,
                command.stockPolicy() != null ? command.stockPolicy() : StockPolicy.NOT_CONTROLLED,
                null));
    return projector
        .toProductRow(created.catalog())
        .orElseThrow(() -> new IllegalStateException("Created POS product could not be projected"));
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
