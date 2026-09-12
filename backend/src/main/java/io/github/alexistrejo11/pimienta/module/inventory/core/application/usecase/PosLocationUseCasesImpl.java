package io.github.alexistrejo11.pimienta.module.inventory.core.application.usecase;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation.LocationType;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.PosLocationUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.StorageLocationRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PosLocationUseCasesImpl implements PosLocationUseCases {

  private static final Logger log = LoggerFactory.getLogger(PosLocationUseCasesImpl.class);

  private final StorageLocationRepository storageLocationRepository;

  public PosLocationUseCasesImpl(StorageLocationRepository storageLocationRepository) {
    this.storageLocationRepository = storageLocationRepository;
  }

  @Override
  @Transactional
  public StorageLocation ensurePosLocation(long headquarterId) {
    Optional<StorageLocation> existing = storageLocationRepository.findPosByHeadquarterId(headquarterId);
    if (existing.isPresent()) {
      return existing.get();
    }

    String code = StorageLocation.posCodeFor(headquarterId);
    Optional<StorageLocation> byCode = storageLocationRepository.findByCode(code);
    if (byCode.isPresent()) {
      StorageLocation loc = byCode.get();
      if (loc.getType() == LocationType.POS && headquarterId == loc.getHeadquarterId()) {
        return loc;
      }
      if (loc.getType() == LocationType.POS && loc.getHeadquarterId() == null) {
        loc.setHeadquarterId(headquarterId);
        return storageLocationRepository.save(loc);
      }
    }

    log.info("ensure POS location create headquarterId={} code={}", headquarterId, code);
    StorageLocation created =
        StorageLocation.create(
            code,
            "POS headquarter " + headquarterId,
            "Canonical cafeteria POS stock location",
            LocationType.POS,
            null,
            Integer.MAX_VALUE / 4,
            headquarterId);
    return storageLocationRepository.save(created);
  }

  @Override
  public Optional<StorageLocation> findPosLocation(long headquarterId) {
    return storageLocationRepository.findPosByHeadquarterId(headquarterId);
  }
}
