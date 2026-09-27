package io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.supplier.core.application.query.SupplierSearchCriteria;
import io.github.alexistrejo11.pimienta.module.supplier.core.domain.Supplier;
import io.github.alexistrejo11.pimienta.module.supplier.core.port.input.SupplierUseCases;
import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.doc.DocSupplierCreate;
import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.doc.DocSupplierDelete;
import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.doc.DocSupplierGet;
import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.doc.DocSupplierList;
import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.doc.DocSupplierUpdate;
import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.doc.DocSuppliers;
import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.dto.SupplierResponse;
import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.dto.SupplierSearchRequest;
import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.dto.UpsertSupplierRequest;
import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.mapper.SupplierWebMapper;
import io.github.alexistrejo11.pimienta.shared.exception.ForbiddenException;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/suppliers")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocSuppliers
public class SupplierController {

  private final SupplierUseCases supplierUseCases;
  private final HeadquarterAccessService headquarterAccessService;

  public SupplierController(
      SupplierUseCases supplierUseCases, HeadquarterAccessService headquarterAccessService) {
    this.supplierUseCases = supplierUseCases;
    this.headquarterAccessService = headquarterAccessService;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocSupplierList
  public PagedResponse<SupplierResponse> list(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @ModelAttribute SupplierSearchRequest filter) {
    SupplierSearchRequest query = filter != null ? filter : new SupplierSearchRequest();
    Long hqFilter =
        headquarterAccessService.enforceHeadquarterFilter(principal, query.getHeadquarterId());
    query.setHeadquarterId(hqFilter);
    List<Long> scope =
        headquarterAccessService.isAdmin(principal)
            ? null
            : List.copyOf(headquarterAccessService.assignedHeadquarters(principal));
    SupplierSearchCriteria criteria = SupplierWebMapper.toCriteria(query, scope);
    return PagedResponse.map(
        supplierUseCases.search(
            criteria, PageRequest.of(query.getPage(), query.getSize())),
        SupplierWebMapper::toResponse);
  }

  @GetMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocSupplierGet
  public SupplierResponse get(
      @AuthenticationPrincipal JwtAuthenticationContext principal, @PathVariable long id) {
    Supplier supplier = supplierUseCases.getById(id);
    requireSupplierAccess(principal, supplier);
    return SupplierWebMapper.toResponse(supplier);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocSupplierCreate
  public SupplierResponse create(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @Valid @RequestBody UpsertSupplierRequest request) {
    requireWriteHeadquarters(principal, request.headquarterIds());
    return SupplierWebMapper.toResponse(
        supplierUseCases.create(SupplierWebMapper.toCommand(request)));
  }

  @PutMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocSupplierUpdate
  public SupplierResponse update(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @PathVariable long id,
      @Valid @RequestBody UpsertSupplierRequest request) {
    Supplier existing = supplierUseCases.getById(id);
    requireSupplierAccess(principal, existing);
    requireWriteHeadquarters(principal, request.headquarterIds());
    return SupplierWebMapper.toResponse(
        supplierUseCases.update(id, SupplierWebMapper.toCommand(request)));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocSupplierDelete
  public void delete(
      @AuthenticationPrincipal JwtAuthenticationContext principal, @PathVariable long id) {
    Supplier existing = supplierUseCases.getById(id);
    requireSupplierAccess(principal, existing);
    supplierUseCases.delete(id);
  }

  private void requireSupplierAccess(JwtAuthenticationContext principal, Supplier supplier) {
    if (headquarterAccessService.isAdmin(principal)) {
      return;
    }
    List<Long> assigned = headquarterAccessService.assignedHeadquarters(principal);
    boolean overlap =
        supplier.getHeadquarterIds().stream().anyMatch(assigned::contains);
    if (!overlap) {
      throw new ForbiddenException(
          "Access denied for this supplier",
          Map.of("supplierId", supplier.getId()),
          "supplier access denied userId=" + principal.userId());
    }
  }

  private void requireWriteHeadquarters(JwtAuthenticationContext principal, List<Long> headquarterIds) {
    List<Long> ids = headquarterIds != null ? headquarterIds : List.of();
    for (Long hqId : ids) {
      headquarterAccessService.requireHeadquarterAccess(principal, hqId);
    }
    if (headquarterAccessService.isAdmin(principal)) {
      return;
    }
    for (Long assigned : headquarterAccessService.assignedHeadquarters(principal)) {
      if (!ids.contains(assigned)) {
        throw new ForbiddenException(
            "Assigned headquarter must be included",
            Map.of("headquarterId", assigned),
            "supplier write missing assigned HQ userId=" + principal.userId());
      }
    }
  }
}
