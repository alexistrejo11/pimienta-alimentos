package io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web;

import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocClientCreate;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocClientSearch;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocClients;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto.ClientResponse;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto.ClientSearchRequest;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto.CreateClientRequest;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.mapper.ClientWebMapper;
import io.github.alexistrejo11.pimienta.module.crm.core.application.query.ClientSearchCriteria;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.Client;
import io.github.alexistrejo11.pimienta.module.crm.core.port.input.ClientUseCases;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clients")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocClients
public class ClientController {

  private final ClientUseCases clientUseCases;

  public ClientController(ClientUseCases clientUseCases) {
    this.clientUseCases = clientUseCases;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocClientSearch
  public PagedResponse<ClientResponse> searchClients(@ModelAttribute ClientSearchRequest filter) {
    ClientSearchCriteria criteria = ClientWebMapper.toCriteria(filter);
    Page<Client> page =
        clientUseCases.search(criteria, PageRequest.of(filter.getPage(), filter.getSize()));
    return PagedResponse.map(page, ClientWebMapper::toResponse);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocClientCreate
  public ClientResponse createClient(@Valid @RequestBody CreateClientRequest request) {
    Client saved = clientUseCases.create(ClientWebMapper.toCommand(request));
    return ClientWebMapper.toResponse(saved);
  }
}
