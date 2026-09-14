package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.repository.jpa;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.InventoryCountResponseJpaEntity; import java.util.List; import org.springframework.data.jpa.repository.JpaRepository;
public interface InventoryCountResponseJpaRepository extends JpaRepository<InventoryCountResponseJpaEntity,Long> { List<InventoryCountResponseJpaEntity> findBySessionIdOrderByIdAsc(Long sessionId); }
