package io.github.alexistrejo11.pimienta.module.pos.core.port.input;

import io.github.alexistrejo11.pimienta.module.pos.core.application.command.CreateEnrollmentCodeCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosEnrollmentCode;

public interface EnrollmentCodeUseCases {

  PosEnrollmentCode create(CreateEnrollmentCodeCommand command);
}
