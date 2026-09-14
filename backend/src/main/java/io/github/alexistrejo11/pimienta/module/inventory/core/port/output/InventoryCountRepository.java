package io.github.alexistrejo11.pimienta.module.inventory.core.port.output;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryCountSession;
import java.util.Optional;
public interface InventoryCountRepository { InventoryCountSession save(InventoryCountSession session); Optional<InventoryCountSession> findById(long id); boolean existsActiveByLocationId(long locationId); }
