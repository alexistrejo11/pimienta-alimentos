# Contexto y problema

## Operación

El POS atenderá una cafetería escolar de alto tráfico. Inicialmente habrá dos tablets Android ubicadas en caja y una sola sede operativa. La prioridad del negocio es mantener la fila en movimiento, aun si falla internet, la red local o el catálogo está incompleto.

Aunque el MVP opera una sola sede, el negocio puede abrir otras en el futuro. Por ello, las reglas y datos operativos se diseñan desde ahora con separación por sede, sin habilitar todavía flujos multi-sede en la interfaz.

## Situación actual

La operación actual presenta tres bloqueos principales:

1. **Flujo de venta lento.** Una venta requiere navegar por varias pantallas o modales: búsqueda, venta y cobro. Esto añade pasos y retrasa la atención.
2. **Periféricos inestables.** El uso de Bluetooth para lectores e impresoras se desconecta con frecuencia. Además, un lector configurado como teclado (HID) interfiere con el teclado virtual de la tablet.
3. **Catálogo e inventario como bloqueo.** Si un artículo no fue cargado o no tiene existencia registrada, no es posible terminar el cobro. La venta se pierde o el cajero debe interrumpir la atención.

## Problema que resolverá el producto

El nuevo POS debe permitir al cajero completar una venta de forma rápida y confiable en la propia tablet, sin depender de que un servicio remoto responda en ese momento. El sistema debe registrar evidencia suficiente para que la venta pueda sincronizarse, auditarse y conciliarse después.

## Principios de producto acordados

- **La venta no espera internet.** Cobrar debe funcionar sin conexión a internet ni a la LAN.
- **El camino frecuente es corto.** El flujo de venta y cobro no usa modales; el objetivo es completar un cobro habitual en hasta dos toques después de agregar los artículos.
- **La operación continúa ante excepciones.** Un dato incompleto no debe detener la fila; se registra la excepción para revisión posterior.
- **La información cobrada es histórica.** Los datos de una venta ya confirmada no se modifican cuando cambian precios o productos posteriormente.
- **Toda excepción es rastreable.** Las acciones que se salen del catálogo normal o de las reglas habituales deben conservar responsable, motivo y estado de revisión.

## Restricciones operativas conocidas

- Las tablets se mantendrán cargando y conectarán periféricos mediante un hub USB-C con Power Delivery de 45 W a 65 W.
- El lector de código de barras se prefiere por cable o en modo serial/SPP; se evita el modo teclado HID.
- La impresora se conectará por USB o por TCP/IP dentro de la red local.
- La sincronización posterior usará el backend existente, basado en Spring Boot y PostgreSQL.
