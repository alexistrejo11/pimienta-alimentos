package io.github.alexistrejo11.pimienta.module.crm.adapter.out.persistence.repository;

import io.github.alexistrejo11.pimienta.module.crm.adapter.out.persistence.mapper.ClientPersistenceMapper;
import io.github.alexistrejo11.pimienta.module.crm.adapter.out.persistence.model.ClientJpaEntity;
import io.github.alexistrejo11.pimienta.module.crm.adapter.out.persistence.specification.ClientSpecifications;
import io.github.alexistrejo11.pimienta.module.crm.core.application.query.ClientSearchCriteria;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.Client;
import io.github.alexistrejo11.pimienta.module.crm.core.port.output.ClientRepository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class ClientRepositoryImpl implements ClientRepository {

  private final ClientJpaRepository jpaRepository;

  public ClientRepositoryImpl(ClientJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<Client> findById(long id) {
    return jpaRepository.findByIdAndDeletedAtIsNull(id).map(ClientPersistenceMapper::toDomain);
  }

  @Override
  public Page<Client> search(ClientSearchCriteria criteria, Pageable pageable) {
    ClientSearchCriteria effective = criteria != null ? criteria : ClientSearchCriteria.empty();
    Specification<ClientJpaEntity> spec = ClientSpecifications.fromCriteria(effective);
    return jpaRepository.findAll(spec, pageable).map(ClientPersistenceMapper::toDomain);
  }

  @Override
  public Client save(Client client) {
    ClientJpaEntity saved = jpaRepository.save(ClientPersistenceMapper.toJpa(client));
    return ClientPersistenceMapper.toDomain(saved);
  }
}
