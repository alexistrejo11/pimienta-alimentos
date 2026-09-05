# Checklist de Phase 0

## Completado en este corte

- [x] Identificar módulos backend relacionados con sede, inventario y usuarios.
- [x] Registrar el mapeo preliminar y sus incompatibilidades.
- [x] Definir un snapshot `pos-bootstrap` versionado para debug.
- [x] Mantener el snapshot fuera de producción mediante `src/debug/assets`.
- [x] Cubrir stock normal, bajo, agotado y no controlado para productos por unidad.
- [x] Incluir usuarios de prueba con roles POS separados de los datos reales.
- [x] Elegir hub USB-C con PD como punto único de conexión de periféricos.

## Pendiente antes de Phase 1

- [ ] Confirmar modelos exactos de tablet, lector, impresora, hub, cables y cargador.
- [ ] Ejecutar la prueba física de carga simultánea y tres periféricos.
- [ ] Acordar contrato backend `pos-bootstrap` y endpoint de bootstrap; se mantiene abierto y no bloquea el desarrollo.
- [x] Definir contrato local debug para categorías de venta, precio efectivo y disponibilidad por unidad.
- [x] Convertir el catálogo real deprecated a un seed debug reproducible.
- [x] Añadir importer debug y Room sobre el contrato local.
- [x] Verificar el build en la computadora de desarrollo.

## Criterio de salida

Phase 0 termina cuando las decisiones que afectan el esquema local y los DTOs
tienen respuesta registrada. Las métricas de rendimiento se definirán en una
fase posterior, cuando existan flujos completos que medir.
