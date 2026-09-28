package io.github.alexistrejo11.pimienta.module.inventory.core.port.input;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.UpdateItemIdentityCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.UpdateItemStockSettingsCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.ItemSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemManagementUseCases {

  Page<Item> search(ItemSearchCriteria criteria, Pageable pageable);

  Item getById(Long id);

  Item create(Item item);

  Item updateIdentity(Long id, UpdateItemIdentityCommand command);

  Item updateStockSettings(Long id, UpdateItemStockSettingsCommand command);

  Item discontinue(Long id);

  Item activate(Long id);

  void delete(Long id);
}
