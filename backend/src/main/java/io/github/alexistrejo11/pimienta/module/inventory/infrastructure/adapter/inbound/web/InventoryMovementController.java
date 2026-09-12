package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryMovement;
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

@RestController
@RequestMapping(BASE + "/inventory/movements")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocInventoryMovements
public class InventoryMovementController {

  private final InventoryMovementQueryUseCases inventoryMovementQueryUseCases;

  public InventoryMovementController(InventoryMovementQueryUseCases inventoryMovementQueryUseCases) {
    this.inventoryMovementQueryUseCases = inventoryMovementQueryUseCases;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryMovementSearch
  public PagedResponse<InventoryMovementResponse> searchMovements(
      @ParameterObject @ModelAttribute InventoryMovementSearchRequest filter) {
    Page<InventoryMovement> page =
        inventoryMovementQueryUseCases.search(filter.toCriteria(), filter.toPageable());
    return PagedResponse.map(page, InventoryMovementWebMapper::toResponse);
  }

  @GetMapping("/by-reference")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryMovementByReference
  public List<InventoryMovementResponse> findByReferenceNumber(
      @RequestParam("referenceNumber") String referenceNumber) {
    List<InventoryMovement> movements =
        inventoryMovementQueryUseCases.findByReferenceNumber(referenceNumber);
    return movements.stream().map(InventoryMovementWebMapper::toResponse).toList();
  }

  @GetMapping("/item/{itemId}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryMovementListByItem
  public List<InventoryMovementResponse> listByItem(@PathVariable Long itemId) {
    List<InventoryMovement> movements = inventoryMovementQueryUseCases.findByItemId(itemId);
    return movements.stream().map(InventoryMovementWebMapper::toResponse).toList();
  }

  @GetMapping("/location/{locationId}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryMovementListByLocation
  public List<InventoryMovementResponse> listByLocation(@PathVariable Long locationId) {
    List<InventoryMovement> movements = inventoryMovementQueryUseCases.findByLocationId(locationId);
    return movements.stream().map(InventoryMovementWebMapper::toResponse).toList();
  }

  @GetMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryMovementGetById
  public InventoryMovementResponse getMovementById(@PathVariable Long id) {
    InventoryMovement movement = inventoryMovementQueryUseCases.getById(id);
    return InventoryMovementWebMapper.toResponse(movement);
  }
}
