package io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.doc.DocContracts;
import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.doc.DocContractsCreate;
import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.doc.DocContractsDelete;
import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.doc.DocContractsExtend;
import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.doc.DocContractsGetById;
import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.doc.DocContractsList;
import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.doc.DocContractsRenew;
import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.doc.DocContractsUpdate;
import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.dto.request.CreateContractRequest;
import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.dto.request.ExtendContractRequest;
import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.dto.request.UpdateContractRequest;
import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.dto.response.ContractResponse;
import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.mapper.ContractManagerWebMapper;
import io.github.alexistrejo11.pimienta.module.contract.core.application.command.CreateContractCommand;
import io.github.alexistrejo11.pimienta.module.contract.core.application.command.UpdateContractCommand;
import io.github.alexistrejo11.pimienta.module.contract.core.domain.Contract;
import io.github.alexistrejo11.pimienta.module.contract.core.port.ContractManagementUseCases;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/contracts")
@DocContracts
@RateLimit(profile = RateLimitProfile.STANDARD)
public class ContractManagerController {

  private final ContractManagementUseCases contractManagementUseCases;

  public ContractManagerController(ContractManagementUseCases contractManagementUseCases) {
    this.contractManagementUseCases = contractManagementUseCases;
  }

  @GetMapping
  @DocContractsList
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  public PagedResponse<ContractResponse> listContracts(@ModelAttribute PageableRequest pageable) {
    Page<Contract> page = contractManagementUseCases.getBy(pageable.toPageable());
    return PagedResponse.map(page, ContractManagerWebMapper::toResponse);
  }

  @GetMapping("/{id}")
  @DocContractsGetById
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  public ContractResponse getContract(@PathVariable Long id) {
    Contract contract = contractManagementUseCases.getById(id);
    return ContractManagerWebMapper.toResponse(contract);
  }

  @PostMapping
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocContractsCreate
  @ResponseStatus(HttpStatus.CREATED)
  public ContractResponse createContract(@Valid @RequestBody CreateContractRequest request) {
    CreateContractCommand command = ContractManagerWebMapper.toCreateCommand(request);
    Contract created = contractManagementUseCases.create(command);
    return ContractManagerWebMapper.toResponse(created);
  }

  @PutMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocContractsUpdate
  public ContractResponse updateContract(@PathVariable Long id, @Valid @RequestBody UpdateContractRequest request) {
    UpdateContractCommand command = ContractManagerWebMapper.toUpdateCommand(request);
    Contract updated = contractManagementUseCases.update(id, command);
    return ContractManagerWebMapper.toResponse(updated);
  }

  @DeleteMapping("/{id}")
  @DocContractsDelete
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  public ResponseEntity<Void> deleteContract(@PathVariable Long id) {
    contractManagementUseCases.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/renew")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocContractsRenew
  public ContractResponse renewContract(@PathVariable Long id) {
    Contract contract = contractManagementUseCases.renew(id);
    return ContractManagerWebMapper.toResponse(contract);
  }

  @PutMapping("/{id}/extend")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocContractsExtend
  public ContractResponse extendContract(@PathVariable Long id, @Valid @RequestBody ExtendContractRequest request) {
    Contract contract = contractManagementUseCases.extend(id, request.newEnd());
    return ContractManagerWebMapper.toResponse(contract);
  }
}
