package io.github.alexistrejo11.pimienta.module.headquarter.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.CreatePosProductCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import io.github.alexistrejo11.pimienta.module.product.core.domain.Product;

public interface PosProductManagementUseCases {

  record CreatedPosProduct(Product product, HeadquarterItem catalog) {}

  CreatedPosProduct create(long headquarterId, CreatePosProductCommand command);
}
