package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.AdjustmentTransactionCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.InTransactionCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.OutTransactionCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryTransaction;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.InventoryTransactionManagementUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.StorageLocationManagementUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryTransactionAdjustment;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryTransactionGetById;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryTransactionIn;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryTransactionOut;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryTransactionSearch;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryTransactions;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.AdjustmentTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.InTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.InventoryTransactionSearchRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.OutTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.InventoryTransactionResponse;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.mapper.InventoryTransactionWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/inventory/transactions")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocInventoryTransactions
public class InventoryTransactionController {

  private final InventoryTransactionManagementUseCases inventoryTransactionManagementUseCases;
  private final HeadquarterAccessService headquarterAccessService;
  private final StorageLocationManagementUseCases storageLocationManagementUseCases;

  public InventoryTransactionController(
      InventoryTransactionManagementUseCases inventoryTransactionManagementUseCases,
      HeadquarterAccessService headquarterAccessService,
      StorageLocationManagementUseCases storageLocationManagementUseCases) {
    this.inventoryTransactionManagementUseCases = inventoryTransactionManagementUseCases;
    this.headquarterAccessService = headquarterAccessService;
    this.storageLocationManagementUseCases = storageLocationManagementUseCases;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryTransactionSearch
  public PagedResponse<InventoryTransactionResponse> searchTransactions(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @ParameterObject @ModelAttribute InventoryTransactionSearchRequest filter) {
    Page<InventoryTransaction> page =
        inventoryTransactionManagementUseCases.search(
            filter.toCriteria(headquarterAccessService.enforceHeadquarterScope(principal, null)),
            filter.toPageable());
    return PagedResponse.map(page, InventoryTransactionWebMapper::toResponse);
  }

  @GetMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryTransactionGetById
  public InventoryTransactionResponse getTransactionById(
      @AuthenticationPrincipal JwtAuthenticationContext principal, @PathVariable Long id) {
    InventoryTransaction tx = inventoryTransactionManagementUseCases.getById(id);
    requireAccess(principal, tx);
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/in")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionIn
  public InventoryTransactionResponse stockIn(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @Valid @RequestBody InTransactionRequest request) {
    InTransactionCommand command = InventoryTransactionWebMapper.toCommand(request);
    command.lines().forEach(line -> requireLocationAccess(principal, line.locationId()));
    return InventoryTransactionWebMapper.toResponse(inventoryTransactionManagementUseCases.receive(command));
  }

  @PostMapping("/out")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionOut
  public InventoryTransactionResponse stockOut(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @Valid @RequestBody OutTransactionRequest request) {
    OutTransactionCommand command = InventoryTransactionWebMapper.toCommand(request);
    command.lines().forEach(line -> requireLocationAccess(principal, line.locationId()));
    return InventoryTransactionWebMapper.toResponse(inventoryTransactionManagementUseCases.exit(command));
  }

  @PostMapping("/adjustment")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionAdjustment
  public InventoryTransactionResponse adjustment(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @Valid @RequestBody AdjustmentTransactionRequest request) {
    AdjustmentTransactionCommand command = InventoryTransactionWebMapper.toCommand(request);
    command.lines().forEach(line -> requireLocationAccess(principal, line.locationId()));
    return InventoryTransactionWebMapper.toResponse(
        inventoryTransactionManagementUseCases.adjustment(command));
  }

  private void requireAccess(JwtAuthenticationContext principal, InventoryTransaction tx) {
    tx.getMovements()
        .forEach(
            movement -> {
              if (movement.getSourceLocation() != null) {
                headquarterAccessService.requireOwnedLocation(
                    principal, movement.getSourceLocation().getHeadquarterId());
              }
              if (movement.getDestinationLocation() != null) {
                headquarterAccessService.requireOwnedLocation(
                    principal, movement.getDestinationLocation().getHeadquarterId());
              }
            });
  }

  private void requireLocationAccess(JwtAuthenticationContext principal, long locationId) {
    headquarterAccessService.requireOwnedLocation(
        principal, storageLocationManagementUseCases.getById(locationId).getHeadquarterId());
  }
}
