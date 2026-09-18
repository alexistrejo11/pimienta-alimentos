package io.github.alexistrejo11.pimienta.module.pos.core.port.input;

import io.github.alexistrejo11.pimienta.module.pos.core.application.command.DeviceCreatePosProductCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncBootstrapUseCases.ProductRow;
import java.util.UUID;

public interface PosDeviceCatalogUseCases {

  ProductRow createProduct(UUID deviceId, DeviceCreatePosProductCommand command);
}
