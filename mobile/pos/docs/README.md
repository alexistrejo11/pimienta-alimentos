# Documentación del POS

## Producto

Problema operativo, alcance del MVP y decisiones de negocio:

- [Índice de producto](product/README.md)

## Arquitectura

Propuesta técnica, componentes, dominio y contratos preliminares:

- [Índice de arquitectura](architecture/README.md)
- [Visión general](architecture/01-vision-general.md)
- [Cliente Android](architecture/02-cliente-android.md)
- [Modelo de dominio](architecture/03-modelo-de-dominio.md)
- [Sincronización y API](architecture/04-sincronizacion-y-api.md)
- [Web Central y backlog](architecture/05-web-central-y-backlog.md)

## UX y wireframes

Estructura de pantallas y flujos táctiles, independiente del diseño visual final:

- [Índice UX](ux/README.md)
- [Pantalla de venta y cobro](ux/01-venta-y-cobro.md)

## Workflows

Flujos end-to-end que conectan pantallas, dominio, datos, periféricos y sincronización:

- [Índice de workflows](workflows/README.md)
- [Ciclo operativo](workflows/01-ciclo-operativo.md)
- [Venta y pagos](workflows/02-venta-y-pagos.md)
- [Manager, turno e inventario](workflows/03-manager-turno-inventario.md)
- [Asincronía y recuperación](workflows/04-asincronia-y-recuperacion.md)
- [Decisiones abiertas](workflows/05-decisiones-abiertas.md)

## Implementación

Estructura de código, datos de desarrollo, fases y pruebas antes de escribir funcionalidades:

- [Plan técnico](implementation/README.md)
- [Blueprint técnico](implementation/01-blueprint-tecnico.md)
- [Datos reales e integración](implementation/02-datos-e-integracion.md)
- [Fases y estrategia de pruebas](implementation/03-fases-y-pruebas.md)
- [Entorno de desarrollo Android](implementation/01-entorno-desarrollo.md)
- [Contrato local de bootstrap](implementation/local-pos-bootstrap-contract.md)

La documentación de producto define el comportamiento esperado. La documentación de arquitectura propone cómo soportarlo. Si ambas entran en conflicto, primero se aclara la regla de negocio y después se actualiza la arquitectura.
