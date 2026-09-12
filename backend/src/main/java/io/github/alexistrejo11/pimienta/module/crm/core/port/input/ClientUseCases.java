package io.github.alexistrejo11.pimienta.module.crm.core.port.input;

import io.github.alexistrejo11.pimienta.module.crm.core.application.command.CreateClientCommand;
import io.github.alexistrejo11.pimienta.module.crm.core.application.query.ClientSearchCriteria;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClientUseCases {

  Page<Client> search(ClientSearchCriteria criteria, Pageable pageable);

  Client getById(Long id);

  Client create(CreateClientCommand command);
}
