package io.github.alexistrejo11.pimienta.module.inventory.core.port.input;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryCountCommands;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryCountSession;
public interface InventoryCountUseCases { InventoryCountSession open(InventoryCountCommands.Open c); InventoryCountSession respond(long id,InventoryCountCommands.Response c); InventoryCountSession submit(long id,long userId); InventoryCountSession approve(long id,long userId); void cancel(long id); InventoryCountSession get(long id); }
