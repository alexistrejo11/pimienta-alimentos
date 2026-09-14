package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.repository.jpa;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.InventoryCountSessionJpaEntity;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryCountSession.Status; import org.springframework.data.jpa.repository.JpaRepository;
public interface InventoryCountSessionJpaRepository extends JpaRepository<InventoryCountSessionJpaEntity,Long> { boolean existsByLocationIdAndStatusIn(Long locationId, java.util.Collection<Status> statuses); }
