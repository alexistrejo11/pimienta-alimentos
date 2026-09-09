package io.github.alexistrejo11.pimienta.module.headquarter.core.port.output;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.PosOperationalConfig;
import java.util.Optional;

public interface PosOperationalConfigRepository {

  Optional<PosOperationalConfig> findByHeadquarterId(long headquarterId);

  PosOperationalConfig save(PosOperationalConfig config);
}
