package io.github.alexistrejo11.pimienta.module.employees.adapter.outbound.persistence.repository;

import io.github.alexistrejo11.pimienta.module.employees.adapter.outbound.persistence.EmployeeWorkSchedulePersistenceMapper;
import io.github.alexistrejo11.pimienta.module.employees.adapter.outbound.persistence.model.EmployeeJpaEntity;
import io.github.alexistrejo11.pimienta.module.employees.adapter.outbound.persistence.model.EmployeeWorkScheduleJson;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.exception.EmployeeNotFoundException;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.model.EmployeeWorkSchedule;
import io.github.alexistrejo11.pimienta.module.employees.core.port.output.EmployeeWorkScheduleRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class EmployeeWorkScheduleRepositoryImpl implements EmployeeWorkScheduleRepository {

  private final EmployeeJpaRepository jpaRepository;

  public EmployeeWorkScheduleRepositoryImpl(EmployeeJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<EmployeeWorkSchedule> findByEmployeeId(long employeeId) {
    return jpaRepository
        .findByIdAndDeletedAtIsNull(employeeId)
        .map(this::scheduleFromEntity);
  }

  @Override
  public void replace(long employeeId, EmployeeWorkSchedule schedule) {
    EmployeeJpaEntity entity = jpaRepository
        .findByIdAndDeletedAtIsNull(employeeId)
        .orElseThrow(() -> new EmployeeNotFoundException(employeeId));

    EmployeeWorkScheduleJson json = EmployeeWorkSchedulePersistenceMapper.toJson(schedule);
    entity.setWorkSchedule(json);
    entity.setUpdatedAt(LocalDateTime.now());
    jpaRepository.save(entity);
  }

  private EmployeeWorkSchedule scheduleFromEntity(EmployeeJpaEntity entity) {
    EmployeeWorkScheduleJson json = entity.getWorkSchedule();
    if (json == null) {
      return EmployeeWorkSchedule.empty();
    }
    return EmployeeWorkSchedulePersistenceMapper.toDomain(json);
  }
}
