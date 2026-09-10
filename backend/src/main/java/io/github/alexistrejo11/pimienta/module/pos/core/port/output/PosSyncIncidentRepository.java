package io.github.alexistrejo11.pimienta.module.pos.core.port.output;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncIncident;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PosSyncIncidentRepository {

  PosSyncIncident save(PosSyncIncident incident);

  Optional<PosSyncIncident> findById(UUID id);

  Page<PosSyncIncident> findFiltered(Long headquarterId, Boolean openOnly, Pageable pageable);

  long countOpenByHeadquarterId(long headquarterId);
}
