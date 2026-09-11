package io.github.alexistrejo11.pimienta.module.crm.core.application;

import io.github.alexistrejo11.pimienta.module.crm.core.application.command.CreateClientCommand;
import io.github.alexistrejo11.pimienta.module.crm.core.application.query.ClientSearchCriteria;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.Client;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.exception.ClientNotFoundException;
import io.github.alexistrejo11.pimienta.module.crm.core.port.input.ClientUseCases;
import io.github.alexistrejo11.pimienta.module.crm.core.port.output.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ClientUseCasesImpl implements ClientUseCases {

  private static final Logger log = LoggerFactory.getLogger(ClientUseCasesImpl.class);

  private final ClientRepository clientRepository;

  public ClientUseCasesImpl(ClientRepository clientRepository) {
    this.clientRepository = clientRepository;
  }

  @Override
  public Page<Client> search(ClientSearchCriteria criteria, Pageable pageable) {
    ClientSearchCriteria effective = criteria != null ? criteria : ClientSearchCriteria.empty();
    return clientRepository.search(effective, pageable);
  }

  @Override
  public Client getById(Long id) {
    return clientRepository.findById(id).orElseThrow(() -> new ClientNotFoundException(id));
  }

  @Override
  public Client create(CreateClientCommand command) {
    log.info("create client start name={}", command.name());
    Client saved =
        clientRepository.save(
            Client.builder()
                .withName(command.name())
                .withCompanyName(command.companyName())
                .register());
    log.info("create client complete clientId={}", saved.getId());
    return saved;
  }
}
