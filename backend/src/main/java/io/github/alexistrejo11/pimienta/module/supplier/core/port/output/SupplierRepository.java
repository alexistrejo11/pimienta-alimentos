package io.github.alexistrejo11.pimienta.module.supplier.core.port.output;

import io.github.alexistrejo11.pimienta.module.supplier.core.application.query.SupplierSearchCriteria;
import io.github.alexistrejo11.pimienta.module.supplier.core.domain.Supplier;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierRepository {

  Optional<Supplier> findById(long id);

  Page<Supplier> search(SupplierSearchCriteria criteria, Pageable pageable);

  Supplier save(Supplier supplier);
}
