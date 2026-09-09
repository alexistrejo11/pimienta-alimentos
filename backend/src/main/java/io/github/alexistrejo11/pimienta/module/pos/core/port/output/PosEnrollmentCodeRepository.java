package io.github.alexistrejo11.pimienta.module.pos.core.port.output;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosEnrollmentCode;
import java.util.Optional;

public interface PosEnrollmentCodeRepository {

  Optional<PosEnrollmentCode> findByCode(String code);

  PosEnrollmentCode save(PosEnrollmentCode enrollmentCode);
}
