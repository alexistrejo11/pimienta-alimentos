package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.input.HeadquarterPosCatalogUseCases;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterPosCatalog;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterPosCatalogDelete;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterPosCatalogGet;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterPosCatalogList;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterPosCatalogPut;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.HeadquarterPosCatalogItemRequest;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.HeadquarterPosCatalogItemResponse;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.HeadquarterPosCatalogSearchRequest;
import io.github.alexistrejo11.pimienta.module.product.core.port.output.ProductRepository;
import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.dto.response.ProductResponse;
import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.mapper.ProductWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/headquarters/{id}/pos-catalog")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocHeadquarterPosCatalog
public class HeadquarterPosCatalogController {

  private final HeadquarterPosCatalogUseCases posCatalogUseCases;
  private final HeadquarterAccessService headquarterAccessService;
  private final ProductRepository productRepository;

  public HeadquarterPosCatalogController(
      HeadquarterPosCatalogUseCases posCatalogUseCases,
      HeadquarterAccessService headquarterAccessService,
      ProductRepository productRepository) {
    this.posCatalogUseCases = posCatalogUseCases;
    this.headquarterAccessService = headquarterAccessService;
    this.productRepository = productRepository;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocHeadquarterPosCatalogList
  public PagedResponse<HeadquarterPosCatalogItemResponse> list(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @PathVariable("id") Long headquarterId,
      @ModelAttribute HeadquarterPosCatalogSearchRequest filter) {
    headquarterAccessService.requireHeadquarterAccess(principal, headquarterId);
    Page<HeadquarterItem> page =
        posCatalogUseCases.search(
            headquarterId,
            filter.getSearch(),
            filter.getSaleCategory(),
            filter.getAvailable(),
            filter.getStockPolicy(),
            filter.toPageable());
    return PagedResponse.map(page, this::toResponse);
  }

  @GetMapping("/candidates")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  public List<ProductResponse> candidates(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @PathVariable("id") Long headquarterId) {
    headquarterAccessService.requireHeadquarterAccess(principal, headquarterId);
    return posCatalogUseCases.candidates(headquarterId).stream()
        .map(ProductWebMapper::toResponse)
        .toList();
  }

  @GetMapping("/{productId}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocHeadquarterPosCatalogGet
  public HeadquarterPosCatalogItemResponse get(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @PathVariable("id") Long headquarterId,
      @PathVariable long productId) {
    headquarterAccessService.requireHeadquarterAccess(principal, headquarterId);
    return toResponse(posCatalogUseCases.get(headquarterId, productId));
  }

  @PutMapping("/{productId}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocHeadquarterPosCatalogPut
  public HeadquarterPosCatalogItemResponse put(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @PathVariable("id") Long headquarterId,
      @PathVariable long productId,
      @Valid @RequestBody HeadquarterPosCatalogItemRequest request) {
    headquarterAccessService.requireHeadquarterAccess(principal, headquarterId);
    HeadquarterItem saved =
        posCatalogUseCases.upsert(
            headquarterId, productId, HeadquarterPosWebMapper.toCommand(request));
    return toResponse(saved);
  }

  @DeleteMapping("/{productId}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocHeadquarterPosCatalogDelete
  public HeadquarterPosCatalogItemResponse delete(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @PathVariable("id") Long headquarterId,
      @PathVariable long productId) {
    headquarterAccessService.requireHeadquarterAccess(principal, headquarterId);
    return toResponse(posCatalogUseCases.softDelete(headquarterId, productId));
  }

  private HeadquarterPosCatalogItemResponse toResponse(HeadquarterItem row) {
    return productRepository
        .findById(row.getProductId())
        .map(product -> HeadquarterPosWebMapper.toResponse(row, product))
        .orElseGet(() -> HeadquarterPosWebMapper.toResponse(row));
  }
}
