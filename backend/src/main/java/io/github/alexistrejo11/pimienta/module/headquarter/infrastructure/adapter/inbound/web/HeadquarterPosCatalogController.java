package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.input.HeadquarterPosCatalogUseCases;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterPosCatalog;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterPosCatalogGet;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterPosCatalogList;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterPosCatalogPut;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.HeadquarterPosCatalogItemRequest;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.HeadquarterPosCatalogItemResponse;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

  public HeadquarterPosCatalogController(
      HeadquarterPosCatalogUseCases posCatalogUseCases,
      HeadquarterAccessService headquarterAccessService) {
    this.posCatalogUseCases = posCatalogUseCases;
    this.headquarterAccessService = headquarterAccessService;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocHeadquarterPosCatalogList
  public PagedResponse<HeadquarterPosCatalogItemResponse> list(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @PathVariable("id") Long headquarterId,
      @ModelAttribute PageableRequest pageable) {
    headquarterAccessService.requireHeadquarterAccess(principal, headquarterId);
    Page<HeadquarterItem> page =
        posCatalogUseCases.list(headquarterId, pageable.toPageable());
    return PagedResponse.map(page, HeadquarterPosWebMapper::toResponse);
  }

  @GetMapping("/{itemId}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocHeadquarterPosCatalogGet
  public HeadquarterPosCatalogItemResponse get(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @PathVariable("id") Long headquarterId,
      @PathVariable Long itemId) {
    headquarterAccessService.requireHeadquarterAccess(principal, headquarterId);
    return HeadquarterPosWebMapper.toResponse(posCatalogUseCases.get(headquarterId, itemId));
  }

  @PutMapping("/{itemId}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocHeadquarterPosCatalogPut
  public HeadquarterPosCatalogItemResponse put(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @PathVariable("id") Long headquarterId,
      @PathVariable Long itemId,
      @Valid @RequestBody HeadquarterPosCatalogItemRequest request) {
    headquarterAccessService.requireHeadquarterAccess(principal, headquarterId);
    HeadquarterItem saved =
        posCatalogUseCases.upsert(headquarterId, itemId, HeadquarterPosWebMapper.toCommand(request));
    return HeadquarterPosWebMapper.toResponse(saved);
  }
}
