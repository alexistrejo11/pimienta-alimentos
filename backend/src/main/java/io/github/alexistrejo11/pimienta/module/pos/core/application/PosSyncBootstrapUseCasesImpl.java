package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.Headquarter;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.PosOperationalConfig;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.HeadquarterNotFoundException;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterItemRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.PosOperationalConfigRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosDevice;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosDeviceNotFoundException;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosDeviceRevokedException;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncBootstrapUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosDeviceRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosOperatorRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosChangeLogRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

@Service
public class PosSyncBootstrapUseCasesImpl implements PosSyncBootstrapUseCases {

  private static final int SCHEMA_VERSION = 1;
  private static final String KIND = "pos-bootstrap";

  private final PosDeviceRepository deviceRepository;
  private final HeadquarterRepository headquarterRepository;
  private final PosOperationalConfigRepository posOperationalConfigRepository;
  private final PosOperatorRepository operatorRepository;
  private final HeadquarterItemRepository headquarterItemRepository;
  private final PosSyncCatalogProjector projector;
  private final PosChangeLogRepository changeLogRepository;

  public PosSyncBootstrapUseCasesImpl(
      PosDeviceRepository deviceRepository,
      HeadquarterRepository headquarterRepository,
      PosOperationalConfigRepository posOperationalConfigRepository,
      PosOperatorRepository operatorRepository,
      HeadquarterItemRepository headquarterItemRepository,
      PosSyncCatalogProjector projector,
      PosChangeLogRepository changeLogRepository) {
    this.deviceRepository = deviceRepository;
    this.headquarterRepository = headquarterRepository;
    this.posOperationalConfigRepository = posOperationalConfigRepository;
    this.operatorRepository = operatorRepository;
    this.headquarterItemRepository = headquarterItemRepository;
    this.projector = projector;
    this.changeLogRepository = changeLogRepository;
  }

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public BootstrapSnapshot bootstrap(UUID deviceId) {
    PosDevice device =
        deviceRepository.findById(deviceId).orElseThrow(() -> new PosDeviceNotFoundException(deviceId));
    if (device.isRevoked()) {
      throw new PosDeviceRevokedException(deviceId);
    }

    long hqId = device.getHeadquarterId();
    long watermark =
        changeLogRepository.findLastByHeadquarterId(hqId).map(e -> e.sequence()).orElse(0L);
    Headquarter hq =
        headquarterRepository
            .findById(hqId)
            .orElseThrow(() -> new HeadquarterNotFoundException(hqId));

    PosOperationalConfig config =
        posOperationalConfigRepository.findByHeadquarterId(hqId).orElse(null);

    String currency =
        config != null && config.getCurrency() != null && !config.getCurrency().isBlank()
            ? config.getCurrency()
            : "MXN";
    List<String> openAmountCategories =
        config != null ? config.getOpenAmountCategories() : List.of();

    List<OperatorRow> operators =
        operatorRepository.findByHeadquarterId(hqId).stream()
            .map(projector::toOperatorRow)
            .toList();

    List<ProductRow> products = new ArrayList<>();
    for (HeadquarterItem row : headquarterItemRepository.findAllByHeadquarterId(hqId)) {
      projector.toProductRow(row).ifPresent(products::add);
    }

    return new BootstrapSnapshot(
        SCHEMA_VERSION,
        KIND,
        UUID.randomUUID(),
        Instant.now(),
        new SiteRow(String.valueOf(hq.getId()), hq.getName(), hq.getAddress(), currency),
        new DeviceRow(
            device.getId().toString(),
            device.getDeviceName(),
            device.getVisibleCode(),
            device.getStatus()),
        operators,
        List.copyOf(products),
        openAmountCategories,
        projector.toPolicies(config),
        new Cursors(PosSyncCursor.of(hqId, watermark).format()));
  }
}
