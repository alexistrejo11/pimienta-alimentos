package io.github.alexistrejo11.pimienta.module.supplier.core.port.input;

import io.github.alexistrejo11.pimienta.module.supplier.core.application.command.UpsertSupplierCommand;
import io.github.alexistrejo11.pimienta.module.supplier.core.application.query.SupplierSearchCriteria;
import io.github.alexistrejo11.pimienta.module.supplier.core.domain.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierUseCases {

  Page<Supplier> search(SupplierSearchCriteria criteria, Pageable pageable);

  Supplier getById(long id);

  Supplier create(UpsertSupplierCommand command);

  Supplier update(long id, UpsertSupplierCommand command);

  void delete(long id);
}
