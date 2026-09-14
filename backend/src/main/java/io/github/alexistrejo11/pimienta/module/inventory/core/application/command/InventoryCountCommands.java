package io.github.alexistrejo11.pimienta.module.inventory.core.application.command;
import java.util.List;
public final class InventoryCountCommands { private InventoryCountCommands() {} public record Open(long locationId,String type,List<Long> itemIds,long createdById) {} public record Response(long itemId,int countedQuantity) {} }
