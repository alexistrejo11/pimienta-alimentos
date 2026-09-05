# Web central y backlog fuera del POS móvil

## Propósito

La tablet es una caja offline-first: cobra, conserva evidencia y permite supervisar **su propia operación local**. La Web Central, sobre el backend Spring Boot, es el lugar de administración maestra, consolidación y auditoría entre sedes.

Este documento reúne funciones que se descartaron deliberadamente del panel de Manager de la tablet. Que una función aparezca aquí **no significa que se implemente en el MVP**; indica su dueño arquitectónico si se necesita después. Así se evita cargar el POS de caja con formularios largos, acciones globales o tareas que requieren red.

## Límite de responsabilidad

```text
Tablet POS local                         Web Central + backend
-----------------                         --------------------
cobro y ticket                           catálogo maestro
turno y Corte Z local                    configuración por sede
merma / reposición operativa             inventario consolidado
historial de su tablet                   usuarios y dispositivos
cola de eventos                          incidencias y auditoría global
estado de periféricos                    reportes multisede
```

La Web Central no sustituye el registro local de una venta ni corrige automáticamente un ticket ya cobrado. Recibe hechos inmutables, los consolida y permite a Superadmin resolver incidencias con bitácora.

## Capacidades centrales decididas

Estas capacidades ya tienen una responsabilidad central definida, aunque su pantalla, prioridad exacta y endpoints finales se validarán contra el backend existente.

### Catálogo y configuración de sede

- alta, edición, desactivación y categorización de productos maestros;
- código único, precios base y atributos de venta, incluido el formato de artículos por peso cuando se confirme la etiqueta real;
- disponibilidad, inventario y precio efectivo por sede;
- políticas por sede: modo de inventario, límite de stock negativo, antigüedad tolerada del catálogo y categorías permitidas de monto abierto;
- publicación de cambios como bootstrap o delta para tablets enroladas.

La tablet recibe una proyección de catálogo para su sede. No crea productos ni modifica permanentemente precios, categorías, disponibilidad o configuraciones globales.

### Inventario maestro

- consulta de existencias y movimientos consolidados por sede y producto;
- conteos físicos periódicos, ajustes masivos y sus razones;
- compras y entradas globales a proveedores;
- auditoría de ventas, reposiciones operativas y mermas recibidas de cada tablet;
- valoración e indicadores de inventario cuando el negocio lo requiera.

Una reposición o merma de la tablet es un hecho operativo sincronizable; no autoriza a esa tablet a reescribir el saldo maestro ni a efectuar un conteo global.

### Seguridad, personas y dispositivos

- crear, editar, activar o desactivar usuarios;
- asignar roles, renovar PIN y publicar los verificadores locales requeridos para modo offline;
- generar códigos de enrolamiento, asignar tablet a sede, revisar estado y revocar dispositivos;
- definir políticas de actualización mínima, retención de datos y accesos administrativos;
- consultar bitácoras de autorizaciones, intentos de Corte Z, descuentos, cancelaciones y acciones críticas.

El POS no administra credenciales ni permisos maestros. Solo valida localmente el snapshot autorizado que descargó.

### Conciliación e incidencias

Solo Superadmin resuelve registros `REQUIRES_REVIEW` provenientes de sincronización. Las acciones son:

1. **Aceptar y clasificar:** conservar ingreso y ticket originales, agregando una categoría/etiqueta contable y nota obligatoria.
2. **Vincular a producto o movimiento correcto:** conservar el ticket y el importe originales, pero asociar la incidencia a un producto maestro para reflejar el impacto de inventario y reportes.

Cada resolución guarda responsable, fecha, acción y nota. Nunca elimina ni modifica retrospectivamente el hecho enviado por la caja.

### Reportes y auditoría consolidada

- ventas por producto, categoría, sede, periodo y estado de sincronización;
- mermas, reposiciones, cancelaciones, descuentos y motivos;
- historial de turnos, Cortes Z y diferencias de caja por tablet, cajero o sede;
- incidentes pendientes, ventas con monto abierto y resoluciones realizadas;
- métricas operativas como productos más vendidos y rotación, basadas en datos sincronizados.

Los reportes centrales deben mostrar que los eventos pendientes de una tablet aún no forman parte del consolidado, en lugar de inferir que no existen ventas.

## Candidatos fuera del MVP, sin compromiso

Estas ideas pueden ser valiosas para el negocio, pero no están aprobadas ni deben aparecer en el POS móvil o en el contrato inicial sin una especificación propia.

- clientes, fiados, apartados, monederos y cuentas por cobrar;
- proveedores, órdenes de compra, recepción detallada, costos y facturas de compra;
- transferencias de inventario entre sedes;
- recetas, insumos, producción, combos, modificadores, tamaños y extras;
- promociones, cupones, reglas de descuento por producto y listas especiales;
- retiros, gastos de caja e ingresos adicionales durante el turno;
- integración de Mercado Pago por API/SDK, devoluciones electrónicas y conciliación automática de terminal;
- facturación electrónica;
- exportaciones CSV, analítica financiera avanzada, margen/ganancia y dashboards personalizados;
- comunicación directa entre tablets, servidor LAN o gateway local.

Cuando una de estas capacidades se apruebe, se definirá primero su regla de negocio, impacto offline, permisos, auditoría, workflow y contrato antes de implementarla.

## Relación con el contrato de API

La Device API existe únicamente para enrolar, descargar la proyección local y sincronizar eventos. La Admin API sirve a la Web Central para operar las capacidades de este documento. Las rutas preliminares están en [Sincronización y API](04-sincronizacion-y-api.md); se deben adaptar al modelo y convenciones reales del backend, sin duplicar recursos existentes.

## Fuera del panel local de Manager

El panel de Manager conserva únicamente Corte Z, inventario operativo, historial de la tablet y diagnóstico de periféricos. Su alcance y wireframe se mantienen en [Panel local de Manager](../ux/03-panel-manager.md).
