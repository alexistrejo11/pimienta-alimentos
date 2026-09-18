package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.HeadquarterNotFoundException;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.CreateEnrollmentCodeCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosEnrollmentCode;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.EnrollmentCodeUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosEnrollmentCodeRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentCodeUseCasesImpl implements EnrollmentCodeUseCases {

  private static final int TTL_MINUTES = 15;
  private static final int CODE_BOUND = 1_000_000;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final PosEnrollmentCodeRepository enrollmentCodeRepository;
  private final HeadquarterRepository headquarterRepository;

  public EnrollmentCodeUseCasesImpl(
      PosEnrollmentCodeRepository enrollmentCodeRepository,
      HeadquarterRepository headquarterRepository) {
    this.enrollmentCodeRepository = enrollmentCodeRepository;
    this.headquarterRepository = headquarterRepository;
  }

  @Override
  @Transactional
  public PosEnrollmentCode create(CreateEnrollmentCodeCommand command) {
    headquarterRepository
        .findById(command.headquarterId())
        .orElseThrow(() -> new HeadquarterNotFoundException(command.headquarterId()));

    LocalDateTime now = LocalDateTime.now();
    PosEnrollmentCode code =
        PosEnrollmentCode.builder()
            .withCode(generateCode())
            .withHeadquarterId(command.headquarterId())
            .withExpiresAt(now.plusMinutes(TTL_MINUTES))
            .register();
    return enrollmentCodeRepository.save(code);
  }

  private static String generateCode() {
    return "%06d".formatted(RANDOM.nextInt(CODE_BOUND));
  }
}
