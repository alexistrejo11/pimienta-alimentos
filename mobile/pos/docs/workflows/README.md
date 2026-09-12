# Workflows operativos del POS

Estos documentos conectan las reglas de [producto](../product/README.md), la [arquitectura](../architecture/README.md) y los [wireframes](../ux/README.md). Describen los flujos end-to-end: entradas de usuario, estados de interfaz, transacciones locales, periféricos y sincronización.

## Convenciones

- **Sincrónico local:** debe terminar antes de informar éxito al usuario.
- **Asíncrono:** ocurre después de confirmar localmente; su fallo no deshace una operación cobrada.
- **Guardia:** regla que permite, bloquea o solicita autorización antes de continuar.
- **Retorno seguro:** estado de interfaz al que vuelve la aplicación tras éxito, cancelación o excepción.
- **Decisión propuesta:** comportamiento documentado para iterar; no debe convertirse en código hasta confirmarlo si aparece como pendiente.

## Documentos

- [Ciclo operativo](01-ciclo-operativo.md): acceso, turno, sesión y entrada a venta.
- [Venta y pagos](02-venta-y-pagos.md): happy path, efectivo, tarjeta externa y monto abierto.
- [Manager, turno e inventario](03-manager-turno-inventario.md): Corte Z, historial, cancelación, mermas y reposiciones.
- [Procesos asíncronos y recuperación](04-asincronia-y-recuperacion.md): impresión, outbox, sincronización y errores.
- [Decisiones abiertas](05-decisiones-abiertas.md): temas que deben confirmarse antes de implementación.

## Regla transversal

```text
Acción de usuario
      ↓
Validar guardias y autorización
      ↓
Transacción local atómica
      ↓
Actualizar UI desde Room
      ├── efecto de hardware asíncrono
      └── evento Outbox asíncrono
```

Una operación confirmada localmente no espera impresora, LAN ni cloud. La UI siempre vuelve a un estado seguro y comprensible para continuar atendiendo.
