package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryTransaction;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.AdjustmentTransactionCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.PhysicalAdjustmentCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.PurchaseTransactionCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.ReturnClientCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.ReturnSupplierCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.SaleTransactionCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.ScrapCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.TransferTransactionCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.InventoryManagementUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.StorageLocationManagementUseCases;
import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.InventoryTransactionManagementUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryTransactionAdjustment;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryTransactionApprove;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryTransactionCancel;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryTransactionComplete;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryTransactionGetById;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryTransactionPhysicalAdjustment;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryTransactionPurchase;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryTransactionReturnClient;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryTransactionReturnSupplier;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryTransactionSale;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryTransactionScrap;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryTransactionSearch;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryTransactionSubmit;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryTransactionTransfer;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryTransactions;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.AdjustmentTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.ApproveTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.InventoryTransactionSearchRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.PhysicalAdjustmentTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.PurchaseTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.ReturnClientTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.ReturnSupplierTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.SaleTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.ScrapTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.TransferTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.InventoryTransactionResponse;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.mapper.InventoryTransactionWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping(BASE + "/inventory/transactions")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocInventoryTransactions
public class InventoryTransactionController {

  private final InventoryTransactionManagementUseCases inventoryTransactionManagementUseCases;
  private final HeadquarterAccessService headquarterAccessService;
  private final StorageLocationManagementUseCases storageLocationManagementUseCases;
  private final InventoryManagementUseCases inventoryManagementUseCases;

