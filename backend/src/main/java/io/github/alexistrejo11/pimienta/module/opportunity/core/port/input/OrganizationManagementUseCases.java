package io.github.alexistrejo11.pimienta.module.opportunity.core.port.input;

import io.github.alexistrejo11.pimienta.module.opportunity.core.application.command.UpsertOrganizationCommand;
import io.github.alexistrejo11.pimienta.module.opportunity.core.application.query.OrganizationSearchCriteria;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrganizationManagementUseCases {

  Page<Organization> search(OrganizationSearchCriteria criteria, Pageable pageable);

  Organization getById(long id);

  Organization create(UpsertOrganizationCommand command);

  Organization update(long id, UpsertOrganizationCommand command);

  void delete(long id);
}
