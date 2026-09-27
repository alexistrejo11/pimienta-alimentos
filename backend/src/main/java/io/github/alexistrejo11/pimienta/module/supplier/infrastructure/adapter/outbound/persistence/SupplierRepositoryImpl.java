package io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.supplier.core.application.query.SupplierSearchCriteria;
import io.github.alexistrejo11.pimienta.module.supplier.core.domain.Supplier;
import io.github.alexistrejo11.pimienta.module.supplier.core.port.output.SupplierRepository;
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
    Specification<io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.outbound.persistence.entity.SupplierJpaEntity> spec =
        SupplierSpecifications.fromCriteria(effective);
    return jpaRepository.findAll(spec, pageable).map(SupplierPersistenceMapper::toDomain);
  }

  @Override
  public Supplier save(Supplier supplier) {
    return SupplierPersistenceMapper.toDomain(
        jpaRepository.save(SupplierPersistenceMapper.toJpa(supplier)));
  }
}