  public InventoryTransactionController(
      InventoryTransactionManagementUseCases inventoryTransactionManagementUseCases,
      HeadquarterAccessService headquarterAccessService,
      StorageLocationManagementUseCases storageLocationManagementUseCases,
      InventoryManagementUseCases inventoryManagementUseCases) {
    this.inventoryTransactionManagementUseCases = inventoryTransactionManagementUseCases;
    this.headquarterAccessService = headquarterAccessService;
    this.storageLocationManagementUseCases = storageLocationManagementUseCases;
    this.inventoryManagementUseCases = inventoryManagementUseCases;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryTransactionSearch
  public PagedResponse<InventoryTransactionResponse> searchTransactions(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @ParameterObject @ModelAttribute InventoryTransactionSearchRequest filter) {
    Page<InventoryTransaction> page = inventoryTransactionManagementUseCases.search(
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

  private void requireAccess(JwtAuthenticationContext principal, InventoryTransaction tx) {
    tx.getMovements().forEach(movement -> {
      if (movement.getSourceLocation() != null) {
        headquarterAccessService.requireOwnedLocation(principal, movement.getSourceLocation().getHeadquarterId());
      }
      if (movement.getDestinationLocation() != null) {
        headquarterAccessService.requireOwnedLocation(principal, movement.getDestinationLocation().getHeadquarterId());
      }
    });
  }

  @PostMapping("/purchase")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionPurchase
  public InventoryTransactionResponse purchase(@AuthenticationPrincipal JwtAuthenticationContext principal,
      @Valid @RequestBody PurchaseTransactionRequest request) {
    PurchaseTransactionCommand command = InventoryTransactionWebMapper.toCommand(request);
    requireAccess(principal, command);
    InventoryTransaction tx = inventoryTransactionManagementUseCases
        .purchase(command);
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/sale")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionSale
  public InventoryTransactionResponse sale(@AuthenticationPrincipal JwtAuthenticationContext principal, @Valid @RequestBody SaleTransactionRequest request) {
    SaleTransactionCommand command = InventoryTransactionWebMapper.toCommand(request);
    requireAccess(principal, command);
    InventoryTransaction tx = inventoryTransactionManagementUseCases
        .sale(command);
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/transfer")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionTransfer
  public InventoryTransactionResponse transfer(@AuthenticationPrincipal JwtAuthenticationContext principal, @Valid @RequestBody TransferTransactionRequest request) {
    TransferTransactionCommand command = InventoryTransactionWebMapper.toCommand(request);
    requireAccess(principal, command);
    InventoryTransaction tx = inventoryTransactionManagementUseCases
        .transfer(command);
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/adjustment")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionAdjustment
  public InventoryTransactionResponse adjustment(@AuthenticationPrincipal JwtAuthenticationContext principal, @Valid @RequestBody AdjustmentTransactionRequest request) {
    AdjustmentTransactionCommand command = InventoryTransactionWebMapper.toCommand(request);
    requireAccess(principal, command);
    InventoryTransaction tx = inventoryTransactionManagementUseCases
        .adjustment(command);
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/return-client")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionReturnClient
  public InventoryTransactionResponse returnFromClient(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @Valid @RequestBody ReturnClientTransactionRequest request) {
    ReturnClientCommand command = InventoryTransactionWebMapper.toCommand(request);
    requireAccess(principal, command);
    InventoryTransaction tx = inventoryTransactionManagementUseCases.returnFromClient(
        command);
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/return-supplier")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionReturnSupplier
  public InventoryTransactionResponse returnToSupplier(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @Valid @RequestBody ReturnSupplierTransactionRequest request) {
    ReturnSupplierCommand command = InventoryTransactionWebMapper.toCommand(request);
    requireAccess(principal, command);
    InventoryTransaction tx = inventoryTransactionManagementUseCases.returnToSupplier(
        command);
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/scrap")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionScrap
  public InventoryTransactionResponse scrap(@AuthenticationPrincipal JwtAuthenticationContext principal, @Valid @RequestBody ScrapTransactionRequest request) {
    ScrapCommand command = InventoryTransactionWebMapper.toCommand(request);
    requireAccess(principal, command);
    InventoryTransaction tx = inventoryTransactionManagementUseCases
        .scrap(command);
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/physical-adjustment")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionPhysicalAdjustment
  public InventoryTransactionResponse physicalAdjustment(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @Valid @RequestBody PhysicalAdjustmentTransactionRequest request) {
    PhysicalAdjustmentCommand command = InventoryTransactionWebMapper.toCommand(request);
    requireAccess(principal, command);
    InventoryTransaction tx = inventoryTransactionManagementUseCases.physicalAdjustment(
        command);
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/{id}/submit")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionSubmit
  public InventoryTransactionResponse submit(@AuthenticationPrincipal JwtAuthenticationContext principal, @PathVariable Long id) {
    requireAccess(principal, inventoryTransactionManagementUseCases.getById(id));
    InventoryTransaction tx = inventoryTransactionManagementUseCases.submit(id);
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/{id}/approve")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionApprove
  public InventoryTransactionResponse approve(@AuthenticationPrincipal JwtAuthenticationContext principal,
      @PathVariable Long id, @Valid @RequestBody ApproveTransactionRequest request) {
    requireAccess(principal, inventoryTransactionManagementUseCases.getById(id));
    InventoryTransaction tx = inventoryTransactionManagementUseCases.approve(id, request.approvedById());
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/{id}/complete")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionComplete
  public InventoryTransactionResponse complete(@AuthenticationPrincipal JwtAuthenticationContext principal, @PathVariable Long id) {
    requireAccess(principal, inventoryTransactionManagementUseCases.getById(id));
    InventoryTransaction tx = inventoryTransactionManagementUseCases.complete(id);
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/{id}/cancel")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionCancel
  public ResponseEntity<Void> cancel(@AuthenticationPrincipal JwtAuthenticationContext principal, @PathVariable Long id) {
    InventoryTransaction tx = inventoryTransactionManagementUseCases.getById(id);
    requireAccess(principal, tx);
    inventoryTransactionManagementUseCases.cancel(id);
    return ResponseEntity.noContent().build();
  }

  private void requireAccess(JwtAuthenticationContext principal, PurchaseTransactionCommand command) {
    command.lines().forEach(line -> requireLocationAccess(principal, line.locationId()));
  }

  private void requireAccess(JwtAuthenticationContext principal, SaleTransactionCommand command) {
    command.lines().forEach(line -> requireLocationAccess(principal, line.locationId()));
  }

  private void requireAccess(JwtAuthenticationContext principal, TransferTransactionCommand command) {
    command.lines().forEach(line -> {
      requireLocationAccess(principal, line.fromLocationId());
      requireLocationAccess(principal, line.toLocationId());
    });
  }

  private void requireAccess(JwtAuthenticationContext principal, AdjustmentTransactionCommand command) {
    command.lines().forEach(line -> requireLocationAccess(principal, line.locationId()));
  }

  private void requireAccess(JwtAuthenticationContext principal, ReturnClientCommand command) {
    command.lines().forEach(line -> requireLocationAccess(principal, line.locationId()));
  }

  private void requireAccess(JwtAuthenticationContext principal, ReturnSupplierCommand command) {
    command.lines().forEach(line -> requireLocationAccess(principal, line.locationId()));
  }

  private void requireAccess(JwtAuthenticationContext principal, ScrapCommand command) {
    command.lines().forEach(line -> requireLocationAccess(principal, line.locationId()));
  }

  private void requireAccess(JwtAuthenticationContext principal, PhysicalAdjustmentCommand command) {
    requireLocationAccess(principal,
        inventoryManagementUseCases.getById(command.inventoryId()).getLocation().getId());
  }

  private void requireLocationAccess(JwtAuthenticationContext principal, long locationId) {
    headquarterAccessService.requireOwnedLocation(
        principal, storageLocationManagementUseCases.getById(locationId).getHeadquarterId());
  }

}
