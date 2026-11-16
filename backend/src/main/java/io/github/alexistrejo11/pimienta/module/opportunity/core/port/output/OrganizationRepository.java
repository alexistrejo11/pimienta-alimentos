package io.github.alexistrejo11.pimienta.module.opportunity.core.port.output;

import io.github.alexistrejo11.pimienta.module.opportunity.core.application.query.OrganizationSearchCriteria;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.Organization;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrganizationRepository {

  Optional<Organization> findById(long id);

  Page<Organization> search(OrganizationSearchCriteria criteria, Pageable pageable);

  Organization save(Organization organization);
}
