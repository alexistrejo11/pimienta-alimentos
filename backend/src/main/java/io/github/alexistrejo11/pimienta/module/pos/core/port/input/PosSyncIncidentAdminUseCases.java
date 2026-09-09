package io.github.alexistrejo11.pimienta.module.pos.core.port.input;

import io.github.alexistrejo11.pimienta.module.pos.core.application.command.AcceptPosSyncIncidentCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncIncident;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PosSyncIncidentAdminUseCases {

  Page<PosSyncIncident> list(Long headquarterId, Boolean openOnly, Pageable pageable);

  PosSyncIncident get(UUID incidentId);

  PosSyncIncident accept(AcceptPosSyncIncidentCommand command);
}
