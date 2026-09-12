# Arquitectura técnica del POS

Esta documentación traduce las reglas de [producto](../product/README.md) a una propuesta técnica inicial. Define límites, responsabilidades y contratos generales; todavía no fija nombres definitivos de clases, tablas ni DTOs.

## Documentos

- [Visión general](01-vision-general.md): estilo arquitectónico, componentes, dependencias y decisiones de alcance.
- [Cliente Android](02-cliente-android.md): capas, módulos, persistencia local, sincronización y periféricos.
- [Modelo de dominio](03-modelo-de-dominio.md): entidades conceptuales, relaciones, estados e invariantes.
- [Sincronización y API](04-sincronizacion-y-api.md): protocolo offline-first, eventos y endpoints preliminares.
- [Web Central y backlog](05-web-central-y-backlog.md): responsabilidades administrativas centralizadas y capacidades explícitamente fuera del POS móvil.

## Principios

1. Cobrar es una operación local y nunca espera a la red.
2. Una venta confirmada es un hecho contable inmutable.
3. Las tablets sincronizan hechos y movimientos; nunca sobrescriben saldos centrales calculados.
4. La UI lee exclusivamente del almacenamiento local.
5. El backend central consolida sedes y aplica idempotencia.
6. Periféricos, red y almacenamiento son detalles reemplazables alrededor del dominio.
7. El diseño prepara crecimiento sin implementar complejidad que el MVP no utiliza.

## Estado de esta propuesta

La dirección recomendada es una aplicación Android nativa offline-first y un backend Spring Boot organizado como monolito modular. Antes de implementar deben validarse los nombres y relaciones contra el modelo existente del backend, especialmente la entidad que representa sede (`Headquarter`, `Project` o equivalente).
