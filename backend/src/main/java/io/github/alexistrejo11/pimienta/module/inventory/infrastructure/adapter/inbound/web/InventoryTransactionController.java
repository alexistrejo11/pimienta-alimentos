package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryTransaction;
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

@RestController
@RequestMapping(BASE + "/inventory/transactions")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocInventoryTransactions
public class InventoryTransactionController {

  private final InventoryTransactionManagementUseCases inventoryTransactionManagementUseCases;

  public InventoryTransactionController(
      InventoryTransactionManagementUseCases inventoryTransactionManagementUseCases) {
    this.inventoryTransactionManagementUseCases = inventoryTransactionManagementUseCases;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryTransactionSearch
  public PagedResponse<InventoryTransactionResponse> searchTransactions(
      @ParameterObject @ModelAttribute InventoryTransactionSearchRequest filter) {
    Page<InventoryTransaction> page = inventoryTransactionManagementUseCases.search(filter.toCriteria(),
        filter.toPageable());
    return PagedResponse.map(page, InventoryTransactionWebMapper::toResponse);
  }

  @GetMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryTransactionGetById
  public InventoryTransactionResponse getTransactionById(@PathVariable Long id) {
    InventoryTransaction tx = inventoryTransactionManagementUseCases.getById(id);
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/purchase")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionPurchase
  public InventoryTransactionResponse purchase(@Valid @RequestBody PurchaseTransactionRequest request) {
    InventoryTransaction tx = inventoryTransactionManagementUseCases
        .purchase(InventoryTransactionWebMapper.toCommand(request));
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/sale")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionSale
  public InventoryTransactionResponse sale(@Valid @RequestBody SaleTransactionRequest request) {
    InventoryTransaction tx = inventoryTransactionManagementUseCases
        .sale(InventoryTransactionWebMapper.toCommand(request));
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/transfer")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionTransfer
  public InventoryTransactionResponse transfer(@Valid @RequestBody TransferTransactionRequest request) {
    InventoryTransaction tx = inventoryTransactionManagementUseCases
        .transfer(InventoryTransactionWebMapper.toCommand(request));
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/adjustment")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionAdjustment
  public InventoryTransactionResponse adjustment(@Valid @RequestBody AdjustmentTransactionRequest request) {
    InventoryTransaction tx = inventoryTransactionManagementUseCases
        .adjustment(InventoryTransactionWebMapper.toCommand(request));
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/return-client")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionReturnClient
  public InventoryTransactionResponse returnFromClient(
      @Valid @RequestBody ReturnClientTransactionRequest request) {
    InventoryTransaction tx = inventoryTransactionManagementUseCases.returnFromClient(
        InventoryTransactionWebMapper.toCommand(request));
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/return-supplier")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionReturnSupplier
  public InventoryTransactionResponse returnToSupplier(
      @Valid @RequestBody ReturnSupplierTransactionRequest request) {
    InventoryTransaction tx = inventoryTransactionManagementUseCases.returnToSupplier(
        InventoryTransactionWebMapper.toCommand(request));
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/scrap")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionScrap
  public InventoryTransactionResponse scrap(@Valid @RequestBody ScrapTransactionRequest request) {
    InventoryTransaction tx = inventoryTransactionManagementUseCases
        .scrap(InventoryTransactionWebMapper.toCommand(request));
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/physical-adjustment")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionPhysicalAdjustment
  public InventoryTransactionResponse physicalAdjustment(
      @Valid @RequestBody PhysicalAdjustmentTransactionRequest request) {
    InventoryTransaction tx = inventoryTransactionManagementUseCases.physicalAdjustment(
        InventoryTransactionWebMapper.toCommand(request));
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/{id}/submit")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionSubmit
  public InventoryTransactionResponse submit(@PathVariable Long id) {
    InventoryTransaction tx = inventoryTransactionManagementUseCases.submit(id);
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/{id}/approve")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionApprove
  public InventoryTransactionResponse approve(
      @PathVariable Long id, @Valid @RequestBody ApproveTransactionRequest request) {
    InventoryTransaction tx = inventoryTransactionManagementUseCases.approve(id, request.approvedById());
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/{id}/complete")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionComplete
  public InventoryTransactionResponse complete(@PathVariable Long id) {
    InventoryTransaction tx = inventoryTransactionManagementUseCases.complete(id);
    return InventoryTransactionWebMapper.toResponse(tx);
  }

  @PostMapping("/{id}/cancel")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocInventoryTransactionCancel
  public ResponseEntity<Void> cancel(@PathVariable Long id) {
    inventoryTransactionManagementUseCases.cancel(id);
    return ResponseEntity.noContent().build();
  }
}
