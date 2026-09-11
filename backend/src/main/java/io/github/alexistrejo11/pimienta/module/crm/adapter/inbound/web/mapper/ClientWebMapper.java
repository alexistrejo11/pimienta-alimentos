package io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto.ClientResponse;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto.ClientSearchRequest;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto.CreateClientRequest;
import io.github.alexistrejo11.pimienta.module.crm.core.application.command.CreateClientCommand;
import io.github.alexistrejo11.pimienta.module.crm.core.application.query.ClientSearchCriteria;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.Client;

public final class ClientWebMapper {

  private ClientWebMapper() {}

  public static ClientSearchCriteria toCriteria(ClientSearchRequest request) {
    if (request == null) {
      return ClientSearchCriteria.empty();
    }
    return new ClientSearchCriteria(request.getNameContains());
  }

  public static CreateClientCommand toCommand(CreateClientRequest request) {
    return new CreateClientCommand(request.name(), request.companyName());
  }

  public static ClientResponse toResponse(Client client) {
    return new ClientResponse(
        client.getId(),
        client.getName(),
        client.getCompanyName(),
        client.getCreatedAt(),
        client.getUpdatedAt());
  }
}
