package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseCategoryTotal;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseDayTotal;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseSearchCriteria;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.OperatingExpense;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.enums.OperatingExpenseCategory;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.exception.OperatingExpenseNotFoundException;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.port.output.OperatingExpenseRepository;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.outbound.persistence.entity.OperatingExpenseJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class OperatingExpenseRepositoryImpl implements OperatingExpenseRepository {

  private final OperatingExpenseJpaRepository jpaRepository;
  private final EntityManager entityManager;

  public OperatingExpenseRepositoryImpl(
      OperatingExpenseJpaRepository jpaRepository, EntityManager entityManager) {
    this.jpaRepository = jpaRepository;
    this.entityManager = entityManager;
  }

  @Override
  public Optional<OperatingExpense> findById(long id) {
    return jpaRepository.findByIdAndDeletedAtIsNull(id).map(OperatingExpensePersistenceMapper::toDomain);
  }

  @Override
  public Page<OperatingExpense> search(OperatingExpenseSearchCriteria criteria, Pageable pageable) {
    Pageable ordered =
        PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Order.desc("occurredOn"), Sort.Order.desc("id")));
    return jpaRepository
        .findAll(OperatingExpenseSpecifications.fromCriteria(criteria), ordered)
        .map(OperatingExpensePersistenceMapper::toDomain);
  }

  @Override
  public BigDecimal sumAmount(OperatingExpenseSearchCriteria criteria) {
    var cb = entityManager.getCriteriaBuilder();
    var query = cb.createQuery(BigDecimal.class);
    var root = query.from(OperatingExpenseJpaEntity.class);
    query.select(cb.coalesce(cb.sum(root.get("amount")), cb.literal(BigDecimal.ZERO)));
    query.where(OperatingExpenseSpecifications.fromCriteria(criteria).toPredicate(root, query, cb));
    BigDecimal total = entityManager.createQuery(query).getSingleResult();
    return total == null ? BigDecimal.ZERO : total;
  }

  @Override
  public List<OperatingExpenseDayTotal> sumByDay(OperatingExpenseSearchCriteria criteria) {
    return grouped(criteria, "occurredOn", true).stream()
        .map(
            row ->
                new OperatingExpenseDayTotal(
                    (LocalDate) row[0], money(row[1]), count(row[2])))
        .toList();
  }

  @Override
  public List<OperatingExpenseCategoryTotal> sumByCategory(OperatingExpenseSearchCriteria criteria) {
    return grouped(criteria, "category", false).stream()
        .map(
            row ->
                new OperatingExpenseCategoryTotal(
                    category(row[0]), money(row[1]), count(row[2])))
        .toList();
  }

  private List<Object[]> grouped(OperatingExpenseSearchCriteria criteria, String field, boolean chronological) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
    Root<OperatingExpenseJpaEntity> root = query.from(OperatingExpenseJpaEntity.class);
    query.multiselect(root.get(field), cb.sum(root.get("amount")), cb.count(root));
    query.where(OperatingExpenseSpecifications.fromCriteria(criteria).toPredicate(root, query, cb));
    query.groupBy(root.get(field));
    if (chronological) {
      query.orderBy(cb.asc(root.get(field)));
    }
    return entityManager.createQuery(query).getResultList();
  }

  private static BigDecimal money(Object value) {
    if (value == null) {
      return BigDecimal.ZERO;
    }
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    return new BigDecimal(value.toString());
  }

  private static long count(Object value) {
    return value instanceof Number number ? number.longValue() : 0L;
  }

  private static OperatingExpenseCategory category(Object value) {
    if (value instanceof OperatingExpenseCategory category) {
      return category;
    }
    return OperatingExpenseCategory.valueOf(String.valueOf(value));
  }

  @Override
  public OperatingExpense save(OperatingExpense expense) {
    OperatingExpenseJpaEntity entity;
    if (expense.getId() != null && expense.getId() > 0) {
      entity =
          jpaRepository
              .findById(expense.getId())
              .orElseThrow(() -> new OperatingExpenseNotFoundException(expense.getId()));
    } else {
      entity = new OperatingExpenseJpaEntity();
    }
    OperatingExpensePersistenceMapper.apply(expense, entity);
    return OperatingExpensePersistenceMapper.toDomain(jpaRepository.save(entity));
  }
}
