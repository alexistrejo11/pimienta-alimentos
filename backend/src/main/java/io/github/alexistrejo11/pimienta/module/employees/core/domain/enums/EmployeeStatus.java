package io.github.alexistrejo11.pimienta.module.employees.core.domain.enums;

/**
 * Employee employment lifecycle. Independent of contracts.
 *
 * <ul>
 *   <li>{@link #ACTIVE} — currently employed and active
 *   <li>{@link #INACTIVE} — registered but not active (default on create)
 *   <li>{@link #RESIGNED} — renuncia
 *   <li>{@link #TERMINATED} — despedido
 *   <li>{@link #DOWN} — baja general / administrativa
 * </ul>
 */
public enum EmployeeStatus {
  ACTIVE,
  INACTIVE,
  RESIGNED,
  TERMINATED,
  DOWN
}
