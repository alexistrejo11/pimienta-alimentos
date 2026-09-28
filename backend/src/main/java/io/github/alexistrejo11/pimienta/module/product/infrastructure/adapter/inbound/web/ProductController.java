package io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.module.product.core.port.input.ProductManagementUseCases;
import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.doc.DocProductCreate;
import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.doc.DocProductGetById;
import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.doc.DocProductLookup;
import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.doc.DocProductSearch;
import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.doc.DocProductUpdate;
import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.doc.DocProducts;
import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.dto.request.ProductCreateRequest;
import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.dto.request.ProductSearchRequest;
import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.dto.request.ProductUpdateRequest;
import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.dto.response.ProductResponse;
import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.mapper.ProductWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/products")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocProducts
public class ProductController {

  private final ProductManagementUseCases productManagementUseCases;

  public ProductController(ProductManagementUseCases productManagementUseCases) {
    this.productManagementUseCases = productManagementUseCases;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocProductSearch
  public PagedResponse<ProductResponse> search(
      @ParameterObject @ModelAttribute ProductSearchRequest filter) {
    return PagedResponse.map(
        productManagementUseCases.search(filter.toCriteria(), filter.toPageable()),
        ProductWebMapper::toResponse);
  }

  @GetMapping("/lookup")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocProductLookup
  public ProductResponse lookup(@RequestParam("q") @NotBlank String skuOrBarcode) {
    return ProductWebMapper.toResponse(
        productManagementUseCases.getBySkuOrBarcode(skuOrBarcode.trim()));
  }

  @GetMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocProductGetById
  public ProductResponse getById(@PathVariable long id) {
    return ProductWebMapper.toResponse(productManagementUseCases.getById(id));
  }

  @PostMapping
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @ResponseStatus(HttpStatus.CREATED)
  @DocProductCreate
  public ProductResponse create(@Valid @RequestBody ProductCreateRequest request) {
    return ProductWebMapper.toResponse(
        productManagementUseCases.create(ProductWebMapper.toCreateCommand(request)));
  }

  @PutMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocProductUpdate
  public ProductResponse update(
      @PathVariable long id, @Valid @RequestBody ProductUpdateRequest request) {
    return ProductWebMapper.toResponse(
        productManagementUseCases.update(id, ProductWebMapper.toUpdateCommand(request)));
  }
}
