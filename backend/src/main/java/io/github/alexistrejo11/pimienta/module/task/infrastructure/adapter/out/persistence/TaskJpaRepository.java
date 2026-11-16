package io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskJpaRepository
    extends JpaRepository<TaskJpaEntity, Long>, JpaSpecificationExecutor<TaskJpaEntity> {

  Optional<TaskJpaEntity> findByIdAndDeletedAtIsNull(Long id);
}
