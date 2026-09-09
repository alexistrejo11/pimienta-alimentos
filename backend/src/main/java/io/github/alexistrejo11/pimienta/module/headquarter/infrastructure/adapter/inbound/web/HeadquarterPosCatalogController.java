package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/headquarters/{id}/pos-catalog")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocHeadquarterPosCatalog
public class HeadquarterPosCatalogController {

  private final HeadquarterPosCatalogUseCases posCatalogUseCases;

  public HeadquarterPosCatalogController(HeadquarterPosCatalogUseCases posCatalogUseCases) {
    this.posCatalogUseCases = posCatalogUseCases;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocHeadquarterPosCatalogList
  public PagedResponse<HeadquarterPosCatalogItemResponse> list(
      @PathVariable("id") Long headquarterId, @ModelAttribute PageableRequest pageable) {
    Page<HeadquarterItem> page =
        posCatalogUseCases.list(headquarterId, pageable.toPageable());
    return PagedResponse.map(page, HeadquarterPosWebMapper::toResponse);
  }

  @GetMapping("/{itemId}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocHeadquarterPosCatalogGet
  public HeadquarterPosCatalogItemResponse get(
      @PathVariable("id") Long headquarterId, @PathVariable Long itemId) {
    return HeadquarterPosWebMapper.toResponse(posCatalogUseCases.get(headquarterId, itemId));
  }

  @PutMapping("/{itemId}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocHeadquarterPosCatalogPut
  public HeadquarterPosCatalogItemResponse put(
      @PathVariable("id") Long headquarterId,
      @PathVariable Long itemId,
      @Valid @RequestBody HeadquarterPosCatalogItemRequest request) {
    HeadquarterItem saved =
        posCatalogUseCases.upsert(headquarterId, itemId, HeadquarterPosWebMapper.toCommand(request));
    return HeadquarterPosWebMapper.toResponse(saved);
  }
}
