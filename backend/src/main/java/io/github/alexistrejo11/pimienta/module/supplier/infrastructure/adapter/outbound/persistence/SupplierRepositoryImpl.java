package io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.supplier.core.application.query.SupplierSearchCriteria;
import io.github.alexistrejo11.pimienta.module.supplier.core.domain.Supplier;
import io.github.alexistrejo11.pimienta.module.supplier.core.domain.exception.SupplierNotFoundException;
import io.github.alexistrejo11.pimienta.module.supplier.core.port.output.SupplierRepository;
import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.outbound.persistence.entity.SupplierJpaEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class SupplierRepositoryImpl implements SupplierRepository {

  private final SupplierJpaRepository jpaRepository;

  public SupplierRepositoryImpl(SupplierJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<Supplier> findById(long id) {
    return jpaRepository.findByIdAndDeletedAtIsNull(id).map(SupplierPersistenceMapper::toDomain);
  }

  @Override
  public Page<Supplier> search(SupplierSearchCriteria criteria, Pageable pageable) {
    SupplierSearchCriteria effective = criteria != null ? criteria : SupplierSearchCriteria.empty();
    Specification<SupplierJpaEntity> spec =
        SupplierSpecifications.fromCriteria(effective);
    return jpaRepository.findAll(spec, pageable).map(SupplierPersistenceMapper::toDomain);
  }

  @Override
  public Supplier save(Supplier supplier) {
    SupplierJpaEntity entity;
    if (supplier.getId() != null && supplier.getId() > 0) {
      entity =
          jpaRepository
              .findById(supplier.getId())
              .orElseThrow(() -> new SupplierNotFoundException(supplier.getId()));
    } else {
      entity = new SupplierJpaEntity();
    }
    SupplierPersistenceMapper.apply(supplier, entity);
    return SupplierPersistenceMapper.toDomain(jpaRepository.save(entity));
  }
}
