package io.github.alexistrejo11.pimienta.module.supplier.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.HeadquarterNotFoundException;
import io.github.alexistrejo11.pimienta.module.supplier.core.application.command.UpsertSupplierCommand;
import io.github.alexistrejo11.pimienta.module.supplier.core.application.query.SupplierSearchCriteria;
import io.github.alexistrejo11.pimienta.module.supplier.core.domain.Supplier;
import io.github.alexistrejo11.pimienta.module.supplier.core.domain.exception.SupplierNotFoundException;
import io.github.alexistrejo11.pimienta.module.supplier.core.port.input.SupplierUseCases;
import io.github.alexistrejo11.pimienta.module.supplier.core.port.output.SupplierRepository;
import java.util.List;
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
    resolveHeadquarters(command.headquarterIds());
    Supplier saved =
        supplierRepository.save(
            Supplier.builder()
                .withName(command.name())
                .withContactName(command.contactName())
                .withPhone(command.phone())
                .withBrand(command.brand())
                .withHeadquarterIds(command.headquarterIds())
                .register());
    log.info("create supplier complete supplierId={}", saved.getId());
    return saved;
  }

  @Override
  @Transactional
  public Supplier update(long id, UpsertSupplierCommand command) {
    Supplier existing = getById(id);
    resolveHeadquarters(command.headquarterIds());
    existing.setName(command.name());
    existing.setContactName(command.contactName());
    existing.setPhone(command.phone());
    existing.setBrand(command.brand());
    existing.setHeadquarterIds(command.headquarterIds());
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

  private void resolveHeadquarters(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    for (Long hqId : ids) {
      headquarterRepository
          .findById(hqId)
          .orElseThrow(() -> new HeadquarterNotFoundException(hqId));
    }
  }
}
