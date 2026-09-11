package io.github.alexistrejo11.pimienta.module.crm.adapter.out.persistence.specification;

import io.github.alexistrejo11.pimienta.module.crm.adapter.out.persistence.model.ClientJpaEntity;
import io.github.alexistrejo11.pimienta.module.crm.core.application.query.ClientSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ClientSpecifications {

  private ClientSpecifications() {}

  public static Specification<ClientJpaEntity> fromCriteria(ClientSearchCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> parts = new ArrayList<>();
      parts.add(cb.isNull(root.get("deletedAt")));

      if (criteria != null && criteria.nameContains() != null && !criteria.nameContains().isBlank()) {
        String pattern = "%" + criteria.nameContains().trim().toLowerCase() + "%";
        parts.add(
            cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("companyName")), pattern)));
      }

      return cb.and(parts.toArray(Predicate[]::new));
    };
  }
}
