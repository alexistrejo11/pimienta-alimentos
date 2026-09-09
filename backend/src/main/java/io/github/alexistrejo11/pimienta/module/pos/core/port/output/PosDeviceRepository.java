package io.github.alexistrejo11.pimienta.module.pos.core.port.output;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosDevice;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosDeviceStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PosDeviceRepository {

  Optional<PosDevice> findById(UUID id);

  Page<PosDevice> findAll(Pageable pageable);

  Page<PosDevice> findByHeadquarterId(long headquarterId, Pageable pageable);

  long countByHeadquarterIdAndStatusNot(long headquarterId, PosDeviceStatus status);

  PosDevice save(PosDevice device);
}
