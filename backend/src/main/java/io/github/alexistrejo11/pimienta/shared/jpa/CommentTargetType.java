package io.github.alexistrejo11.pimienta.shared.jpa;

/** Supported entities for polymorphic {@link EntityCommentJpaEntity} rows. */
public enum CommentTargetType {
  TASK,
  CONTRACT,
  EMPLOYEE,
  HEADQUARTER,
  INVENTORY_ITEM,
  INVENTORY_TRANSACTION,
  PAYROLL_RECORD
}
