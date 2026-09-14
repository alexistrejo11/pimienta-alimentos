package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.repository.jpa;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryCountSession.Status;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.InventoryCountSessionJpaEntity;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InventoryCountSessionJpaRepository
    extends JpaRepository<InventoryCountSessionJpaEntity, Long>,
        JpaSpecificationExecutor<InventoryCountSessionJpaEntity> {

  boolean existsByLocationIdAndStatusIn(Long locationId, Collection<Status> statuses);
}
