package io.github.alexistrejo11.pimienta.module.inventory.core.port.output;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.StorageLocationSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StorageLocationRepository {

  Optional<StorageLocation> findById(long id);

  Optional<StorageLocation> findByIdForUpdate(long id);

  List<StorageLocation> findAllNonDeleted();

  List<StorageLocation> findByParentId(Long parentId);

  Optional<StorageLocation> findPosByHeadquarterId(long headquarterId);

  Optional<StorageLocation> findByCode(String code);

  Page<StorageLocation> search(StorageLocationSearchCriteria criteria, Pageable pageable);

  long countInventoryRowsByLocationId(long locationId);

  StorageLocation save(StorageLocation location);
}
