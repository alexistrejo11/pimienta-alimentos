package io.github.alexistrejo11.pimienta.module.pos.core.port.output;

import io.github.alexistrejo11.pimienta.module.pos.core.application.query.PosReportFilterQuery;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncEvent;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PosSyncEventRepository {

  Optional<PosSyncEvent> findByEventId(UUID eventId);

  PosSyncEvent save(PosSyncEvent event);

  Page<PosSyncEvent> findAcceptedByEventTypes(
      PosReportFilterQuery filter, Collection<String> eventTypes, Pageable pageable);
}
