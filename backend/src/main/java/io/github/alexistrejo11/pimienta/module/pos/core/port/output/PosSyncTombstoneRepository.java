package io.github.alexistrejo11.pimienta.module.pos.core.port.output;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncTombstone;
import java.time.LocalDateTime;
import java.util.List;

public interface PosSyncTombstoneRepository {

  PosSyncTombstone save(PosSyncTombstone tombstone);

  List<PosSyncTombstone> findByHeadquarterIdAndCreatedAtAfter(
      long headquarterId, LocalDateTime since);
}
