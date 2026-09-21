package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.repository.jpa;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryCountSession.Status;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.InventoryCountSessionJpaEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryCountSessionJpaRepository
    extends JpaRepository<InventoryCountSessionJpaEntity, Long>,
        JpaSpecificationExecutor<InventoryCountSessionJpaEntity> {

  boolean existsByLocationIdAndStatusIn(Long locationId, Collection<Status> statuses);

  long countByStatusIn(Collection<Status> statuses);

  @Query(
      """
      select count(c) from InventoryCountSessionJpaEntity c, StorageLocationJpaEntity l
      where c.locationId = l.id and l.deletedAt is null
        and c.status in :statuses and l.headquarterId in :headquarters
      """)
  long countOpenInHeadquarters(
      @Param("headquarters") List<Long> headquarters, @Param("statuses") Collection<Status> statuses);
}
