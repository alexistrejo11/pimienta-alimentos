package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.headquarter.core.application.PosProductManagementUseCases;
import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.CreatePosProductCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.CreatePosProductRequest;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.CreatedPosProductResponse;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(BASE + "/headquarters/{id}/pos-products")
@RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
public class PosProductController {
  private final PosProductManagementUseCases useCases;
  private final HeadquarterAccessService access;
  public PosProductController(PosProductManagementUseCases useCases, HeadquarterAccessService access) { this.useCases = useCases; this.access = access; }
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CreatedPosProductResponse create(@AuthenticationPrincipal JwtAuthenticationContext p, @PathVariable Long id, @Valid @RequestBody CreatePosProductRequest r) {
    access.requireHeadquarterAccess(p, id);
    var result = useCases.create(id, new CreatePosProductCommand(r.name(), r.description(), r.costPrice(), r.salePrice(), r.category(), r.unit(), r.brand(), r.barcode(), r.reorderPoint(), r.reorderQuantity(), r.posSaleCategoryId(), r.available(), r.stockPolicy(), r.negativeStockLimit()));
    var item = result.item(); var row = result.catalog();
    return new CreatedPosProductResponse(item.getId(), item.getSku(), item.getName(), item.getBarcode(), item.getCategory(), item.getUnit(), item.getCostPrice(), item.getSalePrice(), row.getHeadquarterId(), row.getPosSaleCategoryId(), row.getSaleCategory(), row.isAvailable(), row.getStockPolicy());
  }
}
