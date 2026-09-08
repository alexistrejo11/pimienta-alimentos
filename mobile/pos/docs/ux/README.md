# UX y wireframes del POS

Esta carpeta conserva la arquitectura de pantallas, estados y flujos táctiles del POS. No fija diseño visual final —colores, tipografía, iconos, espaciados o estilo de componentes—, sino qué información y acciones deben estar disponibles en cada momento.

## Documentos

- [Pantalla de venta y cobro](01-venta-y-cobro.md): wireframe funcional del flujo principal de cajero.
- [Monto abierto manual](02-monto-abierto.md): wireframe y excepción autorizada sin barcode.
- [Producto pendiente de catálogo](04-producto-pendiente-catalogo.md): excepción rápida para barcode desconocido.
- [Panel local de Manager](03-panel-manager.md): operación, supervisión y diagnóstico de una tablet.

## Navegación del MVP

La aplicación tiene dos áreas principales:

1. **Venta:** área de trabajo del cajero, disponible durante un turno activo.
2. **Panel local de Manager:** supervisión de la tablet, turno, historial, inventario operativo y diagnósticos.

El cobro no es una tercera pantalla ni un flujo de navegación separado. Es un estado temporal del panel derecho dentro de Venta. La web central es una aplicación administrativa independiente y no forma parte de la navegación offline de caja.

## Relación con otras especificaciones

- Las reglas de negocio están en [producto](../product/README.md).
- Los límites técnicos están en [arquitectura](../architecture/README.md).
- Esta documentación define cómo esas reglas se hacen visibles y accionables en la tablet.
