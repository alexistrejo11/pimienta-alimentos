package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryGlobalSummary;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.InventorySearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.InventoryManagementUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.StorageLocationManagementUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryStock;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryStockByItem;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryStockByLocation;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryStockCreateInitial;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryStockGetById;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryStockLow;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryStockOut;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryStockSearch;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.CreateInitialStockRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.InventoryStockSearchRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.InventoryStockResponse;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.GlobalInventoryResponse;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.GlobalInventorySearchRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.mapper.InventoryStockWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/inventory/stock")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocInventoryStock
public class InventoryStockController {

  private final InventoryManagementUseCases inventoryManagementUseCases;
  private final HeadquarterAccessService headquarterAccessService;
  private final StorageLocationManagementUseCases storageLocationManagementUseCases;

  public InventoryStockController(
      InventoryManagementUseCases inventoryManagementUseCases,
      HeadquarterAccessService headquarterAccessService,
      StorageLocationManagementUseCases storageLocationManagementUseCases) {
    this.inventoryManagementUseCases = inventoryManagementUseCases;
    this.headquarterAccessService = headquarterAccessService;
    this.storageLocationManagementUseCases = storageLocationManagementUseCases;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryStockSearch
  public PagedResponse<InventoryStockResponse> searchStock(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @ParameterObject @ModelAttribute InventoryStockSearchRequest filter) {
    var hq = headquarterAccessService.enforceHeadquarterScope(principal, filter.getHeadquarterId());
    Page<Inventory> page =
        inventoryManagementUseCases.search(filter.toCriteria(hq), filter.toPageable());
    return PagedResponse.map(page, InventoryStockWebMapper::toResponse);
  }

  @GetMapping("/summary")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryStockSearch
  public PagedResponse<GlobalInventoryResponse> searchGlobalSummary(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @ParameterObject @ModelAttribute GlobalInventorySearchRequest filter) {
    var scope = headquarterAccessService.enforceHeadquarterScope(principal, filter.getHeadquarterId());
    var pageable = filter.toPageable();
    Page<InventoryGlobalSummary> page = scope != null && scope.isEmpty()
        ? Page.empty(pageable)
        : inventoryManagementUseCases.searchGlobalSummary(
            filter.getSearch(), filter.getCategory(), filter.getStatus(), filter.getMinCost(), filter.getMaxCost(), scope, pageable);
    return PagedResponse.map(page, row -> new GlobalInventoryResponse(row.itemId(), row.sku(), row.name(), row.category(), row.headquarterId(), row.headquarterName(), row.availableQuantity(), row.reservedQuantity(), row.inTransitQuantity(), row.totalQuantity(), row.unitCost(), row.totalValue(), row.status()));
  }

  @GetMapping("/low-stock")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryStockLow
  public PagedResponse<InventoryStockResponse> findLowStock(@AuthenticationPrincipal JwtAuthenticationContext principal, @ParameterObject @ModelAttribute PageableRequest pageable) {
    Page<Inventory> page = inventoryManagementUseCases.findLowStock(new InventorySearchCriteria(null, null, null, headquarterAccessService.enforceHeadquarterScope(principal, null)), pageable.toPageable());
    return PagedResponse.map(page, InventoryStockWebMapper::toResponse);
  }

  @GetMapping("/out-of-stock")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryStockOut
  public PagedResponse<InventoryStockResponse> findOutOfStock(@AuthenticationPrincipal JwtAuthenticationContext principal, @ParameterObject @ModelAttribute PageableRequest pageable) {
    Page<Inventory> page = inventoryManagementUseCases.findOutOfStock(new InventorySearchCriteria(null, null, null, headquarterAccessService.enforceHeadquarterScope(principal, null)), pageable.toPageable());
    return PagedResponse.map(page, InventoryStockWebMapper::toResponse);
  }

  @GetMapping("/item/{itemId}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryStockByItem
  public List<InventoryStockResponse> listByItem(
      @AuthenticationPrincipal JwtAuthenticationContext principal, @PathVariable Long itemId) {
    List<Inventory> rows = inventoryManagementUseCases.findByItemId(itemId);
    rows.forEach(row -> headquarterAccessService.requireOwnedLocation(principal, row.getLocation().getHeadquarterId()));
    return rows.stream().map(InventoryStockWebMapper::toResponse).toList();
  }

  @GetMapping("/location/{locationId}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryStockByLocation
  public List<InventoryStockResponse> listByLocation(
      @AuthenticationPrincipal JwtAuthenticationContext principal, @PathVariable Long locationId) {
    List<Inventory> rows = inventoryManagementUseCases.findByLocationId(locationId);
    rows.forEach(row -> headquarterAccessService.requireOwnedLocation(principal, row.getLocation().getHeadquarterId()));
    return rows.stream().map(InventoryStockWebMapper::toResponse).toList();
  }

  @GetMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryStockGetById
  public InventoryStockResponse getStockById(
      @AuthenticationPrincipal JwtAuthenticationContext principal, @PathVariable Long id) {
    Inventory inv = inventoryManagementUseCases.getById(id);
    headquarterAccessService.requireOwnedLocation(principal, inv.getLocation().getHeadquarterId());
    return InventoryStockWebMapper.toResponse(inv);
  }

  @PostMapping
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @ResponseStatus(HttpStatus.CREATED)
  @DocInventoryStockCreateInitial
  public InventoryStockResponse createInitialStock(@AuthenticationPrincipal JwtAuthenticationContext principal, @Valid @RequestBody CreateInitialStockRequest request) {
    headquarterAccessService.requireOwnedLocation(principal, storageLocationManagementUseCases.getById(request.locationId()).getHeadquarterId());
    Inventory created = inventoryManagementUseCases.createInitialStock(
        request.itemId(), request.locationId(), request.initialQuantity());
    return InventoryStockWebMapper.toResponse(created);
  }
}
