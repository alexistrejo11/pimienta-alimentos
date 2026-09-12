package io.github.alexistrejo11.pimienta.module.inventory.core.port.input;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation;
import java.util.Optional;

/** Ensures and resolves the canonical POS storage location for a headquarter. */
public interface PosLocationUseCases {

  StorageLocation ensurePosLocation(long headquarterId);

  Optional<StorageLocation> findPosLocation(long headquarterId);
}
