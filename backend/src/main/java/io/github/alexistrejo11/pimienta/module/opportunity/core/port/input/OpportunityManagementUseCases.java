package io.github.alexistrejo11.pimienta.module.opportunity.core.port.input;

import io.github.alexistrejo11.pimienta.module.opportunity.core.application.command.CreateOpportunityCommand;
import io.github.alexistrejo11.pimienta.module.opportunity.core.application.command.UpdateOpportunityCommand;
import io.github.alexistrejo11.pimienta.module.opportunity.core.application.query.OpportunitySearchCriteria;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.Opportunity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OpportunityManagementUseCases {

  Page<Opportunity> search(OpportunitySearchCriteria criteria, Pageable pageable);

  Opportunity getById(long id);

  Opportunity create(CreateOpportunityCommand command);

  Opportunity update(long id, UpdateOpportunityCommand command);

  void delete(long id);

  Opportunity submitForReview(long id);

  Opportunity startPreparing(long id);

  Opportunity markSubmitted(long id);

  Opportunity markWon(long id);

  Opportunity markLost(long id);

  Opportunity abandon(long id);

  Opportunity cancel(long id);
}
