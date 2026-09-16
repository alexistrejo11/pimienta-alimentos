package io.github.alexistrejo11.pimienta.module.pos.core.port.output;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PosChangeLogRepository {

  PosChangeLogEntry save(PosChangeLogEntry entry);

  Optional<PosChangeLogEntry> findFirstByHeadquarterId(long headquarterId);

  Optional<PosChangeLogEntry> findLastByHeadquarterId(long headquarterId);

  List<PosChangeLogEntry> findAfterSequence(long headquarterId, long sequence, long upperBound, int limit);

  record PosChangeLogEntry(
      Long sequence,
      long headquarterId,
      String entityType,
      String entityId,
      String operation,
      String projectionPayload,
      LocalDateTime createdAt) {}
}
