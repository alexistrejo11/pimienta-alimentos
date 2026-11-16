package io.github.alexistrejo11.pimienta.module.opportunity.core.application.command;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.ProposalKind;

public record UpsertProposalCommand(ProposalKind kind, String notes) {}
