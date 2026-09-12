package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.pos.core.application.command.AcceptPosSyncIncidentCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncIncident;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosEventResultStatus;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosSyncIncidentNotFoundException;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncIncidentAdminUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosSyncEventRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosSyncIncidentRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PosSyncIncidentAdminUseCasesImpl implements PosSyncIncidentAdminUseCases {

  private final PosSyncIncidentRepository incidentRepository;
  private final PosSyncEventRepository eventRepository;

  public PosSyncIncidentAdminUseCasesImpl(
      PosSyncIncidentRepository incidentRepository, PosSyncEventRepository eventRepository) {
    this.incidentRepository = incidentRepository;
    this.eventRepository = eventRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PosSyncIncident> list(Long headquarterId, Boolean openOnly, Pageable pageable) {
    return incidentRepository.findFiltered(headquarterId, openOnly, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public PosSyncIncident get(UUID incidentId) {
    return incidentRepository
        .findById(incidentId)
        .orElseThrow(() -> new PosSyncIncidentNotFoundException(incidentId));
  }

  @Override
  @Transactional
  public PosSyncIncident accept(AcceptPosSyncIncidentCommand command) {
    PosSyncIncident incident =
        incidentRepository
            .findById(command.incidentId())
            .orElseThrow(() -> new PosSyncIncidentNotFoundException(command.incidentId()));

    incident.accept(command.acceptedByUserId(), command.label(), command.note());
    PosSyncIncident saved = incidentRepository.save(incident);

    eventRepository
        .findByEventId(incident.getEventId())
        .ifPresent(
            event -> {
              if (event.getStatus() == PosEventResultStatus.REQUIRES_REVIEW) {
                event.markAcceptedAfterReview("Accepted by Superadmin after review");
                eventRepository.save(event);
              }
            });

    return saved;
  }
}
