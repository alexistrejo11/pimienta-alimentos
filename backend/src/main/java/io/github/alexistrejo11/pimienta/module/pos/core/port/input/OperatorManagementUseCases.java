package io.github.alexistrejo11.pimienta.module.pos.core.port.input;

import io.github.alexistrejo11.pimienta.module.pos.core.application.command.CreatePosOperatorCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.UpdatePosOperatorCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosOperator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OperatorManagementUseCases {

  Page<PosOperator> list(Long headquarterId, Pageable pageable);

  PosOperator get(long id);

  PosOperator create(CreatePosOperatorCommand command);

  PosOperator update(long id, UpdatePosOperatorCommand command);

  PosOperator assignHeadquarter(long operatorId, long headquarterId);

  PosOperator unassignHeadquarter(long operatorId, long headquarterId);

  PosOperator softDelete(long operatorId);
}
