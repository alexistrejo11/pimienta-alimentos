package io.github.alexistrejo11.pimienta.module.crm.core.port.output;

import io.github.alexistrejo11.pimienta.module.crm.core.application.query.ClientSearchCriteria;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.Client;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClientRepository {

  Optional<Client> findById(long id);

  Page<Client> search(ClientSearchCriteria criteria, Pageable pageable);

  Client save(Client client);
}
