package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.headquarter.core.application.PosProductManagementUseCases;
import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.CreatePosProductCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterPosProductCreate;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterPosProducts;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.CreatePosProductRequest;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.CreatedPosProductResponse;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/headquarters/{id}/pos-products")
@RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
@DocHeadquarterPosProducts
public class PosProductController {

  private final PosProductManagementUseCases useCases;
  private final HeadquarterAccessService access;

  public PosProductController(
      PosProductManagementUseCases useCases, HeadquarterAccessService access) {
    this.useCases = useCases;
    this.access = access;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @DocHeadquarterPosProductCreate
  public CreatedPosProductResponse create(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @PathVariable Long id,
      @Valid @RequestBody CreatePosProductRequest request) {
    access.requireHeadquarterAccess(principal, id);

    var result =
        useCases.create(
            id,
            new CreatePosProductCommand(
                request.name(),
                request.description(),
                request.barcode(),
                request.unit(),
                request.salePrice(),
                request.posSaleCategoryId(),
                request.available(),
                request.stockPolicy(),
                request.negativeStockLimit()));

    var product = result.product();
    var row = result.catalog();
    return new CreatedPosProductResponse(
        product.getId(),
        product.getSku(),
        product.getName(),
        product.getBarcode(),
        product.getUnit(),
        product.isTrackStock(),
        row.getSalePrice(),
        row.getHeadquarterId(),
        row.getPosSaleCategoryId(),
        row.getSaleCategory(),
        row.isAvailable(),
        row.getStockPolicy());
  }
}
