package io.github.alexistrejo11.pimienta.module.crm.adapter.out.persistence.repository;

import io.github.alexistrejo11.pimienta.module.crm.adapter.out.persistence.model.ClientJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClientJpaRepository
    extends JpaRepository<ClientJpaEntity, Long>, JpaSpecificationExecutor<ClientJpaEntity> {

  Optional<ClientJpaEntity> findByIdAndDeletedAtIsNull(Long id);
}
