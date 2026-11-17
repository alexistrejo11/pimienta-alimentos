package io.github.alexistrejo11.pimienta.module.opportunity.core.application;

import io.github.alexistrejo11.pimienta.module.opportunity.core.application.command.UpsertOrganizationCommand;
import io.github.alexistrejo11.pimienta.module.opportunity.core.application.query.OrganizationSearchCriteria;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.Organization;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.OrganizationKind;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.exception.OpportunityRuleViolationException;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.exception.OrganizationNotFoundException;
import io.github.alexistrejo11.pimienta.module.opportunity.core.port.input.OrganizationManagementUseCases;
import io.github.alexistrejo11.pimienta.module.opportunity.core.port.output.OrganizationRepository;
import io.github.alexistrejo11.pimienta.module.opportunity.core.port.output.OpportunityRepository;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationManagementUseCasesImpl implements OrganizationManagementUseCases {

  private static final Logger log = LoggerFactory.getLogger(OrganizationManagementUseCasesImpl.class);

  private final OrganizationRepository organizationRepository;
  private final OpportunityRepository opportunityRepository;

  public OrganizationManagementUseCasesImpl(
      OrganizationRepository organizationRepository, OpportunityRepository opportunityRepository) {
    this.organizationRepository = organizationRepository;
    this.opportunityRepository = opportunityRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Organization> search(OrganizationSearchCriteria criteria, Pageable pageable) {
    return organizationRepository.search(
        criteria != null ? criteria : OrganizationSearchCriteria.empty(), pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Organization getById(long id) {
    return organizationRepository.findById(id).orElseThrow(() -> new OrganizationNotFoundException(id));
  }

  @Override
  @Transactional
  public Organization create(UpsertOrganizationCommand command) {
    log.info("create organization start name={}", command.name());
    OrganizationKind kind = command.kind() != null ? command.kind() : OrganizationKind.OTHER;
    Organization saved =
        organizationRepository.save(
            Organization.builder()
                .withName(command.name())
                .withKind(kind)
                .withNotes(command.notes())
                .withContactName(command.contactName())
                .withContactEmail(command.contactEmail())
                .withContactPhone(command.contactPhone())
                .register());
    log.info("create organization complete organizationId={}", saved.getId());
    return saved;
  }

  @Override
  @Transactional
  public Organization update(long id, UpsertOrganizationCommand command) {
    Organization existing = getById(id);
    OrganizationKind kind = command.kind() != null ? command.kind() : OrganizationKind.OTHER;
    existing.applyProfile(
        command.name(),
        kind,
        command.notes(),
        command.contactName(),
        command.contactEmail(),
        command.contactPhone());
    return organizationRepository.save(existing);
  }

  @Override
  @Transactional
  public void delete(long id) {
    Organization existing = getById(id);
    if (opportunityRepository.existsByOrganizationId(id)) {
      throw new OpportunityRuleViolationException(
          "Cannot delete an organization that still has opportunities.",
          Map.of("organizationId", id),
          "delete organization blocked by opportunities id=" + id);
    }
    existing.delete();
    organizationRepository.save(existing);
  }
}
