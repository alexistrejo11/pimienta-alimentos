package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryMovement;
import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.InventoryMovementQueryUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryMovementByReference;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryMovementGetById;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryMovementListByItem;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryMovementListByLocation;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryMovementSearch;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryMovements;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.InventoryMovementSearchRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.InventoryMovementResponse;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.mapper.InventoryMovementWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping(BASE + "/inventory/movements")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocInventoryMovements
public class InventoryMovementController {

  private final InventoryMovementQueryUseCases inventoryMovementQueryUseCases;
  private final HeadquarterAccessService headquarterAccessService;

  public InventoryMovementController(InventoryMovementQueryUseCases inventoryMovementQueryUseCases,
      HeadquarterAccessService headquarterAccessService) {
    this.inventoryMovementQueryUseCases = inventoryMovementQueryUseCases;
    this.headquarterAccessService = headquarterAccessService;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryMovementSearch
  public PagedResponse<InventoryMovementResponse> searchMovements(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @ParameterObject @ModelAttribute InventoryMovementSearchRequest filter) {
    Page<InventoryMovement> page =
         inventoryMovementQueryUseCases.search(
             filter.toCriteria(headquarterAccessService.enforceHeadquarterScope(principal, null)),
             filter.toPageable());
    return PagedResponse.map(page, InventoryMovementWebMapper::toResponse);
  }

  @GetMapping("/by-reference")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryMovementByReference
  public List<InventoryMovementResponse> findByReferenceNumber(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @RequestParam("referenceNumber") String referenceNumber) {
    List<InventoryMovement> movements =
        inventoryMovementQueryUseCases.findByReferenceNumber(referenceNumber);
    movements.forEach(m -> requireAccess(principal, m));
    return movements.stream().map(InventoryMovementWebMapper::toResponse).toList();
  }

  @GetMapping("/item/{itemId}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryMovementListByItem
  public List<InventoryMovementResponse> listByItem(@AuthenticationPrincipal JwtAuthenticationContext principal, @PathVariable Long itemId) {
    List<InventoryMovement> movements = inventoryMovementQueryUseCases.findByItemId(itemId);
    movements.forEach(m -> requireAccess(principal, m));
    return movements.stream().map(InventoryMovementWebMapper::toResponse).toList();
  }

  @GetMapping("/location/{locationId}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryMovementListByLocation
  public List<InventoryMovementResponse> listByLocation(@AuthenticationPrincipal JwtAuthenticationContext principal, @PathVariable Long locationId) {
    List<InventoryMovement> movements = inventoryMovementQueryUseCases.findByLocationId(locationId);
    movements.forEach(m -> requireAccess(principal, m));
    return movements.stream().map(InventoryMovementWebMapper::toResponse).toList();
  }

  @GetMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryMovementGetById
  public InventoryMovementResponse getMovementById(@AuthenticationPrincipal JwtAuthenticationContext principal, @PathVariable Long id) {
    InventoryMovement movement = inventoryMovementQueryUseCases.getById(id);
    requireAccess(principal, movement);
    return InventoryMovementWebMapper.toResponse(movement);
  }

  private void requireAccess(JwtAuthenticationContext principal, InventoryMovement movement) {
    if (movement.getSourceLocation() != null) {
      headquarterAccessService.requireOwnedLocation(principal, movement.getSourceLocation().getHeadquarterId());
    }
    if (movement.getDestinationLocation() != null) {
      headquarterAccessService.requireOwnedLocation(principal, movement.getDestinationLocation().getHeadquarterId());
    }
  }
}
