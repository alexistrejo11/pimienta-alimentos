package io.github.alexistrejo11.pimienta.module.opportunity.core.port.input;

import io.github.alexistrejo11.pimienta.module.opportunity.core.application.command.CreateRequirementCommand;
import io.github.alexistrejo11.pimienta.module.opportunity.core.application.command.UpdateRequirementCommand;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.OpportunityRequirement;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.RequirementStatus;
import java.util.List;

public interface OpportunityRequirementUseCases {

  List<OpportunityRequirement> listByOpportunity(long opportunityId);

  OpportunityRequirement getById(long opportunityId, long requirementId);

  OpportunityRequirement create(long opportunityId, CreateRequirementCommand command);

  OpportunityRequirement update(
      long opportunityId, long requirementId, UpdateRequirementCommand command);

  OpportunityRequirement changeStatus(
      long opportunityId, long requirementId, RequirementStatus status);

  void delete(long opportunityId, long requirementId);

  long countBlockingMandatory(long opportunityId);
}
