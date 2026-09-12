package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosEnrollmentCode;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosEnrollmentCodeRepository;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.mapper.PosEnrollmentCodePersistenceMapper;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository.PosEnrollmentCodeSpringDataRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PosEnrollmentCodeRepositoryImpl implements PosEnrollmentCodeRepository {

  private final PosEnrollmentCodeSpringDataRepository jpa;

  public PosEnrollmentCodeRepositoryImpl(PosEnrollmentCodeSpringDataRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<PosEnrollmentCode> findByCode(String code) {
    return jpa.findByCodeAndDeletedAtIsNull(code).map(PosEnrollmentCodePersistenceMapper::toDomain);
  }

  @Override
  public PosEnrollmentCode save(PosEnrollmentCode enrollmentCode) {
    return PosEnrollmentCodePersistenceMapper.toDomain(
        jpa.save(PosEnrollmentCodePersistenceMapper.toEntity(enrollmentCode)));
  }
}
