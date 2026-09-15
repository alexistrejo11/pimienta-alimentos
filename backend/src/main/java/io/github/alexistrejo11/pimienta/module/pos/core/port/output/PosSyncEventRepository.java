package io.github.alexistrejo11.pimienta.module.pos.core.port.output;

import io.github.alexistrejo11.pimienta.module.pos.core.application.query.PosReportFilterQuery;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncEvent;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosEventResultStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PosSyncEventRepository {

  Optional<PosSyncEvent> findByEventId(UUID eventId);

  PosSyncEvent save(PosSyncEvent event);

  Page<PosSyncEvent> findAcceptedByEventTypes(
      PosReportFilterQuery filter, Collection<String> eventTypes, Pageable pageable);

  long countAcceptedByEventType(long headquarterId, Instant from, Instant to, String eventType);

  Instant findLastAcceptedEventAt(long headquarterId, Instant from, Instant to, String eventType);

  Map<UUID, PosEventResultStatus> findSyncStatusesByEventIds(Collection<UUID> eventIds);
}
