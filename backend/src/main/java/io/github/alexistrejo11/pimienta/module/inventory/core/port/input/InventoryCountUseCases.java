package io.github.alexistrejo11.pimienta.module.inventory.core.port.input;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryCountCommands;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.InventoryCountSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryCountSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryCountUseCases {

  Page<InventoryCountSession> search(InventoryCountSearchCriteria criteria, Pageable pageable);

  InventoryCountSession open(InventoryCountCommands.Open command);

  InventoryCountSession respond(long id, InventoryCountCommands.Response command);

  InventoryCountSession submit(long id, long userId);

  InventoryCountSession approve(long id, long userId);

  void cancel(long id);

  InventoryCountSession get(long id);
}
