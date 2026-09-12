package io.github.alexistrejo11.pimienta.module.headquarter.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.CreatePosProductCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;

public interface PosProductManagementUseCases {
  record CreatedPosProduct(io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item item, HeadquarterItem catalog) {}
  CreatedPosProduct create(long headquarterId, CreatePosProductCommand command);
}
