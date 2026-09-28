package io.github.alexistrejo11.pimienta.module.supplier.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.HeadquarterNotFoundException;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterRepository;
import io.github.alexistrejo11.pimienta.module.supplier.core.application.command.UpsertSupplierCommand;
import io.github.alexistrejo11.pimienta.module.supplier.core.application.query.SupplierSearchCriteria;
import io.github.alexistrejo11.pimienta.module.supplier.core.domain.Supplier;
import io.github.alexistrejo11.pimienta.module.supplier.core.domain.SupplierHeadquarterLink;
import io.github.alexistrejo11.pimienta.module.supplier.core.domain.exception.SupplierNotFoundException;
import io.github.alexistrejo11.pimienta.module.supplier.core.port.input.SupplierUseCases;
import io.github.alexistrejo11.pimienta.module.supplier.core.port.output.SupplierRepository;
import io.github.alexistrejo11.pimienta.shared.exception.BusinessValidationException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierUseCasesImpl implements SupplierUseCases {

  private static final Logger log = LoggerFactory.getLogger(SupplierUseCasesImpl.class);

  private final SupplierRepository supplierRepository;
  private final HeadquarterRepository headquarterRepository;

  public SupplierUseCasesImpl(
      SupplierRepository supplierRepository, HeadquarterRepository headquarterRepository) {
    this.supplierRepository = supplierRepository;
    this.headquarterRepository = headquarterRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Supplier> search(SupplierSearchCriteria criteria, Pageable pageable) {
    SupplierSearchCriteria effective = criteria != null ? criteria : SupplierSearchCriteria.empty();
    return supplierRepository.search(effective, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Supplier getById(long id) {
    return supplierRepository.findById(id).orElseThrow(() -> new SupplierNotFoundException(id));
  }

  @Override
  @Transactional
  public Supplier create(UpsertSupplierCommand command) {
    log.info("create supplier start brand={}", command.brand());
    List<SupplierHeadquarterLink> headquarters = normalizeHeadquarters(command.headquarters());
    Supplier saved =
        supplierRepository.save(
            Supplier.builder()
                .withName(command.name())
                .withPhone(command.phone())
                .withBrand(command.brand())
                .withHeadquarters(headquarters)
                .register());
    log.info("create supplier complete supplierId={}", saved.getId());
    return saved;
  }

  @Override
  @Transactional
  public Supplier update(long id, UpsertSupplierCommand command) {
    Supplier existing = getById(id);
    List<SupplierHeadquarterLink> headquarters = normalizeHeadquarters(command.headquarters());
    existing.setName(command.name());
    existing.setPhone(command.phone());
    existing.setBrand(command.brand());
    existing.setHeadquarters(headquarters);
    existing.touch();
    return supplierRepository.save(existing);
  }

  @Override
  @Transactional
  public void delete(long id) {
    Supplier existing = getById(id);
    existing.delete();
    supplierRepository.save(existing);
  }

  private List<SupplierHeadquarterLink> normalizeHeadquarters(List<SupplierHeadquarterLink> links) {
    List<SupplierHeadquarterLink> source = links != null ? links : List.of();
    Set<Long> seen = new HashSet<>();
    for (SupplierHeadquarterLink link : source) {
      if (!seen.add(link.headquarterId())) {
        throw new BusinessValidationException(
            "Duplicate headquarter",
            Map.of("headquarterId", link.headquarterId()),
            "duplicate supplier headquarter id=" + link.headquarterId());
      }
      headquarterRepository
          .findById(link.headquarterId())
          .orElseThrow(() -> new HeadquarterNotFoundException(link.headquarterId()));
    }
    return List.copyOf(source);
  }
}
